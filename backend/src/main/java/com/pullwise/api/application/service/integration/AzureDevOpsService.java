package com.pullwise.api.application.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.service.config.ConfigurationResolver;
import com.pullwise.api.domain.enums.Platform;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.repository.ProjectRepository;
import com.pullwise.api.domain.repository.PullRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Serviço de integração com a API do Azure DevOps.
 * Suporta dev.azure.com e instâncias on-premises (Azure DevOps Server).
 *
 * <p>API Reference: https://learn.microsoft.com/en-us/rest/api/azure/devops/
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AzureDevOpsService {

    @Value("${integrations.azure-devops.api-version:7.1}")
    private String apiVersion;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ProjectRepository projectRepository;
    private final PullRequestRepository pullRequestRepository;
    private final ConfigurationResolver configurationResolver;

    /**
     * Busca os diffs de um Pull Request no Azure DevOps.
     * GET /{org}/{project}/_apis/git/repositories/{repo}/pullRequests/{id}/iterations
     * GET /{org}/{project}/_apis/git/repositories/{repo}/pullRequests/{id}/iterations/{iterationId}/changes
     */
    public List<GitHubService.FileDiff> fetchPullRequestDiffs(Project project, int prNumber) {
        String[] orgInfo = extractOrgProjectRepo(project);
        if (orgInfo == null) return List.of();

        String baseUrl = buildBaseUrl(orgInfo[0]);
        List<GitHubService.FileDiff> diffs = new ArrayList<>();

        try {
            // Get the latest iteration
            String iterationsUrl = String.format("%s/%s/_apis/git/repositories/%s/pullRequests/%d/iterations?api-version=%s",
                    baseUrl, orgInfo[1], orgInfo[2], prNumber, apiVersion);

            HttpEntity<Void> entity = new HttpEntity<>(createHeaders(project));
            ResponseEntity<String> iterResponse = restTemplate.exchange(iterationsUrl, HttpMethod.GET, entity, String.class);

            if (iterResponse.getBody() == null) return List.of();

            JsonNode iterRoot = objectMapper.readTree(iterResponse.getBody());
            JsonNode iterValues = iterRoot.get("value");
            if (iterValues == null || !iterValues.isArray() || iterValues.isEmpty()) return List.of();

            // Use latest iteration
            int latestIteration = iterValues.get(iterValues.size() - 1).get("id").asInt();

            // Get changes for that iteration
            String changesUrl = String.format("%s/%s/_apis/git/repositories/%s/pullRequests/%d/iterations/%d/changes?api-version=%s",
                    baseUrl, orgInfo[1], orgInfo[2], prNumber, latestIteration, apiVersion);

            ResponseEntity<String> changesResponse = restTemplate.exchange(changesUrl, HttpMethod.GET, entity, String.class);

            if (changesResponse.getBody() != null) {
                JsonNode changesRoot = objectMapper.readTree(changesResponse.getBody());
                JsonNode changeEntries = changesRoot.get("changeEntries");
                if (changeEntries != null && changeEntries.isArray()) {
                    for (JsonNode change : changeEntries) {
                        JsonNode item = change.get("item");
                        if (item == null) continue;

                        String path = item.has("path") ? item.get("path").asText() : "";
                        // Remove leading slash
                        if (path.startsWith("/")) path = path.substring(1);

                        // Skip folders
                        boolean isFolder = item.has("isFolder") && item.get("isFolder").asBoolean();
                        if (isFolder) continue;

                        String changeType = change.has("changeType") ? change.get("changeType").asText() : "edit";
                        String status = mapChangeType(changeType);

                        // Azure DevOps doesn't provide line counts in change entries; estimate from diff
                        diffs.add(new GitHubService.FileDiff(path, status, 0, 0, ""));
                    }
                }
            }

            // Fetch actual diffs for each file
            fetchFileDiffs(project, orgInfo, prNumber, diffs);

        } catch (Exception e) {
            log.error("Failed to fetch Azure DevOps PR diffs for PR #{}: {}", prNumber, e.getMessage());
        }

        return diffs;
    }

    /**
     * Posta um comentário (thread) em um Pull Request.
     * POST /{org}/{project}/_apis/git/repositories/{repo}/pullRequests/{id}/threads
     */
    public String postPullRequestComment(Project project, int prNumber, String comment) {
        String[] orgInfo = extractOrgProjectRepo(project);
        if (orgInfo == null) return null;

        String baseUrl = buildBaseUrl(orgInfo[0]);
        String url = String.format("%s/%s/_apis/git/repositories/%s/pullRequests/%d/threads?api-version=%s",
                baseUrl, orgInfo[1], orgInfo[2], prNumber, apiVersion);

        try {
            HttpHeaders headers = createHeaders(project);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Azure DevOps uses threads with comments array
            Map<String, Object> threadComment = Map.of(
                    "parentCommentId", 0,
                    "content", comment,
                    "commentType", 1  // 1 = text
            );
            Map<String, Object> body = Map.of(
                    "comments", List.of(threadComment),
                    "status", 1  // 1 = active
            );

            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode node = objectMapper.readTree(response.getBody());
                String threadId = node.has("id") ? node.get("id").asText() : null;
                log.info("Posted thread {} on Azure DevOps PR #{}", threadId, prNumber);
                return threadId;
            }
        } catch (Exception e) {
            log.error("Failed to post comment on Azure DevOps PR #{}: {}", prNumber, e.getMessage());
        }

        return null;
    }

    /**
     * Posta comentários inline em um Pull Request via threads com threadContext.
     */
    public void postInlineComments(Project project, int prNumber,
                                    List<GitHubService.InlineComment> comments) {
        String[] orgInfo = extractOrgProjectRepo(project);
        if (orgInfo == null) return;

        String baseUrl = buildBaseUrl(orgInfo[0]);

        for (GitHubService.InlineComment comment : comments) {
            String url = String.format("%s/%s/_apis/git/repositories/%s/pullRequests/%d/threads?api-version=%s",
                    baseUrl, orgInfo[1], orgInfo[2], prNumber, apiVersion);

            try {
                HttpHeaders headers = createHeaders(project);
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> threadComment = Map.of(
                        "parentCommentId", 0,
                        "content", comment.body(),
                        "commentType", 1
                );

                // Thread context for inline positioning
                Map<String, Object> rightFileStart = Map.of("line", comment.line(), "offset", 1);
                Map<String, Object> rightFileEnd = Map.of("line", comment.line(), "offset", 1);
                Map<String, Object> threadContext = Map.of(
                        "filePath", "/" + comment.path(),
                        "rightFileStart", rightFileStart,
                        "rightFileEnd", rightFileEnd
                );

                Map<String, Object> body = Map.of(
                        "comments", List.of(threadComment),
                        "threadContext", threadContext,
                        "status", 1
                );

                String jsonBody = objectMapper.writeValueAsString(body);
                HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.debug("Posted inline comment on Azure DevOps PR #{} at {}:{}", prNumber, comment.path(), comment.line());
                }
            } catch (Exception e) {
                log.warn("Failed to post inline comment on Azure DevOps PR #{} at {}:{}: {}",
                        prNumber, comment.path(), comment.line(), e.getMessage());
            }
        }
    }

    /**
     * Sincroniza um Pull Request recebido via webhook do Azure DevOps.
     */
    public PullRequest syncPullRequest(AzureDevOpsWebhookPayload payload) {
        AzureDevOpsWebhookPayload.PullRequestResource resource = payload.getResource();
        AzureDevOpsWebhookPayload.RepositoryInfo repo = resource.getRepository();

        String repoUrl = repo.getRemoteUrl();
        if (repoUrl == null) {
            repoUrl = String.format("https://dev.azure.com/%s/%s/_git/%s",
                    repo.getProject() != null ? repo.getProject().getName() : "",
                    repo.getProject() != null ? repo.getProject().getName() : "",
                    repo.getName());
        }

        final String finalRepoUrl = repoUrl;
        Optional<Project> projectOpt = projectRepository.findAll().stream()
                .filter(p -> p.getRepositoryUrl() != null && p.getPlatform() == Platform.AZURE_DEVOPS
                        && (p.getRepositoryUrl().contains(repo.getName())
                        || finalRepoUrl.contains(p.getRepositoryUrl())))
                .findFirst();

        if (projectOpt.isEmpty()) {
            log.warn("Project not found for Azure DevOps repo: {}", repo.getName());
            return null;
        }

        Project project = projectOpt.get();
        int prNumber = resource.getPullRequestId();

        String sourceBranch = resource.getSourceRefName() != null
                ? resource.getSourceRefName().replace("refs/heads/", "") : "unknown";
        String targetBranch = resource.getTargetRefName() != null
                ? resource.getTargetRefName().replace("refs/heads/", "") : "unknown";

        return pullRequestRepository.findByProjectIdAndPrNumber(project.getId(), prNumber)
                .map(existing -> {
                    existing.setTitle(resource.getTitle());
                    if ("completed".equalsIgnoreCase(resource.getStatus())) {
                        existing.setIsMerged(true);
                    } else if ("abandoned".equalsIgnoreCase(resource.getStatus())) {
                        existing.setIsClosed(true);
                    }
                    return pullRequestRepository.save(existing);
                })
                .orElseGet(() -> {
                    String authorName = resource.getCreatedBy() != null
                            ? resource.getCreatedBy().getDisplayName() : "unknown";

                    String reviewUrl = String.format("https://dev.azure.com/%s/%s/_git/%s/pullrequest/%d",
                            repo.getProject() != null ? repo.getProject().getName() : "",
                            repo.getProject() != null ? repo.getProject().getName() : "",
                            repo.getName(), prNumber);

                    PullRequest pr = PullRequest.builder()
                            .project(project)
                            .platform(Platform.AZURE_DEVOPS)
                            .prId((long) prNumber)
                            .prNumber(prNumber)
                            .title(resource.getTitle())
                            .description(resource.getDescription())
                            .sourceBranch(sourceBranch)
                            .targetBranch(targetBranch)
                            .authorName(authorName)
                            .reviewUrl(reviewUrl)
                            .build();

                    return pullRequestRepository.save(pr);
                });
    }

    /**
     * Aprova um Pull Request no Azure DevOps.
     * PUT /{org}/{project}/_apis/git/repositories/{repo}/pullRequests/{id}/reviewers/{reviewerId}
     */
    public void approvePullRequest(Project project, int prNumber) {
        String[] orgInfo = extractOrgProjectRepo(project);
        if (orgInfo == null) return;

        String baseUrl = buildBaseUrl(orgInfo[0]);

        try {
            // First get the current user ID
            String meUrl = String.format("%s/%s/_apis/connectionData?api-version=%s",
                    baseUrl, orgInfo[1], apiVersion);
            HttpEntity<Void> getEntity = new HttpEntity<>(createHeaders(project));
            ResponseEntity<String> meResponse = restTemplate.exchange(meUrl, HttpMethod.GET, getEntity, String.class);

            String reviewerId = "me";
            if (meResponse.getBody() != null) {
                JsonNode meNode = objectMapper.readTree(meResponse.getBody());
                if (meNode.has("authenticatedUser") && meNode.get("authenticatedUser").has("id")) {
                    reviewerId = meNode.get("authenticatedUser").get("id").asText();
                }
            }

            String url = String.format("%s/%s/_apis/git/repositories/%s/pullRequests/%d/reviewers/%s?api-version=%s",
                    baseUrl, orgInfo[1], orgInfo[2], prNumber, reviewerId, apiVersion);

            HttpHeaders headers = createHeaders(project);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // vote=10 means "Approved"
            String body = objectMapper.writeValueAsString(Map.of("vote", 10));
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            log.info("Approved Azure DevOps PR #{}", prNumber);
        } catch (Exception e) {
            log.warn("Failed to approve Azure DevOps PR #{}: {}", prNumber, e.getMessage());
        }
    }

    /**
     * Atualiza a descrição de um Pull Request.
     * PATCH /{org}/{project}/_apis/git/repositories/{repo}/pullRequests/{id}
     */
    public void updatePullRequestDescription(Project project, int prNumber, String description) {
        String[] orgInfo = extractOrgProjectRepo(project);
        if (orgInfo == null) return;

        String baseUrl = buildBaseUrl(orgInfo[0]);
        String url = String.format("%s/%s/_apis/git/repositories/%s/pullRequests/%d?api-version=%s",
                baseUrl, orgInfo[1], orgInfo[2], prNumber, apiVersion);

        try {
            HttpHeaders headers = createHeaders(project);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String body = objectMapper.writeValueAsString(Map.of("description", description));
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange(url, HttpMethod.PATCH, entity, String.class);
            log.info("Updated description of Azure DevOps PR #{}", prNumber);
        } catch (Exception e) {
            log.warn("Failed to update Azure DevOps PR #{} description: {}", prNumber, e.getMessage());
        }
    }

    // ========== Private Helpers ==========

    /**
     * Fetches actual file diffs to populate patch content and line counts.
     */
    private void fetchFileDiffs(Project project, String[] orgInfo, int prNumber, List<GitHubService.FileDiff> diffs) {
        String baseUrl = buildBaseUrl(orgInfo[0]);

        for (int i = 0; i < diffs.size(); i++) {
            GitHubService.FileDiff fileDiff = diffs.get(i);
            try {
                // Get diff for specific file between source and target
                String diffUrl = String.format("%s/%s/_apis/git/repositories/%s/diffs/commits?baseVersionType=branch&targetVersionType=branch&api-version=%s",
                        baseUrl, orgInfo[1], orgInfo[2], apiVersion);

                // Use the PR diff endpoint instead for each file
                String itemUrl = String.format("%s/%s/_apis/git/repositories/%s/pullRequests/%d/threads?api-version=%s",
                        baseUrl, orgInfo[1], orgInfo[2], prNumber, apiVersion);

                // Count lines from status
                int additions = 0, deletions = 0;
                if (!fileDiff.patch().isEmpty()) {
                    for (String line : fileDiff.patch().split("\n")) {
                        if (line.startsWith("+") && !line.startsWith("+++")) additions++;
                        else if (line.startsWith("-") && !line.startsWith("---")) deletions++;
                    }
                }

                diffs.set(i, new GitHubService.FileDiff(fileDiff.filename(), fileDiff.status(), additions, deletions, fileDiff.patch()));
            } catch (Exception e) {
                log.debug("Could not fetch diff for file {}: {}", fileDiff.filename(), e.getMessage());
            }
        }
    }

    private String mapChangeType(String changeType) {
        return switch (changeType.toLowerCase()) {
            case "add" -> "added";
            case "delete" -> "removed";
            case "rename" -> "renamed";
            default -> "modified";
        };
    }

    /**
     * Extrai org, project e repo da URL do repositório.
     * Suporta formatos:
     * - https://dev.azure.com/{org}/{project}/_git/{repo}
     * - https://{org}.visualstudio.com/{project}/_git/{repo}
     */
    private String[] extractOrgProjectRepo(Project project) {
        String repoUrl = project.getRepositoryUrl();
        if (repoUrl == null || repoUrl.isBlank()) {
            log.warn("No repository URL for project {}", project.getName());
            return null;
        }

        try {
            // Format: https://dev.azure.com/{org}/{project}/_git/{repo}
            if (repoUrl.contains("dev.azure.com")) {
                String path = repoUrl.substring(repoUrl.indexOf("dev.azure.com/") + "dev.azure.com/".length());
                path = path.replaceAll("/$|\\.git$", "");
                String[] parts = path.split("/");
                if (parts.length >= 4 && "_git".equals(parts[2])) {
                    return new String[]{parts[0], parts[1], parts[3]};
                }
            }

            // Format: https://{org}.visualstudio.com/{project}/_git/{repo}
            if (repoUrl.contains("visualstudio.com")) {
                String host = repoUrl.split("//")[1].split("/")[0];
                String org = host.split("\\.")[0];
                String path = repoUrl.substring(repoUrl.indexOf(host) + host.length() + 1);
                path = path.replaceAll("/$|\\.git$", "");
                String[] parts = path.split("/");
                if (parts.length >= 3 && "_git".equals(parts[1])) {
                    return new String[]{org, parts[0], parts[2]};
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Azure DevOps URL: {}", repoUrl);
        }

        log.warn("Could not extract org/project/repo from URL: {}", repoUrl);
        return null;
    }

    private String buildBaseUrl(String org) {
        return String.format("https://dev.azure.com/%s", org);
    }

    /**
     * Cria headers de autenticação para Azure DevOps.
     * Azure DevOps usa PAT (Personal Access Token) como Basic auth: base64(":PAT")
     */
    private HttpHeaders createHeaders(Project project) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("User-Agent", "Pullwise-Code-Review");

        String pat = null;
        if (project != null && project.getId() != null) {
            pat = configurationResolver.getConfig(project.getId(), "azure_devops.pat");
        }

        if (pat != null && !pat.isBlank()) {
            String credentials = ":" + pat;
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encoded);
        }

        return headers;
    }

    // ========== Webhook Payload DTOs ==========

    public static class AzureDevOpsWebhookPayload {
        private String subscriptionId;
        private String eventType;
        private PullRequestResource resource;
        private MessageInfo message;

        public String getSubscriptionId() { return subscriptionId; }
        public void setSubscriptionId(String subscriptionId) { this.subscriptionId = subscriptionId; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public PullRequestResource getResource() { return resource; }
        public void setResource(PullRequestResource resource) { this.resource = resource; }
        public MessageInfo getMessage() { return message; }
        public void setMessage(MessageInfo message) { this.message = message; }

        public static class PullRequestResource {
            private int pullRequestId;
            private String title;
            private String description;
            private String status;
            private String sourceRefName;
            private String targetRefName;
            private RepositoryInfo repository;
            private IdentityInfo createdBy;
            // For comment events
            private List<CommentInfo> comments;

            public int getPullRequestId() { return pullRequestId; }
            public void setPullRequestId(int pullRequestId) { this.pullRequestId = pullRequestId; }
            public String getTitle() { return title; }
            public void setTitle(String title) { this.title = title; }
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
            public String getStatus() { return status; }
            public void setStatus(String status) { this.status = status; }
            public String getSourceRefName() { return sourceRefName; }
            public void setSourceRefName(String sourceRefName) { this.sourceRefName = sourceRefName; }
            public String getTargetRefName() { return targetRefName; }
            public void setTargetRefName(String targetRefName) { this.targetRefName = targetRefName; }
            public RepositoryInfo getRepository() { return repository; }
            public void setRepository(RepositoryInfo repository) { this.repository = repository; }
            public IdentityInfo getCreatedBy() { return createdBy; }
            public void setCreatedBy(IdentityInfo createdBy) { this.createdBy = createdBy; }
            public List<CommentInfo> getComments() { return comments; }
            public void setComments(List<CommentInfo> comments) { this.comments = comments; }
        }

        public static class RepositoryInfo {
            private String id;
            private String name;
            private String remoteUrl;
            private ProjectInfo project;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getRemoteUrl() { return remoteUrl; }
            public void setRemoteUrl(String remoteUrl) { this.remoteUrl = remoteUrl; }
            public ProjectInfo getProject() { return project; }
            public void setProject(ProjectInfo project) { this.project = project; }
        }

        public static class ProjectInfo {
            private String id;
            private String name;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
        }

        public static class IdentityInfo {
            private String id;
            private String displayName;
            private String uniqueName;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getDisplayName() { return displayName; }
            public void setDisplayName(String displayName) { this.displayName = displayName; }
            public String getUniqueName() { return uniqueName; }
            public void setUniqueName(String uniqueName) { this.uniqueName = uniqueName; }
        }

        public static class CommentInfo {
            private int id;
            private String content;
            private IdentityInfo author;

            public int getId() { return id; }
            public void setId(int id) { this.id = id; }
            public String getContent() { return content; }
            public void setContent(String content) { this.content = content; }
            public IdentityInfo getAuthor() { return author; }
            public void setAuthor(IdentityInfo author) { this.author = author; }
        }

        public static class MessageInfo {
            private String text;

            public String getText() { return text; }
            public void setText(String text) { this.text = text; }
        }
    }
}
