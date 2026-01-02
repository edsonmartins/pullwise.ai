package com.pullwise.api.application.service.integration;

import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.repository.ProjectRepository;
import com.pullwise.api.domain.repository.PullRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Serviço de integração com a API do GitHub.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    @Value("${integrations.github.api-url:https://api.github.com}")
    private String githubApiUrl;

    @Value("${integrations.github.app-id}")
    private String githubAppId;

    @Value("${integrations.github.private-key}")
    private String githubPrivateKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ProjectRepository projectRepository;
    private final PullRequestRepository pullRequestRepository;

    /**
     * Busca os diffs de um Pull Request.
     */
    public List<FileDiff> fetchPullRequestDiffs(Project project, int prNumber) {
        String owner = extractOwner(project.getRepositoryUrl());
        String repo = extractRepo(project.getRepositoryUrl());

        String url = String.format("%s/repos/%s/%s/pulls/%d/files",
                githubApiUrl, owner, repo, prNumber);

        HttpHeaders headers = createHeaders(project);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            FileDiff[] diffs = restTemplate.exchange(url, HttpMethod.GET, entity,
                    FileDiff[].class).getBody();

            return diffs != null ? List.of(diffs) : List.of();

        } catch (Exception e) {
            log.error("Failed to fetch PR files for {}/{} #{}", owner, repo, prNumber, e);
            return List.of();
        }
    }

    /**
     * Posta um comentário em um Pull Request.
     */
    public String postPullRequestComment(Project project, int prNumber, String comment) {
        String owner = extractOwner(project.getRepositoryUrl());
        String repo = extractRepo(project.getRepositoryUrl());

        String url = String.format("%s/repos/%s/%s/issues/%d/comments",
                githubApiUrl, owner, repo, prNumber);

        HttpHeaders headers = createHeaders(project);
        CommentRequest body = new CommentRequest(comment);
        HttpEntity<CommentRequest> entity = new HttpEntity<>(body, headers);

        try {
            CommentResponse response = restTemplate.postForObject(url, entity,
                    CommentResponse.class);

            return response != null ? String.valueOf(response.id()) : null;

        } catch (Exception e) {
            log.error("Failed to post comment for {}/{} #{}", owner, repo, prNumber, e);
            throw new RuntimeException("Failed to post comment", e);
        }
    }

    /**
     * Sincroniza um PR recebido via webhook.
     */
    public com.pullwise.api.domain.model.PullRequest syncPullRequest(GitHubWebhookPayload payload) {
        Project project = projectRepository.findByRepositoryIdAndPlatform(
                String.valueOf(payload.getRepository().getId()),
                com.pullwise.api.domain.enums.Platform.GITHUB
        ).orElseThrow(() -> new IllegalArgumentException("Project not found"));

        return pullRequestRepository.findByProjectIdAndPrNumber(
                project.getId(),
                payload.getPullRequest().getNumber()
        ).orElseGet(() -> {
            com.pullwise.api.domain.model.PullRequest newPr = com.pullwise.api.domain.model.PullRequest.builder()
                    .project(project)
                    .platform(com.pullwise.api.domain.enums.Platform.GITHUB)
                    .prId(payload.getPullRequest().getId())
                    .prNumber(payload.getPullRequest().getNumber())
                    .title(payload.getPullRequest().getTitle())
                    .description(payload.getPullRequest().getBody())
                    .sourceBranch(payload.getPullRequest().getHead().getRef())
                    .targetBranch(payload.getPullRequest().getBase().getRef())
                    .authorName(payload.getPullRequest().getUser().getLogin())
                    .reviewUrl(payload.getPullRequest().getHtmlUrl())
                    .build();

            return pullRequestRepository.save(newPr);
        });
    }

    /**
     * Extrai o owner da URL do repositório.
     */
    private String extractOwner(String repoUrl) {
        // https://github.com/owner/repo.git -> owner
        String path = repoUrl.replace("https://github.com/", "")
                .replace(".git", "");
        return path.split("/")[0];
    }

    /**
     * Extrai o nome do repositório da URL.
     */
    private String extractRepo(String repoUrl) {
        // https://github.com/owner/repo.git -> repo
        String path = repoUrl.replace("https://github.com/", "")
                .replace(".git", "");
        return path.split("/")[1];
    }

    /**
     * Cria headers com autenticação.
     */
    private HttpHeaders createHeaders(Project project) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.set("User-Agent", "Pullwise-Code-Review");

        if (project.getGithubInstallationId() != null) {
            String token = generateInstallationToken(project.getGithubInstallationId());
            headers.set("Authorization", "Bearer " + token);
        }

        return headers;
    }

    /**
     * Gera um token de instalação do GitHub App.
     */
    private String generateInstallationToken(Long installationId) {
        // Em produção, isso geraria um JWT assinado com a chave privada do GitHub App
        // e trocaria por um installation token
        // Por simplicidade, retornando um placeholder
        return "ghp_" + installationId;
    }

    public record FileDiff(
            String filename,
            String status,
            int additions,
            int deletions,
            String patch
    ) {}

    public record CommentRequest(String body) {}
    public record CommentResponse(Long id, String body) {}

    public static class GitHubWebhookPayload {
        private Repository repository;
        private PullRequestDTO pullRequest;
        private String action;

        public Repository getRepository() { return repository; }
        public void setRepository(Repository repository) { this.repository = repository; }
        public PullRequestDTO getPullRequest() { return pullRequest; }
        public void setPullRequest(PullRequestDTO pullRequest) { this.pullRequest = pullRequest; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public static class Repository {
            private Long id;
            private String name;
            private String fullName;

            public Long getId() { return id; }
            public void setId(Long id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getFullName() { return fullName; }
            public void setFullName(String fullName) { this.fullName = fullName; }
        }

        public static class PullRequestDTO {
            private Long id;
            private Integer number;
            private String title;
            private String body;
            private Head head;
            private Base base;
            private User user;
            private String htmlUrl;
            private String state;

            public Long getId() { return id; }
            public void setId(Long id) { this.id = id; }
            public Integer getNumber() { return number; }
            public void setNumber(Integer number) { this.number = number; }
            public String getTitle() { return title; }
            public void setTitle(String title) { this.title = title; }
            public String getBody() { return body; }
            public void setBody(String body) { this.body = body; }
            public Head getHead() { return head; }
            public void setHead(Head head) { this.head = head; }
            public Base getBase() { return base; }
            public void setBase(Base base) { this.base = base; }
            public User getUser() { return user; }
            public void setUser(User user) { this.user = user; }
            public String getHtmlUrl() { return htmlUrl; }
            public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
            public String getState() { return state; }
            public void setState(String state) { this.state = state; }
        }

        public static class Head {
            private String ref;
            private String sha;

            public String getRef() { return ref; }
            public void setRef(String ref) { this.ref = ref; }
            public String getSha() { return sha; }
            public void setSha(String sha) { this.sha = sha; }
        }

        public static class Base {
            private String ref;
            private String sha;

            public String getRef() { return ref; }
            public void setRef(String ref) { this.ref = ref; }
            public String getSha() { return sha; }
            public void setSha(String sha) { this.sha = sha; }
        }

        public static class User {
            private String login;

            public String getLogin() { return login; }
            public void setLogin(String login) { this.login = login; }
        }
    }
}
