package com.pullwise.api.infrastructure.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.dto.BitBucketWebhookPayload;
import com.pullwise.api.application.service.config.ConfigurationResolver;
import com.pullwise.api.domain.constants.ConfigKeys;
import com.pullwise.api.application.service.integration.BitBucketService;
import com.pullwise.api.application.service.integration.SlashCommandService;
import com.pullwise.api.application.service.review.ReviewOrchestrator;
import com.pullwise.api.domain.enums.Platform;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.repository.ProjectRepository;
import com.pullwise.api.domain.repository.PullRequestRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

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
    private final PullRequestRepository pullRequestRepository;
    private final ReviewOrchestrator reviewOrchestrator;
    private final ConfigurationResolver configurationResolver;
    private final SlashCommandService slashCommandService;
    private final ObjectMapper objectMapper;

    @Value("${integrations.bitbucket.webhook-secret:}")
    private String webhookSecret;

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
            // Validate webhook signature if secret is configured
            if (webhookSecret != null && !webhookSecret.isBlank()) {
                if (!validateSignature(payload, signature)) {
                    log.warn("Invalid BitBucket webhook signature from {}", request.getRemoteAddr());
                    return ResponseEntity.status(401).build();
                }
            }

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

                case "pullrequest:comment_created" -> {
                    handleCommentEvent(webhookPayload);
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
     * Processa eventos de comentário em PRs (slash commands).
     */
    private void handleCommentEvent(BitBucketWebhookPayload payload) {
        BitBucketWebhookPayload.Comment comment = payload.getComment();
        BitBucketWebhookPayload.PullRequest prData = payload.getPullRequest();

        if (comment == null || prData == null) return;

        String body = comment.getContent() != null ? comment.getContent().getRaw() : null;
        if (body == null || !slashCommandService.containsCommand(body)) return;

        log.info("Slash command detected in BitBucket PR #{} comment", prData.getNumber());

        // Encontrar projeto e PR no banco
        String repoFullName = payload.getRepository() != null ? payload.getRepository().getFullName() : null;
        if (repoFullName == null) return;

        PullRequest pr = bitBucketService.syncPullRequest(payload);
        if (pr == null) {
            log.warn("Could not find/sync BitBucket PR #{}", prData.getNumber());
            return;
        }

        String username = comment.getUser() != null ? comment.getUser().getNickname() : "unknown";
        slashCommandService.executeCommand(pr.getId(), body, username);
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
                configurationResolver.getConfig(project.getId(), ConfigKeys.REVIEW_AUTO_POST)
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

    /**
     * Validates the HMAC-SHA256 signature of a BitBucket webhook payload.
     */
    private boolean validateSignature(String payload, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);
            return expected.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to validate BitBucket webhook signature", e);
            return false;
        }
    }
}
