package com.pullwise.api.application.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.dto.BitBucketWebhookPayload;
import com.pullwise.api.application.service.config.ConfigurationResolver;
import com.pullwise.api.domain.constants.ConfigKeys;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.model.User;
import com.pullwise.api.domain.repository.ProjectRepository;
import com.pullwise.api.domain.repository.PullRequestRepository;
import com.pullwise.api.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Serviço para integração com API do BitBucket.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BitBucketService {

    private static final String BITBUCKET_API = "https://api.bitbucket.org/2.0";

    private final RestTemplate restTemplate;
    private final ProjectRepository projectRepository;
    private final PullRequestRepository pullRequestRepository;
    private final UserRepository userRepository;
    private final ConfigurationResolver configurationResolver;
    private final ObjectMapper objectMapper;

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
     * GET /2.0/repositories/{workspace}/{repo_slug}/pullrequests/{id}/diff
     */
    public String getPullRequestDiff(Project project, Long prId) {
        String[] repoInfo = extractWorkspaceAndSlug(project);
        if (repoInfo == null) return "";

        String url = String.format("%s/repositories/%s/%s/pullrequests/%d/diff",
                BITBUCKET_API, repoInfo[0], repoInfo[1], prId);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(project));
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody() != null ? response.getBody() : "";
        } catch (Exception e) {
            log.error("Failed to fetch BitBucket PR diff for PR #{}: {}", prId, e.getMessage());
            return "";
        }
    }

    /**
     * Busca diffs do PR no formato compatível com GitHubService.FileDiff.
     * Usa o endpoint diffstat para obter file-level info + diff para os patches.
     */
    public List<GitHubService.FileDiff> fetchPullRequestDiffs(Project project, int prNumber) {
        String[] repoInfo = extractWorkspaceAndSlug(project);
        if (repoInfo == null) return List.of();

        List<GitHubService.FileDiff> diffs = new ArrayList<>();

        try {
            // Buscar diffstat para info por arquivo
            String diffstatUrl = String.format("%s/repositories/%s/%s/pullrequests/%d/diffstat",
                    BITBUCKET_API, repoInfo[0], repoInfo[1], prNumber);

            HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(project));
            ResponseEntity<String> diffstatResponse = restTemplate.exchange(diffstatUrl, HttpMethod.GET, entity, String.class);

            if (diffstatResponse.getBody() != null) {
                JsonNode root = objectMapper.readTree(diffstatResponse.getBody());
                JsonNode values = root.get("values");

                if (values != null && values.isArray()) {
                    // Buscar o diff completo para os patches
                    String rawDiff = getPullRequestDiff(project, (long) prNumber);
                    Map<String, String> patchesByFile = parseDiffPatches(rawDiff);

                    for (JsonNode entry : values) {
                        String filename = "";
                        String status = "modified";
                        if (entry.has("new") && entry.get("new").has("path")) {
                            filename = entry.get("new").get("path").asText();
                        }
                        if (entry.has("status")) {
                            status = entry.get("status").asText();
                        }
                        int additions = entry.has("lines_added") ? entry.get("lines_added").asInt() : 0;
                        int deletions = entry.has("lines_removed") ? entry.get("lines_removed").asInt() : 0;

                        String patch = patchesByFile.getOrDefault(filename, "");

                        diffs.add(new GitHubService.FileDiff(filename, status, additions, deletions, patch));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch BitBucket PR diffs for PR #{}: {}", prNumber, e.getMessage());
        }

        return diffs;
    }

    /**
     * Obtém os commits de um PR no BitBucket.
     * GET /2.0/repositories/{workspace}/{repo_slug}/pullrequests/{id}/commits
     */
    public List<String> getPullRequestCommits(Project project, Long prId) {
        String[] repoInfo = extractWorkspaceAndSlug(project);
        if (repoInfo == null) return List.of();

        String url = String.format("%s/repositories/%s/%s/pullrequests/%d/commits",
                BITBUCKET_API, repoInfo[0], repoInfo[1], prId);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(project));
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            List<String> commitShas = new ArrayList<>();
            if (response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode values = root.get("values");
                if (values != null && values.isArray()) {
                    for (JsonNode commit : values) {
                        if (commit.has("hash")) {
                            commitShas.add(commit.get("hash").asText());
                        }
                    }
                }
            }
            return commitShas;
        } catch (Exception e) {
            log.error("Failed to fetch BitBucket PR commits for PR #{}: {}", prId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Posta um comentário de review no BitBucket.
     * POST /2.0/repositories/{workspace}/{repo_slug}/pullrequests/{id}/comments
     */
    public String postReviewComment(Project project, Long prId, String comment) {
        String[] repoInfo = extractWorkspaceAndSlug(project);
        if (repoInfo == null) return null;

        String url = String.format("%s/repositories/%s/%s/pullrequests/%d/comments",
                BITBUCKET_API, repoInfo[0], repoInfo[1], prId);

        try {
            Map<String, Object> body = Map.of("content", Map.of("raw", comment));
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createAuthHeaders(project));
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String commentId = root.has("id") ? root.get("id").asText() : null;
                log.info("Posted review comment {} to BitBucket PR #{}", commentId, prId);
                return commentId;
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to post BitBucket PR comment for PR #{}: {}", prId, e.getMessage());
            return null;
        }
    }

    /**
     * Obtém os arquivos alterados de um PR.
     * GET /2.0/repositories/{workspace}/{repo_slug}/pullrequests/{id}/diffstat
     */
    public List<BitBucketFileChange> getPullRequestFiles(Project project, Long prId) {
        String[] repoInfo = extractWorkspaceAndSlug(project);
        if (repoInfo == null) return List.of();

        String url = String.format("%s/repositories/%s/%s/pullrequests/%d/diffstat",
                BITBUCKET_API, repoInfo[0], repoInfo[1], prId);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(project));
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            List<BitBucketFileChange> files = new ArrayList<>();
            if (response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode values = root.get("values");
                if (values != null && values.isArray()) {
                    for (JsonNode entry : values) {
                        String filename = entry.has("new") && entry.get("new").has("path")
                                ? entry.get("new").get("path").asText() : "";
                        String status = entry.has("status") ? entry.get("status").asText() : "modified";
                        int additions = entry.has("lines_added") ? entry.get("lines_added").asInt() : 0;
                        int deletions = entry.has("lines_removed") ? entry.get("lines_removed").asInt() : 0;
                        files.add(new BitBucketFileChange(filename, status, additions, deletions));
                    }
                }
            }
            return files;
        } catch (Exception e) {
            log.error("Failed to fetch BitBucket PR files for PR #{}: {}", prId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Posta comentários inline em um PR do BitBucket.
     * POST /2.0/repositories/{workspace}/{repo_slug}/pullrequests/{id}/comments
     * com inline.path e inline.to para posicionar o comentário.
     */
    public void postInlineComments(Project project, Long prId,
                                    List<GitHubService.InlineComment> comments) {
        String[] repoInfo = extractWorkspaceAndSlug(project);
        if (repoInfo == null) return;

        for (GitHubService.InlineComment comment : comments) {
            String url = String.format("%s/repositories/%s/%s/pullrequests/%d/comments",
                    BITBUCKET_API, repoInfo[0], repoInfo[1], prId);

            try {
                Map<String, Object> body = Map.of(
                        "content", Map.of("raw", comment.body()),
                        "inline", Map.of(
                                "path", comment.path(),
                                "to", comment.line()
                        )
                );

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createAuthHeaders(project));
                ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.debug("Posted inline comment on BitBucket PR #{} at {}:{}",
                            prId, comment.path(), comment.line());
                }
            } catch (Exception e) {
                log.warn("Failed to post inline comment on BitBucket PR #{} at {}:{}: {}",
                        prId, comment.path(), comment.line(), e.getMessage());
            }
        }
    }

    /**
     * Cria headers de autenticação para a API do BitBucket.
     * Usa App Password via Basic Auth com token da configuração.
     */
    private HttpHeaders createAuthHeaders(Project project) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String token = null;
        if (project != null && project.getId() != null) {
            token = configurationResolver.getConfig(project.getId(), ConfigKeys.BITBUCKET_TOKEN);
        }
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    /**
     * Extrai workspace e repo slug da URL do repositório.
     * Suporta formatos: https://bitbucket.org/workspace/repo ou workspace/repo
     */
    private String[] extractWorkspaceAndSlug(Project project) {
        String repoUrl = project.getRepositoryUrl();
        if (repoUrl == null || repoUrl.isBlank()) {
            log.warn("No repository URL for project {}", project.getName());
            return null;
        }

        // Extrair workspace/slug da URL
        String path = repoUrl;
        if (path.contains("bitbucket.org/")) {
            path = path.substring(path.indexOf("bitbucket.org/") + "bitbucket.org/".length());
        }
        path = path.replaceAll("^/|/$|\\.git$", "");

        String[] parts = path.split("/");
        if (parts.length < 2) {
            log.warn("Could not extract workspace/slug from URL: {}", repoUrl);
            return null;
        }
        return new String[]{parts[0], parts[1]};
    }

    /**
     * Parseia o diff unificado em patches por arquivo.
     */
    private Map<String, String> parseDiffPatches(String rawDiff) {
        Map<String, String> patches = new HashMap<>();
        if (rawDiff == null || rawDiff.isBlank()) return patches;

        String[] lines = rawDiff.split("\n");
        String currentFile = null;
        StringBuilder currentPatch = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("diff --git")) {
                if (currentFile != null) {
                    patches.put(currentFile, currentPatch.toString().trim());
                }
                // Extract filename from "diff --git a/path b/path"
                int bIndex = line.lastIndexOf(" b/");
                currentFile = bIndex >= 0 ? line.substring(bIndex + 3) : null;
                currentPatch = new StringBuilder();
            } else if (currentFile != null) {
                currentPatch.append(line).append("\n");
            }
        }
        if (currentFile != null) {
            patches.put(currentFile, currentPatch.toString().trim());
        }

        return patches;
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
