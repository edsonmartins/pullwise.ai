package com.pullwise.api.application.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.service.config.ConfigurationResolver;
import com.pullwise.api.domain.constants.ConfigKeys;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço de integração com a API do GitLab.
 * Suporta GitLab.com e instâncias self-hosted.
 *
 * <p>API Reference: https://docs.gitlab.com/ee/api/
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitLabService {

    @Value("${integrations.gitlab.api-url:https://gitlab.com/api/v4}")
    private String gitlabApiUrl;

    @Value("${integrations.gitlab.token:}")
    private String gitlabToken;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ProjectRepository projectRepository;
    private final PullRequestRepository pullRequestRepository;
    private final ConfigurationResolver configurationResolver;

    /**
     * Verifica se o GitLab está configurado.
     */
    public boolean isConfigured() {
        return gitlabToken != null && !gitlabToken.isBlank();
    }

    /**
     * Busca os diffs de um Merge Request.
     * GET /projects/:id/merge_requests/:merge_request_iid/changes
     */
    public List<GitHubService.FileDiff> fetchMergeRequestDiffs(Project project, int mrIid) {
        String projectId = extractGitLabProjectId(project);
        String url = String.format("%s/projects/%s/merge_requests/%d/changes",
                gitlabApiUrl, projectId, mrIid);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders(project));
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseMergeRequestChanges(response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to fetch MR diffs for project {} MR !{}", projectId, mrIid, e);
        }

        return List.of();
    }

    /**
     * Posta um comentário (note) em um Merge Request.
     * POST /projects/:id/merge_requests/:merge_request_iid/notes
     */
    public String postMergeRequestComment(Project project, int mrIid, String comment) {
        String projectId = extractGitLabProjectId(project);
        String url = String.format("%s/projects/%s/merge_requests/%d/notes",
                gitlabApiUrl, projectId, mrIid);

        try {
            HttpHeaders headers = createHeaders(project);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String body = objectMapper.writeValueAsString(java.util.Map.of("body", comment));
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode node = objectMapper.readTree(response.getBody());
                String noteId = node.has("id") ? node.get("id").asText() : null;
                log.info("Posted comment {} on MR !{}", noteId, mrIid);
                return noteId;
            }
        } catch (Exception e) {
            log.error("Failed to post comment on MR !{}: {}", mrIid, e.getMessage());
        }

        return null;
    }

    /**
     * Busca os commits de um Merge Request.
     * GET /projects/:id/merge_requests/:merge_request_iid/commits
     */
    public List<String> fetchMergeRequestCommits(Project project, int mrIid) {
        String projectId = extractGitLabProjectId(project);
        String url = String.format("%s/projects/%s/merge_requests/%d/commits",
                gitlabApiUrl, projectId, mrIid);

        List<String> commits = new ArrayList<>();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders(project));
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.isArray()) {
                    for (JsonNode commit : root) {
                        if (commit.has("id")) {
                            commits.add(commit.get("id").asText());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch MR commits for MR !{}: {}", mrIid, e.getMessage());
        }

        return commits;
    }

    /**
     * Sincroniza um Merge Request recebido via webhook.
     */
    public PullRequest syncMergeRequest(GitLabWebhookPayload payload) {
        String projectIdStr = String.valueOf(payload.getProject().getId());

        Project project = projectRepository.findByRepositoryIdAndPlatform(
                projectIdStr, Platform.GITLAB
        ).orElseThrow(() -> new IllegalArgumentException(
                "Project not found for GitLab project ID: " + projectIdStr));

        GitLabWebhookPayload.MergeRequestAttributes mr = payload.getObjectAttributes();

        return pullRequestRepository.findByProjectIdAndPrNumber(
                project.getId(), mr.getIid()
        ).orElseGet(() -> {
            PullRequest newPr = PullRequest.builder()
                    .project(project)
                    .platform(Platform.GITLAB)
                    .prId(mr.getId())
                    .prNumber(mr.getIid())
                    .title(mr.getTitle())
                    .description(mr.getDescription())
                    .sourceBranch(mr.getSourceBranch())
                    .targetBranch(mr.getTargetBranch())
                    .authorName(payload.getUser() != null ? payload.getUser().getUsername() : "unknown")
                    .reviewUrl(mr.getUrl())
                    .build();

            return pullRequestRepository.save(newPr);
        });
    }

    /**
     * Posta comentários inline em um Merge Request via Discussions API.
     * POST /projects/:id/merge_requests/:merge_request_iid/discussions
     */
    public void postInlineComments(Project project, int mrIid,
                                    List<GitHubService.InlineComment> comments) {
        String projectId = extractGitLabProjectId(project);

        for (GitHubService.InlineComment comment : comments) {
            String url = String.format("%s/projects/%s/merge_requests/%d/discussions",
                    gitlabApiUrl, projectId, mrIid);

            try {
                HttpHeaders headers = createHeaders(project);
                headers.setContentType(MediaType.APPLICATION_JSON);

                // GitLab inline comment via Discussions API requires position object
                var position = java.util.Map.of(
                        "base_sha", "",
                        "start_sha", "",
                        "head_sha", "",
                        "position_type", "text",
                        "new_path", comment.path(),
                        "new_line", comment.line()
                );

                var body = java.util.Map.of(
                        "body", comment.body(),
                        "position", position
                );

                String jsonBody = objectMapper.writeValueAsString(body);
                HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

                ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.debug("Posted inline comment on MR !{} at {}:{}", mrIid, comment.path(), comment.line());
                }
            } catch (Exception e) {
                log.warn("Failed to post inline comment on MR !{} at {}:{}: {}",
                        mrIid, comment.path(), comment.line(), e.getMessage());
            }
        }
    }

    // ========== Private Helpers ==========

    private List<GitHubService.FileDiff> parseMergeRequestChanges(String json) {
        List<GitHubService.FileDiff> diffs = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode changes = root.get("changes");
            if (changes == null || !changes.isArray()) return diffs;

            for (JsonNode change : changes) {
                String oldPath = change.has("old_path") ? change.get("old_path").asText() : "";
                String newPath = change.has("new_path") ? change.get("new_path").asText() : "";
                String diff = change.has("diff") ? change.get("diff").asText() : "";
                boolean newFile = change.has("new_file") && change.get("new_file").asBoolean();
                boolean deletedFile = change.has("deleted_file") && change.get("deleted_file").asBoolean();
                boolean renamedFile = change.has("renamed_file") && change.get("renamed_file").asBoolean();

                String status = deletedFile ? "removed" : (newFile ? "added" : (renamedFile ? "renamed" : "modified"));
                String filename = newPath.isEmpty() ? oldPath : newPath;

                // Count additions and deletions from diff
                int additions = 0, deletions = 0;
                for (String line : diff.split("\n")) {
                    if (line.startsWith("+") && !line.startsWith("+++")) additions++;
                    else if (line.startsWith("-") && !line.startsWith("---")) deletions++;
                }

                diffs.add(new GitHubService.FileDiff(filename, status, additions, deletions, diff));
            }
        } catch (Exception e) {
            log.error("Error parsing GitLab MR changes: {}", e.getMessage());
        }

        return diffs;
    }

    /**
     * Extrai o project ID (URL-encoded path ou numeric ID) para a API do GitLab.
     */
    private String extractGitLabProjectId(Project project) {
        // Se tiver repositoryId numérico, usar diretamente
        if (project.getRepositoryId() != null) {
            try {
                Long.parseLong(project.getRepositoryId());
                return project.getRepositoryId();
            } catch (NumberFormatException e) {
                log.debug("Repository ID is not numeric, falling back to URL extraction");
            }
        }

        // Extrair namespace/project do URL e URL-encode
        String repoUrl = project.getRepositoryUrl();
        if (repoUrl != null) {
            String path = repoUrl
                    .replaceFirst("https?://[^/]+/", "")
                    .replace(".git", "");
            return URLEncoder.encode(path, StandardCharsets.UTF_8);
        }

        return project.getName();
    }

    private HttpHeaders createHeaders(Project project) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("User-Agent", "Pullwise-Code-Review");

        // Tentar token do projeto primeiro, depois global
        String token = gitlabToken;
        if (project.getOrganization() != null) {
            String orgToken = configurationResolver.getConfig(
                    project.getId(), ConfigKeys.GITLAB_TOKEN);
            if (orgToken != null && !orgToken.isBlank()) {
                token = orgToken;
            }
        }

        if (token != null && !token.isBlank()) {
            headers.set("PRIVATE-TOKEN", token);
        }

        return headers;
    }

    // ========== Webhook Payload DTOs ==========

    public static class GitLabWebhookPayload {
        private String objectKind;
        private String eventType;
        private ProjectDTO project;
        private MergeRequestAttributes objectAttributes;
        private UserDTO user;

        public String getObjectKind() { return objectKind; }
        public void setObjectKind(String objectKind) { this.objectKind = objectKind; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public ProjectDTO getProject() { return project; }
        public void setProject(ProjectDTO project) { this.project = project; }
        public MergeRequestAttributes getObjectAttributes() { return objectAttributes; }
        public void setObjectAttributes(MergeRequestAttributes objectAttributes) { this.objectAttributes = objectAttributes; }
        public UserDTO getUser() { return user; }
        public void setUser(UserDTO user) { this.user = user; }

        public static class ProjectDTO {
            private Long id;
            private String name;
            private String pathWithNamespace;
            private String webUrl;

            public Long getId() { return id; }
            public void setId(Long id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getPathWithNamespace() { return pathWithNamespace; }
            public void setPathWithNamespace(String pathWithNamespace) { this.pathWithNamespace = pathWithNamespace; }
            public String getWebUrl() { return webUrl; }
            public void setWebUrl(String webUrl) { this.webUrl = webUrl; }
        }

        public static class MergeRequestAttributes {
            private Long id;
            private Integer iid;
            private String title;
            private String description;
            private String sourceBranch;
            private String targetBranch;
            private String state;
            private String action;
            private String url;

            public Long getId() { return id; }
            public void setId(Long id) { this.id = id; }
            public Integer getIid() { return iid; }
            public void setIid(Integer iid) { this.iid = iid; }
            public String getTitle() { return title; }
            public void setTitle(String title) { this.title = title; }
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
            public String getSourceBranch() { return sourceBranch; }
            public void setSourceBranch(String sourceBranch) { this.sourceBranch = sourceBranch; }
            public String getTargetBranch() { return targetBranch; }
            public void setTargetBranch(String targetBranch) { this.targetBranch = targetBranch; }
            public String getState() { return state; }
            public void setState(String state) { this.state = state; }
            public String getAction() { return action; }
            public void setAction(String action) { this.action = action; }
            public String getUrl() { return url; }
            public void setUrl(String url) { this.url = url; }
        }

        public static class UserDTO {
            private String username;
            private String name;

            public String getUsername() { return username; }
            public void setUsername(String username) { this.username = username; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
        }
    }
}
