package com.pullwise.api.application.service.integration;

import com.pullwise.api.application.dto.BitBucketWebhookPayload;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.model.User;
import com.pullwise.api.domain.repository.ProjectRepository;
import com.pullwise.api.domain.repository.PullRequestRepository;
import com.pullwise.api.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Serviço para integração com API do BitBucket.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BitBucketService {

    private final RestTemplate restTemplate;
    private final ProjectRepository projectRepository;
    private final PullRequestRepository pullRequestRepository;
    private final UserRepository userRepository;

    /**
     * Sincroniza um Pull Request vindo do webhook do BitBucket.
     */
    public PullRequest syncPullRequest(BitBucketWebhookPayload payload) {
        BitBucketWebhookPayload.PullRequest prData = payload.getPullRequest();
        BitBucketWebhookPayload.Repository repoData = payload.getRepository();

        // Buscar projeto pelo slug do repositório
        String repoSlug = repoData.getSlug();
        String owner = repoData.getOwner().getUsername();
        String repoFullName = owner + "/" + repoSlug;

        Optional<Project> projectOpt = projectRepository.findAll().stream()
                .filter(p -> p.getRepositoryUrl() != null && p.getRepositoryUrl().contains(repoSlug))
                .findFirst();

        if (projectOpt.isEmpty()) {
            log.warn("Project not found for BitBucket repo: {}", repoFullName);
            return null;
        }

        Project project = projectOpt.get();

        // Buscar ou criar PullRequest
        Optional<PullRequest> existingPr = pullRequestRepository.findByProjectIdAndPrNumber(
                project.getId(),
                prData.getNumber()
        );

        PullRequest pr;
        if (existingPr.isPresent()) {
            pr = existingPr.get();
            updatePrFromPayload(pr, prData);
        } else {
            pr = createPrFromPayload(project, prData);
        }

        return pullRequestRepository.save(pr);
    }

    /**
     * Cria um novo PullRequest a partir do payload do webhook.
     */
    private PullRequest createPrFromPayload(Project project, BitBucketWebhookPayload.PullRequest prData) {
        String authorName = prData.getAuthor() != null ? prData.getAuthor().getDisplayName() : "Unknown";
        String authorEmail = prData.getAuthor() != null ? prData.getAuthor().getEmail() : null;

        String sourceBranch = prData.getSource() != null && prData.getSource().getBranch() != null
                ? prData.getSource().getBranch().getName()
                : "unknown";

        String targetBranch = prData.getDestination() != null && prData.getDestination().getBranch() != null
                ? prData.getDestination().getBranch().getName()
                : "unknown";

        String reviewUrl = prData.getLinks() != null && prData.getLinks().getHtml() != null
                ? prData.getLinks().getHtml().getHref()
                : null;

        return PullRequest.builder()
                .project(project)
                .platform(com.pullwise.api.domain.enums.Platform.BITBUCKET)
                .prId(prData.getId())
                .prNumber(prData.getNumber())
                .title(prData.getTitle())
                .description(prData.getDescription())
                .sourceBranch(sourceBranch)
                .targetBranch(targetBranch)
                .authorName(authorName)
                .authorEmail(authorEmail)
                .reviewUrl(reviewUrl)
                .isClosed("closed".equalsIgnoreCase(prData.getState())
                        || "declined".equalsIgnoreCase(prData.getState()))
                .isMerged("merged".equalsIgnoreCase(prData.getState()))
                .build();
    }

    /**
     * Atualiza um PullRequest existente com dados do payload.
     */
    private void updatePrFromPayload(PullRequest pr, BitBucketWebhookPayload.PullRequest prData) {
        if ("closed".equalsIgnoreCase(prData.getState())
                || "declined".equalsIgnoreCase(prData.getState())) {
            pr.setIsClosed(true);
        }
        if ("merged".equalsIgnoreCase(prData.getState())) {
            pr.setIsMerged(true);
        }
        pr.setTitle(prData.getTitle());
        pr.setDescription(prData.getDescription());
    }

    /**
     * Busca o diff de um PR no BitBucket.
     */
    public String getPullRequestDiff(Project project, Long prId) {
        // TODO: Implementar chamada à API do BitBucket
        // GET /repositories/{workspace}/{repo_slug}/pullrequests/{pull_request_id}/diff
        log.warn("BitBucket getPullRequestDiff not yet implemented for prId={}", prId);
        return "";
    }

    /**
     * Obtém os commits de um PR no BitBucket.
     */
    public java.util.List<String> getPullRequestCommits(Project project, Long prId) {
        // TODO: Implementar chamada à API do BitBucket
        // GET /repositories/{workspace}/{repo_slug}/pullrequests/{pull_request_id}/commits
        log.warn("BitBucket getPullRequestCommits not yet implemented for prId={}", prId);
        return java.util.Collections.emptyList();
    }

    /**
     * Posta um comentário de review no BitBucket.
     */
    public void postReviewComment(Project project, Long prId, String comment) {
        // TODO: Implementar chamada à API do BitBucket
        // POST /repositories/{workspace}/{repo_slug}/pullrequests/{pull_request_id}/comments
        log.info("Posting review comment to BitBucket PR #{}: {}", prId, comment);
    }

    /**
     * Obtém os arquivos alterados de um PR.
     */
    public java.util.List<BitBucketFileChange> getPullRequestFiles(Project project, Long prId) {
        // TODO: Implementar chamada à API do BitBucket
        return java.util.Collections.emptyList();
    }

    /**
     * DTO para mudanças de arquivo.
     */
    public record BitBucketFileChange(
            String filename,
            String status,
            Integer additions,
            Integer deletions
    ) {}
}
