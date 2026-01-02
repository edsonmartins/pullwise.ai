package com.pullwise.api.infrastructure.webhook;

import com.pullwise.api.application.service.review.ReviewOrchestrator;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.config.ConfigurationResolver;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.repository.ProjectRepository;
import com.pullwise.api.domain.repository.PullRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller para receber webhooks do GitHub.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/github")
@RequiredArgsConstructor
public class GitHubWebhookController {

    private final GitHubService gitHubService;
    private final ProjectRepository projectRepository;
    private final PullRequestRepository pullRequestRepository;
    private final ReviewOrchestrator reviewOrchestrator;
    private final ConfigurationResolver configurationResolver;
    private final ObjectMapper objectMapper;

    /**
     * Endpoint para receber webhooks do GitHub.
     */
    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            HttpServletRequest request
    ) {
        log.info("Received GitHub webhook event: {}", eventType);

        try {
            // Validar assinatura do webhook (em produção)
            // if (!validateSignature(payload, signature)) {
            //     return ResponseEntity.status(401).build();
            // }

            GitHubService.GitHubWebhookPayload webhookPayload =
                    objectMapper.readValue(payload, GitHubService.GitHubWebhookPayload.class);

            // Processar diferentes tipos de eventos
            switch (eventType) {
                case "pull_request":
                    handlePullRequestEvent(webhookPayload);
                    break;

                case "pull_request_review":
                    handlePullRequestReviewEvent(webhookPayload);
                    break;

                case "installation":
                    handleInstallationEvent(webhookPayload);
                    break;

                case "push":
                    handlePushEvent(webhookPayload);
                    break;

                default:
                    log.debug("Unhandled event type: {}", eventType);
            }

            return ResponseEntity.accepted().build();

        } catch (Exception e) {
            log.error("Error processing GitHub webhook", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Processa eventos de Pull Request.
     */
    private void handlePullRequestEvent(GitHubService.GitHubWebhookPayload payload) {
        String action = payload.getAction();

        log.info("Processing PR action: {} for PR #{}",
                action, payload.getPullRequest().getNumber());

        switch (action) {
            case "opened", "synchronize", "reopened" -> {
                // Sincronizar PR
                PullRequest pr = gitHubService.syncPullRequest(payload);

                // Verificar se auto-review está habilitado
                boolean autoReviewEnabled = isAutoReviewEnabled(pr.getProject());

                if (autoReviewEnabled) {
                    // Iniciar review automaticamente
                    startReviewForPullRequest(pr);
                } else {
                    log.info("Auto-review disabled for project {}", pr.getProject().getName());
                }
            }

            case "closed" -> {
                // PR fechado sem merge
                updatePullRequestStatus(payload, false, true);
            }

            case "merged" -> {
                // PR foi mergeado
                updatePullRequestStatus(payload, true, false);
            }
        }
    }

    /**
     * Processa eventos de revisão de PR.
     */
    private void handlePullRequestReviewEvent(GitHubService.GitHubWebhookPayload payload) {
        log.info("Processing PR review event for PR #{}", payload.getPullRequest().getNumber());
        // Poderia ser usado para detectar quando um PR foi aprovado
    }

    /**
     * Processa eventos de instalação do GitHub App.
     */
    private void handleInstallationEvent(GitHubService.GitHubWebhookPayload payload) {
        log.info("Processing GitHub App installation event");
        // Salvar informações da instalação
    }

    /**
     * Processa eventos de push.
     */
    private void handlePushEvent(GitHubService.GitHubWebhookPayload payload) {
        log.info("Processing push event");
        // Poderia ser usado para trigger de análises
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

            log.info("Started review {} for PR #{}", review.getId(), pr.getPrNumber());

        } catch (Exception e) {
            log.error("Failed to start review for PR #{}", pr.getPrNumber(), e);
        }
    }

    /**
     * Atualiza o status do PR quando fechado/mergeado.
     */
    private void updatePullRequestStatus(GitHubService.GitHubWebhookPayload payload,
                                         boolean merged, boolean closed) {
        // Buscar PR e atualizar status
        // Implementação simplificada
    }
}
