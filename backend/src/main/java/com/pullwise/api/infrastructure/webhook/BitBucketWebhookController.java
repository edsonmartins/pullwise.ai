package com.pullwise.api.infrastructure.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.dto.BitBucketWebhookPayload;
import com.pullwise.api.application.service.config.ConfigurationResolver;
import com.pullwise.api.application.service.integration.BitBucketService;
import com.pullwise.api.application.service.review.ReviewOrchestrator;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.repository.ProjectRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para receber webhooks do BitBucket.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/bitbucket")
@RequiredArgsConstructor
public class BitBucketWebhookController {

    private final BitBucketService bitBucketService;
    private final ProjectRepository projectRepository;
    private final ReviewOrchestrator reviewOrchestrator;
    private final ConfigurationResolver configurationResolver;
    private final ObjectMapper objectMapper;

    /**
     * Endpoint para receber webhooks do BitBucket.
     */
    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Event-Key") String eventType,
            @RequestHeader(value = "X-Hub-Signature", required = false) String signature,
            HttpServletRequest request
    ) {
        log.info("Received BitBucket webhook event: {}", eventType);

        try {
            // Validar assinatura do webhook (em produção)
            // if (!validateSignature(payload, signature)) {
            //     return ResponseEntity.status(401).build();
            // }

            BitBucketWebhookPayload webhookPayload =
                    objectMapper.readValue(payload, BitBucketWebhookPayload.class);

            // Processar diferentes tipos de eventos
            switch (eventType) {
                case "pullrequest:created",
                     "pullrequest:updated",
                     "pullrequest:fulfilled" -> {
                    handlePullRequestEvent(eventType, webhookPayload);
                }

                case "pullrequest:approved",
                     "pullrequest:unapproved",
                     "pullrequest:changes_request_created" -> {
                    handlePullRequestReviewEvent(eventType, webhookPayload);
                }

                case "repo:push" -> {
                    handlePushEvent(webhookPayload);
                }

                default -> log.debug("Unhandled event type: {}", eventType);
            }

            return ResponseEntity.accepted().build();

        } catch (Exception e) {
            log.error("Error processing BitBucket webhook", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Processa eventos de Pull Request do BitBucket.
     */
    private void handlePullRequestEvent(String action, BitBucketWebhookPayload payload) {
        BitBucketWebhookPayload.PullRequest prData = payload.getPullRequest();
        String actionName = action.substring(action.lastIndexOf(':') + 1);

        log.info("Processing BitBucket PR action: {} for PR #{}",
                actionName, prData.getNumber());

        // Ações que devem trigger review
        boolean shouldTrigger = switch (action) {
            case "pullrequest:created",
                 "pullrequest:updated",
                 "pullrequest:fulfilled" -> true;
            default -> false;
        };

        if (!shouldTrigger) {
            return;
        }

        // Sincronizar PR
        PullRequest pr = bitBucketService.syncPullRequest(payload);

        if (pr == null || pr.getProject() == null) {
            log.warn("Could not sync PR from BitBucket webhook");
            return;
        }

        // Verificar se o PR está aberto
        if (pr.getIsClosed() || pr.getIsMerged()) {
            log.info("PR #{} is closed or merged, skipping review", pr.getPrNumber());
            return;
        }

        // Verificar se auto-review está habilitado
        boolean autoReviewEnabled = isAutoReviewEnabled(pr.getProject());

        if (autoReviewEnabled) {
            // Iniciar review automaticamente
            startReviewForPullRequest(pr);
        } else {
            log.info("Auto-review disabled for project {}", pr.getProject().getName());
        }
    }

    /**
     * Processa eventos de revisão de PR.
     */
    private void handlePullRequestReviewEvent(String action, BitBucketWebhookPayload payload) {
        BitBucketWebhookPayload.PullRequest prData = payload.getPullRequest();
        log.info("Processing BitBucket PR review event: {} for PR #{}",
                action, prData.getNumber());

        // Se aprovado, não fazer nada (review já passou)
        // Se changes_request_created, poderia trigger novo review
        if ("pullrequest:changes_request_created".equals(action)) {
            PullRequest pr = bitBucketService.syncPullRequest(payload);
            if (pr != null && !pr.getIsClosed() && !pr.getIsMerged()) {
                log.info("Changes requested for PR #{}, triggering new review", pr.getPrNumber());
                startReviewForPullRequest(pr);
            }
        }
    }

    /**
     * Processa eventos de push.
     */
    private void handlePushEvent(BitBucketWebhookPayload payload) {
        log.info("Processing BitBucket push event to {}", payload.getRepository().getFullName());
        // Poderia ser usado para trigger de análises em branches específicos
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
     * Inicia um review para o PR.
     */
    private void startReviewForPullRequest(PullRequest pr) {
        try {
            Long projectId = pr.getProject().getId();

            boolean sastEnabled = configurationResolver.isSastEnabled(projectId);
            boolean llmEnabled = configurationResolver.isLLMEnabled(projectId);
            boolean ragEnabled = configurationResolver.isRAGEnabled(projectId);

            com.pullwise.api.domain.model.Review review =
                    reviewOrchestrator.createReview(pr.getId(), sastEnabled, llmEnabled, ragEnabled);

            // Iniciar processamento assíncrono
            reviewOrchestrator.startReview(review.getId());

            log.info("Started review {} for BitBucket PR #{}", review.getId(), pr.getPrNumber());

        } catch (Exception e) {
            log.error("Failed to start review for PR #{}", pr.getPrNumber(), e);
        }
    }
}
