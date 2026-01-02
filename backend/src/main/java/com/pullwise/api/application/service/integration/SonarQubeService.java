package com.pullwise.api.application.service.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.dto.SonarQubeResponse;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.model.Review;
import com.pullwise.api.domain.enums.IssueSource;
import com.pullwise.api.domain.enums.IssueType;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Serviço para integração real com API do SonarQube.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SonarQubeService {

    @Value("${integrations.sonarqube.url:}")
    private String sonarqubeUrl;

    @Value("${integrations.sonarqube.token:}")
    private String sonarqubeToken;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final IssueRepository issueRepository;

    /**
     * Verifica se o SonarQube está configurado e disponível.
     */
    public boolean isConfigured() {
        return sonarqubeUrl != null && !sonarqubeUrl.isBlank()
                && sonarqubeToken != null && !sonarqubeToken.isBlank();
    }

    /**
     * Busca issues do SonarQube para um projeto específico.
     *
     * @param projectKey Chave do projeto no SonarQube (ex: "organization:repo")
     * @return Lista de issues encontradas
     */
    public List<SonarQubeResponse.Issue> getProjectIssues(String projectKey) {
        if (!isConfigured()) {
            log.warn("SonarQube is not configured");
            return List.of();
        }

        String url = sonarqubeUrl + "/api/issues/search"
                + "?componentKeys=" + projectKey
                + "&ps=500"; // pageSize=500

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                SonarQubeResponse sqResponse = objectMapper.readValue(
                        response.getBody(),
                        SonarQubeResponse.class
                );
                return sqResponse.getIssues() != null ? sqResponse.getIssues() : List.of();
            }

        } catch (JsonProcessingException e) {
            log.error("Error parsing SonarQube response", e);
        } catch (Exception e) {
            log.error("Error fetching issues from SonarQube for project: {}", projectKey, e);
        }

        return List.of();
    }

    /**
     * Busca issues do SonarQube para um branch específico.
     *
     * @param projectKey Chave do projeto no SonarQube
     * @param branch Nome do branch
     * @return Lista de issues encontradas
     */
    public List<SonarQubeResponse.Issue> getBranchIssues(String projectKey, String branch) {
        if (!isConfigured()) {
            return List.of();
        }

        String url = sonarqubeUrl + "/api/issues/search"
                + "?componentKeys=" + projectKey
                + "&branch=" + branch
                + "&ps=500";

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                SonarQubeResponse sqResponse = objectMapper.readValue(
                        response.getBody(),
                        SonarQubeResponse.class
                );
                return sqResponse.getIssues() != null ? sqResponse.getIssues() : List.of();
            }

        } catch (Exception e) {
            log.error("Error fetching branch issues from SonarQube", e);
        }

        return List.of();
    }

    /**
     * Busca issues do SonarQube para uma Pull Request.
     *
     * @param projectKey Chave do projeto no SonarQube
     * @param prKey Chave da Pull Request no SonarQube
     * @return Lista de issues encontradas
     */
    public List<SonarQubeResponse.Issue> getPullRequestIssues(String projectKey, String prKey) {
        if (!isConfigured()) {
            return List.of();
        }

        String url = sonarqubeUrl + "/api/issues/search"
                + "?componentKeys=" + projectKey
                + "&pullRequest=" + prKey
                + "&ps=500";

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                SonarQubeResponse sqResponse = objectMapper.readValue(
                        response.getBody(),
                        SonarQubeResponse.class
                );
                return sqResponse.getIssues() != null ? sqResponse.getIssues() : List.of();
            }

        } catch (Exception e) {
            log.error("Error fetching PR issues from SonarQube", e);
        }

        return List.of();
    }

    /**
     * Converte issues do SonarQube para Issues do Pullwise e salva no banco.
     *
     * @param review Review ao qual as_issues pertencem
     * @param sqIssues Lista de_issues do SonarQube
     * @return Lista de Issues salvas
     */
    public List<Issue> convertAndSaveIssues(Review review, List<SonarQubeResponse.Issue> sqIssues) {
        List<Issue> issues = new ArrayList<>();

        for (SonarQubeResponse.Issue sqIssue : sqIssues) {
            try {
                Issue issue = Issue.builder()
                        .review(review)
                        .source(IssueSource.SONARQUBE)
                        .type(mapIssueType(sqIssue.getType()))
                        .severity(mapSeverity(sqIssue.getSeverity()))
                        .ruleId(sqIssue.getRule())
                        .title(sqIssue.getMessage())
                        .description(sqIssue.getMessage())
                        .filePath(extractFilePath(sqIssue.getComponent()))
                        .lineStart(sqIssue.getTextRange() != null ? sqIssue.getTextRange().getStartLine() : null)
                        .lineEnd(sqIssue.getTextRange() != null ? sqIssue.getTextRange().getEndLine() : null)
                        .isFalsePositive(false)
                        .build();

                issues.add(issueRepository.save(issue));
            } catch (Exception e) {
                log.error("Error converting SonarQube issue: {}", sqIssue.getKey(), e);
            }
        }

        return issues;
    }

    /**
     * Extrai o caminho do arquivo do componente do SonarQube.
     * Formato: "projectKey:module:path/to/File.java"
     */
    private String extractFilePath(String component) {
        if (component == null) {
            return null;
        }

        // Remove a chave do projeto e mantém apenas o path do arquivo
        String[] parts = component.split(":");
        if (parts.length >= 3) {
            // Retorna tudo após o segundo ":"
            return component.substring(component.indexOf(":", component.indexOf(":") + 1) + 1);
        } else if (parts.length == 2) {
            return parts[1];
        }

        return component;
    }

    /**
     * Mapeia o tipo de issue do SonarQube para o tipo do Pullwise.
     */
    private IssueType mapIssueType(String sonarType) {
        if (sonarType == null) {
            return IssueType.CODE_SMELL;
        }

        return switch (sonarType.toUpperCase()) {
            case "BUG" -> IssueType.BUG;
            case "VULNERABILITY" -> IssueType.VULNERABILITY;
            case "CODE_SMELL" -> IssueType.CODE_SMELL;
            case "SECURITY_HOTSPOT" -> IssueType.SECURITY;
            default -> IssueType.CODE_SMELL;
        };
    }

    /**
     * Mapeia a severidade do SonarQube para a severidade do Pullwise.
     */
    private Severity mapSeverity(String sonarSeverity) {
        if (sonarSeverity == null) {
            return Severity.MEDIUM;
        }

        return switch (sonarSeverity.toUpperCase()) {
            case "BLOCKER", "CRITICAL" -> Severity.CRITICAL;
            case "MAJOR" -> Severity.HIGH;
            case "MINOR" -> Severity.MEDIUM;
            case "INFO" -> Severity.LOW;
            default -> Severity.MEDIUM;
        };
    }

    /**
     * Cria HttpHeaders com autenticação do SonarQube.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + sonarqubeToken);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    /**
     * Trigger uma nova análise no SonarQube para um branch.
     *
     * @param projectKey Chave do projeto no SonarQube
     * @param branch Nome do branch
     * @return true se a análise foi triggerada com sucesso
     */
    public boolean triggerAnalysis(String projectKey, String branch) {
        if (!isConfigured()) {
            log.warn("SonarQube is not configured");
            return false;
        }

        String url = sonarqubeUrl + "/api/project_analysis/trigger"
                + "?project=" + projectKey
                + "&branch=" + branch;

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully triggered SonarQube analysis for {}/{}", projectKey, branch);
                return true;
            }

        } catch (Exception e) {
            log.error("Error triggering SonarQube analysis", e);
        }

        return false;
    }

    /**
     * Obtém o status da Quality Gate para um projeto.
     *
     * @param projectKey Chave do projeto no SonarQube
     * @param branch Nome do branch (opcional)
     * @return Status da Quality Gate ("OK", "WARN", "ERROR")
     */
    public Optional<String> getQualityGateStatus(String projectKey, String branch) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        String url = sonarqubeUrl + "/api/qualitygates/project_status"
                + "?projectKey=" + projectKey;

        if (branch != null && !branch.isBlank()) {
            url += "&branch=" + branch;
        }

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Parse response manualmente para extrair o status
                String body = response.getBody();
                int statusIdx = body.indexOf("\"status\"");
                if (statusIdx >= 0) {
                    int start = body.indexOf("\"", statusIdx + 9) + 1;
                    int end = body.indexOf("\"", start);
                    if (end > start) {
                        return Optional.of(body.substring(start, end));
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error fetching Quality Gate status", e);
        }

        return Optional.empty();
    }

    /**
     * Verifica se um projeto existe no SonarQube.
     *
     * @param projectKey Chave do projeto no SonarQube
     * @return true se o projeto existe
     */
    public boolean projectExists(String projectKey) {
        if (!isConfigured()) {
            return false;
        }

        String url = sonarqubeUrl + "/api/projects/search"
                + "?projects=" + projectKey;

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().contains("\"key\":\"" + projectKey + "\"");
            }

        } catch (Exception e) {
            log.error("Error checking if project exists in SonarQube", e);
        }

        return false;
    }
}
