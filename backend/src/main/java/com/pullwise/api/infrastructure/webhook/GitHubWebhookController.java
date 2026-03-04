package com.pullwise.api.infrastructure.webhook;

import com.pullwise.api.application.service.review.ReviewOrchestrator;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.integration.SlashCommandService;
import com.pullwise.api.application.service.config.ConfigurationResolver;
import com.pullwise.api.application.service.config.RAGService;
import com.pullwise.api.domain.constants.ConfigKeys;
import com.pullwise.api.domain.enums.Platform;
import com.pullwise.api.domain.model.Organization;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.repository.OrganizationRepository;
import com.pullwise.api.domain.repository.ProjectRepository;
import com.pullwise.api.domain.repository.PullRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
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
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final PullRequestRepository pullRequestRepository;
    private final ReviewOrchestrator reviewOrchestrator;
    private final ConfigurationResolver configurationResolver;
    private final SlashCommandService slashCommandService;
    private final RAGService ragService;
    private final ObjectMapper objectMapper;

    @Value("${integrations.github.webhook-secret:}")
    private String webhookSecret;

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
            // Validar assinatura do webhook
            if (webhookSecret != null && !webhookSecret.isBlank()) {
                if (!validateSignature(payload, signature)) {
                    log.warn("Invalid webhook signature from {}", request.getRemoteAddr());
                    return ResponseEntity.status(401).build();
                }
            }

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

                case "issue_comment":
                    handleIssueCommentEvent(webhookPayload);
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
     * Creates or updates the Organization and registers Projects for each repository
     * included in the installation.
     */
    private void handleInstallationEvent(GitHubService.GitHubWebhookPayload payload) {
        String action = payload.getAction();
        GitHubService.GitHubWebhookPayload.Installation installation = payload.getInstallation();

        if (installation == null || installation.getAccount() == null) {
            log.warn("Installation event missing installation or account data");
            return;
        }

        String githubOrgId = String.valueOf(installation.getAccount().getId());
        String orgName = installation.getAccount().getLogin();
        Long installationId = installation.getId();

        log.info("Processing GitHub App installation event: action={}, org={}, installationId={}",
                action, orgName, installationId);

        if ("deleted".equals(action)) {
            // Deactivate all projects associated with this installation
            organizationRepository.findByGitHubOrgId(githubOrgId).ifPresent(org -> {
                List<Project> projects = projectRepository.findByOrganizationIdAndPlatform(
                        org.getId(), Platform.GITHUB);
                for (Project project : projects) {
                    project.setIsActive(false);
                    project.setGithubInstallationId(null);
                    projectRepository.save(project);
                }
                log.info("Deactivated {} projects for uninstalled org {}", projects.size(), orgName);
            });
            return;
        }

        // For "created" or "new_permissions_accepted" actions: create/update org and projects
        Organization org = organizationRepository.findByGitHubOrgId(githubOrgId)
                .orElseGet(() -> {
                    Organization newOrg = Organization.builder()
                            .name(orgName)
                            .slug(orgName.toLowerCase().replaceAll("[^a-z0-9-]", "-"))
                            .githubOrgId(githubOrgId)
                            .logoUrl(installation.getAccount().getAvatarUrl())
                            .build();
                    Organization saved = organizationRepository.save(newOrg);
                    log.info("Created organization '{}' from GitHub App installation", orgName);
                    return saved;
                });

        // Register each repository from the installation as a Project
        List<GitHubService.GitHubWebhookPayload.Repository> repos = payload.getRepositories();
        if (repos == null || repos.isEmpty()) {
            log.debug("No repositories in installation payload for org {}", orgName);
            return;
        }

        for (GitHubService.GitHubWebhookPayload.Repository repo : repos) {
            String repoId = String.valueOf(repo.getId());
            Optional<Project> existingProject = projectRepository.findByOrganizationIdAndRepositoryId(
                    org.getId(), repoId);

            if (existingProject.isPresent()) {
                Project project = existingProject.get();
                project.setGithubInstallationId(installationId);
                project.setIsActive(true);
                projectRepository.save(project);
                log.debug("Updated existing project {} with installation ID {}", repo.getFullName(), installationId);
            } else {
                String repoUrl = repo.getHtmlUrl() != null
                        ? repo.getHtmlUrl()
                        : "https://github.com/" + repo.getFullName();

                Project newProject = Project.builder()
                        .name(repo.getName())
                        .organization(org)
                        .platform(Platform.GITHUB)
                        .repositoryUrl(repoUrl)
                        .repositoryId(repoId)
                        .githubInstallationId(installationId)
                        .isActive(true)
                        .autoReviewEnabled(true)
                        .build();
                projectRepository.save(newProject);
                log.info("Created project '{}' for org '{}'", repo.getFullName(), orgName);
            }
        }

        log.info("Processed {} repositories for installation on org {}", repos.size(), orgName);
    }

    /**
     * Processa eventos de push.
     * Triggers knowledge base (RAG) re-indexing for the affected project
     * when pushes land on the default branch.
     */
    private void handlePushEvent(GitHubService.GitHubWebhookPayload payload) {
        if (payload.getRepository() == null) {
            log.warn("Push event missing repository data");
            return;
        }

        String ref = payload.getRef();
        String repoId = String.valueOf(payload.getRepository().getId());
        String repoName = payload.getRepository().getFullName();

        log.info("Processing push event for repo {} on ref {}", repoName, ref);

        // Only trigger knowledge base sync for pushes to the default branch (main/master)
        if (ref == null || !(ref.endsWith("/main") || ref.endsWith("/master"))) {
            log.debug("Ignoring push to non-default branch: {}", ref);
            return;
        }

        Optional<Project> projectOpt = projectRepository.findByRepositoryIdAndPlatform(
                repoId, Platform.GITHUB);

        if (projectOpt.isEmpty()) {
            log.debug("No project found for repository {} (id={}), skipping RAG sync", repoName, repoId);
            return;
        }

        Project project = projectOpt.get();

        if (!Boolean.TRUE.equals(project.getIsActive())) {
            log.debug("Project {} is inactive, skipping RAG sync", project.getName());
            return;
        }

        log.info("Triggering knowledge base re-indexing for project {} (id={})", project.getName(), project.getId());
        ragService.indexProjectDocuments(project.getId());
    }

    /**
     * Processa eventos de comentário em issues/PRs (slash commands).
     * GitHub envia issue_comment para comentários em PRs também.
     */
    private void handleIssueCommentEvent(GitHubService.GitHubWebhookPayload payload) {
        if (!"created".equals(payload.getAction())) return;

        GitHubService.GitHubWebhookPayload.Comment comment = payload.getComment();
        GitHubService.GitHubWebhookPayload.IssueDTO issue = payload.getIssue();

        if (comment == null || issue == null) return;

        // Verificar se é um PR (issue_comment em PRs tem pull_request != null)
        if (issue.getPullRequest() == null) return;

        String body = comment.getBody();
        if (!slashCommandService.containsCommand(body)) return;

        log.info("Slash command detected in PR #{} comment by {}",
                issue.getNumber(), comment.getUser() != null ? comment.getUser().getLogin() : "unknown");

        // Encontrar o PR no banco
        String repoId = String.valueOf(payload.getRepository().getId());
        Project project = projectRepository.findByRepositoryIdAndPlatform(
                repoId, Platform.GITHUB).orElse(null);

        if (project == null) {
            log.warn("Project not found for repo ID {}", repoId);
            return;
        }

        PullRequest pr = pullRequestRepository.findByProjectIdAndPrNumber(
                project.getId(), issue.getNumber()).orElse(null);

        if (pr == null) {
            log.warn("PR #{} not found for project {}", issue.getNumber(), project.getName());
            return;
        }

        String username = comment.getUser() != null ? comment.getUser().getLogin() : "unknown";
        slashCommandService.executeCommand(pr.getId(), body, username);
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

    /**
     * Valida a assinatura HMAC-SHA256 do webhook do GitHub.
     */
    private boolean validateSignature(String payload, String signature) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = "sha256=" + HexFormat.of().formatHex(hash);
            return expected.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to validate webhook signature", e);
            return false;
        }
    }
}
