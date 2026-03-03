package com.pullwise.api.infrastructure.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.service.config.ConfigurationResolver;
import com.pullwise.api.application.service.integration.GitLabService;
import com.pullwise.api.application.service.integration.GitLabService.GitLabWebhookPayload;
import com.pullwise.api.application.service.review.ReviewOrchestrator;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.model.PullRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para receber webhooks do GitLab.
 * Suporta GitLab.com e instâncias self-hosted.
 *
 * <p>GitLab envia o header X-Gitlab-Token para validação (shared secret).
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/gitlab")
@RequiredArgsConstructor
public class GitLabWebhookController {

    private final GitLabService gitLabService;
    private final ReviewOrchestrator reviewOrchestrator;
    private final ConfigurationResolver configurationResolver;
    private final ObjectMapper objectMapper;

    @Value("${integrations.gitlab.webhook-secret:}")
    private String webhookSecret;

    /**
     * Endpoint para receber webhooks do GitLab.
     */
    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Gitlab-Event", required = false) String eventType,
            @RequestHeader(value = "X-Gitlab-Token", required = false) String token,
            HttpServletRequest request
    ) {
        log.info("Received GitLab webhook event: {}", eventType);

        try {
            // Validar token do webhook
            if (webhookSecret != null && !webhookSecret.isBlank()) {
                if (!webhookSecret.equals(token)) {
                    log.warn("Invalid GitLab webhook token from {}", request.getRemoteAddr());
                    return ResponseEntity.status(401).build();
                }
            }

            GitLabWebhookPayload webhookPayload =
                    objectMapper.readValue(payload, GitLabWebhookPayload.class);

            // Processar por tipo de evento
            if ("Merge Request Hook".equals(eventType) ||
                    "merge_request".equals(webhookPayload.getObjectKind())) {
                handleMergeRequestEvent(webhookPayload);
            } else if ("Push Hook".equals(eventType)) {
                handlePushEvent(webhookPayload);
            } else if ("Note Hook".equals(eventType)) {
                handleNoteEvent(webhookPayload);
            } else {
                log.debug("Unhandled GitLab event type: {}", eventType);
            }

            return ResponseEntity.accepted().build();

        } catch (Exception e) {
            log.error("Error processing GitLab webhook", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Processa eventos de Merge Request.
     */
    private void handleMergeRequestEvent(GitLabWebhookPayload payload) {
        GitLabWebhookPayload.MergeRequestAttributes mr = payload.getObjectAttributes();
        if (mr == null) {
            log.warn("Merge request event without object_attributes");
            return;
        }

        String action = mr.getAction();
        log.info("Processing GitLab MR action: {} for MR !{}", action, mr.getIid());

        switch (action != null ? action : "") {
            case "open", "reopen", "update" -> {
                // Sincronizar MR
                PullRequest pr = gitLabService.syncMergeRequest(payload);

                if (pr == null || pr.getProject() == null) {
                    log.warn("Could not sync MR from GitLab webhook");
                    return;
                }

                // Verificar se auto-review está habilitado
                boolean autoReviewEnabled = isAutoReviewEnabled(pr.getProject());

                if (autoReviewEnabled) {
                    startReviewForMergeRequest(pr);
                } else {
                    log.info("Auto-review disabled for project {}", pr.getProject().getName());
                }
            }

            case "close" -> {
                log.info("MR !{} closed", mr.getIid());
            }

            case "merge" -> {
                log.info("MR !{} merged", mr.getIid());
            }

            default -> log.debug("Unhandled MR action: {}", action);
        }
    }

    /**
     * Processa eventos de push.
     */
    private void handlePushEvent(GitLabWebhookPayload payload) {
        log.info("Processing GitLab push event for project {}",
                payload.getProject() != null ? payload.getProject().getPathWithNamespace() : "unknown");
    }

    /**
     * Processa eventos de notas/comentários.
     */
    private void handleNoteEvent(GitLabWebhookPayload payload) {
        log.info("Processing GitLab note event");
    }

    /**
     * Verifica se auto-review está habilitado para o projeto.
     */
    private boolean isAutoReviewEnabled(Project project) {
        if (project.getAutoReviewEnabled() != null && project.getAutoReviewEnabled()) {
            return true;
        }
        return Boolean.parseBoolean(
                configurationResolver.getConfig(project.getId(), "review.auto_post")
        );
    }

    /**
     * Inicia um review para o MR.
     */
    private void startReviewForMergeRequest(PullRequest pr) {
        try {
            Long projectId = pr.getProject().getId();

            boolean sastEnabled = configurationResolver.isSastEnabled(projectId);
            boolean llmEnabled = configurationResolver.isLLMEnabled(projectId);
            boolean ragEnabled = configurationResolver.isRAGEnabled(projectId);

            com.pullwise.api.domain.model.Review review =
                    reviewOrchestrator.createReview(pr.getId(), sastEnabled, llmEnabled, ragEnabled);

            reviewOrchestrator.startReview(review.getId());

            log.info("Started review {} for GitLab MR !{}", review.getId(), pr.getPrNumber());

        } catch (Exception e) {
            log.error("Failed to start review for MR !{}", pr.getPrNumber(), e);
        }
    }
}
