package com.pullwise.api.infrastructure.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.service.config.ConfigurationResolver;
import com.pullwise.api.domain.constants.ConfigKeys;
import com.pullwise.api.application.service.integration.AzureDevOpsService;
import com.pullwise.api.application.service.integration.AzureDevOpsService.AzureDevOpsWebhookPayload;
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

import java.util.List;

/**
 * Controller para receber webhooks do Azure DevOps.
 * Suporta Azure DevOps Services (cloud) e Azure DevOps Server (on-premises).
 *
 * <p>Azure DevOps Service Hooks envia webhooks para eventos de Pull Request.
 * Validação via shared secret no header customizado ou Basic auth.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/azure-devops")
@RequiredArgsConstructor
public class AzureDevOpsWebhookController {

    private final AzureDevOpsService azureDevOpsService;
    private final ReviewOrchestrator reviewOrchestrator;
    private final ConfigurationResolver configurationResolver;
    private final SlashCommandService slashCommandService;
    private final ProjectRepository projectRepository;
    private final PullRequestRepository pullRequestRepository;
    private final ObjectMapper objectMapper;

    @Value("${integrations.azure-devops.webhook-secret:}")
    private String webhookSecret;

    /**
     * Endpoint para receber webhooks do Azure DevOps.
     */
    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Azure-DevOps-Secret", required = false) String secret,
            HttpServletRequest request
    ) {
        log.info("Received Azure DevOps webhook from {}", request.getRemoteAddr());

        try {
            // Validate webhook secret if configured
            if (webhookSecret != null && !webhookSecret.isBlank()) {
                if (!webhookSecret.equals(secret)) {
                    log.warn("Invalid Azure DevOps webhook secret from {}", request.getRemoteAddr());
                    return ResponseEntity.status(401).build();
                }
            }

            AzureDevOpsWebhookPayload webhookPayload =
                    objectMapper.readValue(payload, AzureDevOpsWebhookPayload.class);

            String eventType = webhookPayload.getEventType();
            log.info("Azure DevOps event type: {}", eventType);

            if (eventType == null) {
                log.debug("No eventType in Azure DevOps webhook payload");
                return ResponseEntity.accepted().build();
            }

            switch (eventType) {
                case "git.pullrequest.created", "git.pullrequest.updated" ->
                        handlePullRequestEvent(webhookPayload);
                case "git.pullrequest.merged" ->
                        handlePullRequestMerged(webhookPayload);
                case "ms.vss-code.git-pullrequest-comment-event" ->
                        handleCommentEvent(webhookPayload);
                default ->
                        log.debug("Unhandled Azure DevOps event type: {}", eventType);
            }

            return ResponseEntity.accepted().build();

        } catch (Exception e) {
            log.error("Error processing Azure DevOps webhook", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Processa eventos de Pull Request (created/updated).
     */
    private void handlePullRequestEvent(AzureDevOpsWebhookPayload payload) {
        AzureDevOpsWebhookPayload.PullRequestResource resource = payload.getResource();
        if (resource == null) {
            log.warn("Pull request event without resource");
            return;
        }

        int prNumber = resource.getPullRequestId();
        log.info("Processing Azure DevOps PR event for PR #{}", prNumber);

        // Sync pull request
        PullRequest pr = azureDevOpsService.syncPullRequest(payload);

        if (pr == null || pr.getProject() == null) {
            log.warn("Could not sync PR from Azure DevOps webhook");
            return;
        }

        // Check if auto-review is enabled
        boolean autoReviewEnabled = isAutoReviewEnabled(pr.getProject());

        if (autoReviewEnabled) {
            startReviewForPullRequest(pr);
        } else {
            log.info("Auto-review disabled for project {}", pr.getProject().getName());
        }
    }

    /**
     * Processa eventos de merge de Pull Request.
     */
    private void handlePullRequestMerged(AzureDevOpsWebhookPayload payload) {
        AzureDevOpsWebhookPayload.PullRequestResource resource = payload.getResource();
        if (resource == null) return;

        log.info("Azure DevOps PR #{} merged", resource.getPullRequestId());

        // Sync to update status
        azureDevOpsService.syncPullRequest(payload);
    }

    /**
     * Processa eventos de comentário para detectar slash commands.
     */
    private void handleCommentEvent(AzureDevOpsWebhookPayload payload) {
        AzureDevOpsWebhookPayload.PullRequestResource resource = payload.getResource();
        if (resource == null) return;

        List<AzureDevOpsWebhookPayload.CommentInfo> comments = resource.getComments();
        if (comments == null || comments.isEmpty()) return;

        // Get the latest comment
        AzureDevOpsWebhookPayload.CommentInfo latestComment = comments.get(comments.size() - 1);
        String commentBody = latestComment.getContent();

        if (commentBody == null || commentBody.isBlank()) return;
        if (!slashCommandService.containsCommand(commentBody)) return;

        int prNumber = resource.getPullRequestId();
        AzureDevOpsWebhookPayload.RepositoryInfo repo = resource.getRepository();
        if (repo == null) return;

        // Find project
        Project project = projectRepository.findAll().stream()
                .filter(p -> p.getPlatform() == Platform.AZURE_DEVOPS
                        && p.getRepositoryUrl() != null
                        && p.getRepositoryUrl().contains(repo.getName()))
                .findFirst()
                .orElse(null);

        if (project == null) {
            log.warn("Project not found for Azure DevOps repo: {}", repo.getName());
            return;
        }

        PullRequest pr = pullRequestRepository.findByProjectIdAndPrNumber(
                project.getId(), prNumber).orElse(null);

        if (pr == null) {
            log.warn("PR #{} not found for project {}", prNumber, project.getName());
            return;
        }

        String username = latestComment.getAuthor() != null
                ? latestComment.getAuthor().getDisplayName() : "unknown";
        log.info("Slash command detected in Azure DevOps PR #{} by {}", prNumber, username);
        slashCommandService.executeCommand(pr.getId(), commentBody, username);
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
     * Inicia um review para o Pull Request.
     */
    private void startReviewForPullRequest(PullRequest pr) {
        try {
            Long projectId = pr.getProject().getId();

            boolean sastEnabled = configurationResolver.isSastEnabled(projectId);
            boolean llmEnabled = configurationResolver.isLLMEnabled(projectId);
            boolean ragEnabled = configurationResolver.isRAGEnabled(projectId);

            com.pullwise.api.domain.model.Review review =
                    reviewOrchestrator.createReview(pr.getId(), sastEnabled, llmEnabled, ragEnabled);

            reviewOrchestrator.startReview(review.getId());

            log.info("Started review {} for Azure DevOps PR #{}", review.getId(), pr.getPrNumber());

        } catch (Exception e) {
            log.error("Failed to start review for Azure DevOps PR #{}", pr.getPrNumber(), e);
        }
    }
}
