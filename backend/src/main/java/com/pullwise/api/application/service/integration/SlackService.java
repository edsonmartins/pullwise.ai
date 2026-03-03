package com.pullwise.api.application.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.service.config.ConfigurationResolver;
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
 * Serviço de integração com Slack via Incoming Webhooks.
 *
 * <p>Envia notificações de review completado para canais Slack configurados.
 * Usa a Slack Incoming Webhooks API (não requer OAuth app).
 *
 * <p>Configuração por organização/projeto via ConfigurationResolver:
 * <ul>
 *   <li>{@code slack.webhook-url} — URL do webhook do Slack</li>
 *   <li>{@code slack.channel} — Canal (override, opcional)</li>
 *   <li>{@code slack.notify-on} — Quando notificar: "all", "critical", "high" (default: "all")</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlackService {

    @Value("${integrations.slack.default-webhook-url:}")
    private String defaultWebhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final ConfigurationResolver configurationResolver;

    /**
     * Verifica se Slack está configurado (global ou por projeto).
     */
    public boolean isConfigured() {
        return defaultWebhookUrl != null && !defaultWebhookUrl.isBlank();
    }

    /**
     * Verifica se Slack está configurado para um projeto específico.
     */
    public boolean isConfiguredForProject(Long projectId) {
        String webhookUrl = resolveWebhookUrl(projectId);
        return webhookUrl != null && !webhookUrl.isBlank();
    }

    /**
     * Envia notificação de review completado para o Slack.
     */
    public boolean sendReviewNotification(Review review, List<Issue> issues) {
        Long projectId = review.getPullRequest().getProject().getId();
        String webhookUrl = resolveWebhookUrl(projectId);

        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Slack not configured for project {}", projectId);
            return false;
        }

        // Verificar filtro de severidade
        String notifyOn = configurationResolver.getConfig(projectId, "slack.notify-on");
        if (notifyOn == null) notifyOn = "all";

        if (!shouldNotify(notifyOn, issues)) {
            log.debug("Slack notification filtered for project {} (notifyOn={})", projectId, notifyOn);
            return false;
        }

        try {
            String payload = buildSlackPayload(review, issues);
            return sendWebhook(webhookUrl, payload);
        } catch (Exception e) {
            log.error("Failed to send Slack notification for review {}: {}", review.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Envia mensagem de texto simples para o Slack.
     */
    public boolean sendMessage(Long projectId, String text) {
        String webhookUrl = resolveWebhookUrl(projectId);
        if (webhookUrl == null || webhookUrl.isBlank()) return false;

        try {
            String payload = objectMapper.writeValueAsString(Map.of("text", text));
            return sendWebhook(webhookUrl, payload);
        } catch (Exception e) {
            log.error("Failed to send Slack message: {}", e.getMessage());
            return false;
        }
    }

    // ========== Private Helpers ==========

    private String resolveWebhookUrl(Long projectId) {
        String projectUrl = configurationResolver.getConfig(projectId, "slack.webhook-url");
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

    private String buildSlackPayload(Review review, List<Issue> issues) throws Exception {
        var pr = review.getPullRequest();
        String repoName = pr.getProject() != null ? pr.getProject().getName() : "unknown";

        Map<Severity, Long> counts = issues.stream()
                .collect(Collectors.groupingBy(Issue::getSeverity, Collectors.counting()));

        long critical = counts.getOrDefault(Severity.CRITICAL, 0L);
        long high = counts.getOrDefault(Severity.HIGH, 0L);
        long medium = counts.getOrDefault(Severity.MEDIUM, 0L);
        long low = counts.getOrDefault(Severity.LOW, 0L);

        String severity = critical > 0 ? "danger" : (high > 0 ? "warning" : "good");

        // Build Block Kit payload
        var blocks = List.of(
                Map.of(
                        "type", "header",
                        "text", Map.of("type", "plain_text", "text", "Pullwise Review Complete")
                ),
                Map.of(
                        "type", "section",
                        "fields", List.of(
                                Map.of("type", "mrkdwn", "text", "*Repository:*\n" + repoName),
                                Map.of("type", "mrkdwn", "text", "*PR:*\n#" + pr.getPrNumber() + " " + (pr.getTitle() != null ? pr.getTitle() : "")),
                                Map.of("type", "mrkdwn", "text", "*Issues Found:*\n" + issues.size()),
                                Map.of("type", "mrkdwn", "text", String.format(
                                        "*Breakdown:*\n:red_circle: %d  :large_orange_circle: %d  :large_yellow_circle: %d  :large_green_circle: %d",
                                        critical, high, medium, low
                                ))
                        )
                ),
                Map.of(
                        "type", "context",
                        "elements", List.of(
                                Map.of("type", "mrkdwn", "text",
                                        pr.getReviewUrl() != null
                                                ? "<" + pr.getReviewUrl() + "|View PR> | Powered by <https://pullwise.ai|Pullwise>"
                                                : "Powered by <https://pullwise.ai|Pullwise>"
                                )
                        )
                )
        );

        return objectMapper.writeValueAsString(Map.of("blocks", blocks));
    }

    private boolean sendWebhook(String webhookUrl, String payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    webhookUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Slack notification sent successfully");
                return true;
            } else {
                log.warn("Slack webhook returned status: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send Slack webhook: {}", e.getMessage());
            return false;
        }
    }
}
