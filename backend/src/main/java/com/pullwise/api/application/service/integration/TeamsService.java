package com.pullwise.api.application.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.service.config.ConfigurationResolver;
import com.pullwise.api.domain.constants.ConfigKeys;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serviço de integração com Microsoft Teams via Incoming Webhooks.
 *
 * <p>Envia notificações de review completado para canais Teams configurados.
 * Usa Adaptive Cards para formatação rica.
 *
 * <p>Configuração por organização/projeto via ConfigurationResolver:
 * <ul>
 *   <li>{@code teams.webhook-url} — URL do webhook do Teams</li>
 *   <li>{@code teams.notify-on} — Quando notificar: "all", "critical", "high" (default: "all")</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamsService {

    @Value("${integrations.teams.default-webhook-url:}")
    private String defaultWebhookUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ConfigurationResolver configurationResolver;

    /**
     * Verifica se Teams está configurado (global ou por projeto).
     */
    public boolean isConfigured() {
        return defaultWebhookUrl != null && !defaultWebhookUrl.isBlank();
    }

    /**
     * Verifica se Teams está configurado para um projeto específico.
     */
    public boolean isConfiguredForProject(Long projectId) {
        String webhookUrl = resolveWebhookUrl(projectId);
        return webhookUrl != null && !webhookUrl.isBlank();
    }

    /**
     * Envia notificação de review completado para o Teams.
     */
    public boolean sendReviewNotification(Review review, List<Issue> issues) {
        Long projectId = review.getPullRequest().getProject().getId();
        String webhookUrl = resolveWebhookUrl(projectId);

        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Teams not configured for project {}", projectId);
            return false;
        }

        // Verificar filtro de severidade
        String notifyOn = configurationResolver.getConfig(projectId, ConfigKeys.TEAMS_NOTIFY_ON);
        if (notifyOn == null) notifyOn = "all";

        if (!shouldNotify(notifyOn, issues)) {
            log.debug("Teams notification filtered for project {} (notifyOn={})", projectId, notifyOn);
            return false;
        }

        try {
            String payload = buildAdaptiveCardPayload(review, issues);
            return sendWebhook(webhookUrl, payload);
        } catch (Exception e) {
            log.error("Failed to send Teams notification for review {}: {}", review.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Envia mensagem de texto simples para o Teams.
     */
    public boolean sendMessage(Long projectId, String text) {
        String webhookUrl = resolveWebhookUrl(projectId);
        if (webhookUrl == null || webhookUrl.isBlank()) return false;

        try {
            String payload = objectMapper.writeValueAsString(Map.of("text", text));
            return sendWebhook(webhookUrl, payload);
        } catch (Exception e) {
            log.error("Failed to send Teams message: {}", e.getMessage());
            return false;
        }
    }

    // ========== Private Helpers ==========

    private String resolveWebhookUrl(Long projectId) {
        String projectUrl = configurationResolver.getConfig(projectId, ConfigKeys.TEAMS_WEBHOOK_URL);
        if (projectUrl != null && !projectUrl.isBlank()) {
            return projectUrl;
        }
        return defaultWebhookUrl;
    }

    private boolean shouldNotify(String notifyOn, List<Issue> issues) {
        if ("all".equalsIgnoreCase(notifyOn) || issues.isEmpty()) return true;

        return switch (notifyOn.toLowerCase()) {
            case "critical" -> issues.stream().anyMatch(i -> i.getSeverity() == Severity.CRITICAL);
            case "high" -> issues.stream().anyMatch(i ->
                    i.getSeverity() == Severity.CRITICAL || i.getSeverity() == Severity.HIGH);
            default -> true;
        };
    }

    /**
     * Constrói payload usando Adaptive Card para Microsoft Teams.
     */
    private String buildAdaptiveCardPayload(Review review, List<Issue> issues) throws Exception {
        var pr = review.getPullRequest();
        String repoName = pr.getProject() != null ? pr.getProject().getName() : "unknown";

        Map<Severity, Long> counts = issues.stream()
                .collect(Collectors.groupingBy(Issue::getSeverity, Collectors.counting()));

        long critical = counts.getOrDefault(Severity.CRITICAL, 0L);
        long high = counts.getOrDefault(Severity.HIGH, 0L);
        long medium = counts.getOrDefault(Severity.MEDIUM, 0L);
        long low = counts.getOrDefault(Severity.LOW, 0L);

        String themeColor = critical > 0 ? "attention" : (high > 0 ? "warning" : "good");

        // Adaptive Card body elements
        var bodyElements = List.of(
                Map.of(
                        "type", "TextBlock",
                        "size", "Medium",
                        "weight", "Bolder",
                        "text", "Pullwise Review Complete"
                ),
                Map.of(
                        "type", "FactSet",
                        "facts", List.of(
                                Map.of("title", "Repository", "value", repoName),
                                Map.of("title", "PR", "value", "#" + pr.getPrNumber() + " " + (pr.getTitle() != null ? pr.getTitle() : "")),
                                Map.of("title", "Total Issues", "value", String.valueOf(issues.size())),
                                Map.of("title", "Critical", "value", String.valueOf(critical)),
                                Map.of("title", "High", "value", String.valueOf(high)),
                                Map.of("title", "Medium", "value", String.valueOf(medium)),
                                Map.of("title", "Low", "value", String.valueOf(low))
                        )
                )
        );

        // Build Adaptive Card
        var card = Map.of(
                "type", "AdaptiveCard",
                "$schema", "http://adaptivecards.io/schemas/adaptive-card.json",
                "version", "1.4",
                "body", bodyElements,
                "actions", pr.getReviewUrl() != null
                        ? List.of(Map.of(
                        "type", "Action.OpenUrl",
                        "title", "View Pull Request",
                        "url", pr.getReviewUrl()
                ))
                        : List.of()
        );

        // Teams webhook expects this envelope format
        var envelope = Map.of(
                "type", "message",
                "attachments", List.of(Map.of(
                        "contentType", "application/vnd.microsoft.card.adaptive",
                        "contentUrl", (Object) null,
                        "content", card
                ))
        );

        return objectMapper.writeValueAsString(envelope);
    }

    private boolean sendWebhook(String webhookUrl, String payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    webhookUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Teams notification sent successfully");
                return true;
            } else {
                log.warn("Teams webhook returned status: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send Teams webhook: {}", e.getMessage());
            return false;
        }
    }
}
