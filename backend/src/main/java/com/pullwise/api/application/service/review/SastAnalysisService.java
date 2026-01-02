package com.pullwise.api.application.service.review;

import com.pullwise.api.application.dto.SonarQubeResponse;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.integration.SonarQubeService;
import com.pullwise.api.domain.enums.IssueSource;
import com.pullwise.api.domain.enums.IssueType;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.model.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Serviço de análise SAST (Static Application Security Testing).
 * Integra com SonarQube, Checkstyle, PMD, SpotBugs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SastAnalysisService {

    private final GitHubService gitHubService;
    private final SonarQubeService sonarQubeService;

    /**
     * Executa análise SAST nos arquivos do PR.
     * Tenta usar SonarQube se configurado, senão usa análise local.
     */
    public List<Issue> analyze(Review review, PullRequest pr, List<GitHubService.FileDiff> diffs) {
        List<Issue> issues = new ArrayList<>();

        // Primeiro tenta buscar do SonarQube
        if (sonarQubeService.isConfigured()) {
            List<Issue> sonarIssues = analyzeWithSonarQube(review, pr);
            if (!sonarIssues.isEmpty()) {
                issues.addAll(sonarIssues);
                log.info("SonarQube found {} issues for PR {}", sonarIssues.size(), pr.getPrNumber());
            }
        }

        // Se SonarQube não retornou issues ou não está configurado, usa análise local
        // como fallback ou complemento
        for (GitHubService.FileDiff diff : diffs) {
            issues.addAll(analyzeFile(pr, diff));
        }

        log.info("SAST analysis found {} total issues for PR {}", issues.size(), pr.getPrNumber());
        return issues;
    }

    /**
     * Busca issues do SonarQube para o PR/branch.
     */
    private List<Issue> analyzeWithSonarQube(Review review, PullRequest pr) {
        List<Issue> issues = new ArrayList<>();

        try {
            Project project = pr.getProject();
            String sonarProjectKey = buildSonarProjectKey(project);

            // Tenta buscar por PR primeiro (para GitHub/BitBucket PRs)
            String prKey = buildSonarPRKey(pr);
            List<SonarQubeResponse.Issue> sqIssues = sonarQubeService.getPullRequestIssues(sonarProjectKey, prKey);

            // Se não encontrou por PR, tenta por branch
            if (sqIssues.isEmpty() && pr.getSourceBranch() != null) {
                sqIssues = sonarQubeService.getBranchIssues(sonarProjectKey, pr.getSourceBranch());
            }

            // Se ainda não encontrou, tenta issues gerais do projeto
            if (sqIssues.isEmpty()) {
                sqIssues = sonarQubeService.getProjectIssues(sonarProjectKey);
            }

            // Converte e salva as issues
            issues.addAll(sonarQubeService.convertAndSaveIssues(review, sqIssues));

            log.info("Retrieved {} issues from SonarQube for project {}", issues.size(), sonarProjectKey);

        } catch (Exception e) {
            log.error("Error analyzing with SonarQube for PR {}", pr.getPrNumber(), e);
        }

        return issues;
    }

    /**
     * Constrói a chave do projeto no SonarQube.
     * Formato: owner_repo (ex: "pullwise_backend")
     */
    private String buildSonarProjectKey(Project project) {
        // Tenta extrair do repositoryUrl
        // URL: https://github.com/owner/repo.git -> owner_repo
        if (project.getRepositoryUrl() != null) {
            String url = project.getRepositoryUrl();
            String[] parts = url.split("/");
            if (parts.length >= 2) {
                String owner = parts[parts.length - 2].replaceAll("[^a-zA-Z0-9_-]", "");
                String repo = parts[parts.length - 1].replace(".git", "").replaceAll("[^a-zA-Z0-9_-]", "");
                return owner + "_" + repo;
            }
        }

        // Fallback: usa o nome do projeto
        return project.getName().replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    /**
     * Constrói a chave da PR no SonarQube.
     * Formato: numero (ex: "123")
     */
    private String buildSonarPRKey(PullRequest pr) {
        return String.valueOf(pr.getPrNumber());
    }

    /**
     * Analisa um arquivo específico usando regras locais.
     * Usado como fallback quando SonarQube não está disponível.
     */
    private List<Issue> analyzeFile(PullRequest pr, GitHubService.FileDiff diff) {
        List<Issue> issues = new ArrayList<>();
        String filename = diff.filename().toLowerCase();

        // Análise de código Java
        if (filename.endsWith(".java")) {
            issues.addAll(analyzeJavaFile(diff));
        }

        // Análise de código JavaScript/TypeScript
        if (filename.endsWith(".js") || filename.endsWith(".ts") || filename.endsWith(".tsx")) {
            issues.addAll(analyzeJavaScriptFile(diff));
        }

        // Análise de código Python
        if (filename.endsWith(".py")) {
            issues.addAll(analyzePythonFile(diff));
        }

        return issues;
    }

    /**
     * Análise simples de arquivos Java.
     */
    private List<Issue> analyzeJavaFile(GitHubService.FileDiff diff) {
        List<Issue> issues = new ArrayList<>();
        String content = diff.patch();

        // Detectar TODOs sem ticket
        if (content.contains("TODO") || content.contains("FIXME")) {
            issues.add(Issue.builder()
                    .severity(Severity.LOW)
                    .type(IssueType.CODE_SMELL)
                    .source(IssueSource.SONARQUBE)
                    .title("TODO/FIXME found in code")
                    .description("Consider replacing TODO/FIXME with a proper ticket number.")
                    .filePath(diff.filename())
                    .ruleId("java:S1135")
                    .suggestion("Replace with a ticket reference: // TODO: TICKET-123 description")
                    .build());
        }

        // Detectar printStackTrace
        if (content.contains("printStackTrace()")) {
            issues.add(Issue.builder()
                    .severity(Severity.MEDIUM)
                    .type(IssueType.BUG)
                    .source(IssueSource.SONARQUBE)
                    .title("printStackTrace() should not be used")
                    .description("Using printStackTrace() is not recommended for production code.")
                    .filePath(diff.filename())
                    .ruleId("java:S1148")
                    .suggestion("Use a proper logging framework instead.")
                    .build());
        }

        // Detectar System.out.println
        if (content.contains("System.out.println")) {
            issues.add(Issue.builder()
                    .severity(Severity.LOW)
                    .type(IssueType.CODE_SMELL)
                    .source(IssueSource.SONARQUBE)
                    .title("System.out.println should not be used")
                    .description("Use a logging framework instead of System.out.println.")
                    .filePath(diff.filename())
                    .ruleId("java:S106")
                    .suggestion("Replace with logger.debug() or logger.info().")
                    .build());
        }

        // Detectar empty catch blocks
        if (content.matches(".*catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}.*")) {
            issues.add(Issue.builder()
                    .severity(Severity.HIGH)
                    .type(IssueType.BUG)
                    .source(IssueSource.PMD)
                    .title("Empty catch block")
                    .description("Empty catch blocks can hide errors and make debugging difficult.")
                    .filePath(diff.filename())
                    .ruleId("pmd:EmptyCatches")
                    .suggestion("Add proper error handling or at least log the exception.")
                    .build());
        }

        return issues;
    }

    /**
     * Análise simples de arquivos JavaScript/TypeScript.
     */
    private List<Issue> analyzeJavaScriptFile(GitHubService.FileDiff diff) {
        List<Issue> issues = new ArrayList<>();
        String content = diff.patch();

        // Detectar console.log
        if (content.contains("console.log")) {
            issues.add(Issue.builder()
                    .severity(Severity.LOW)
                    .type(IssueType.CODE_SMELL)
                    .source(IssueSource.LLM)
                    .title("console.log should not be used in production")
                    .description("Remove console.log statements or replace with proper logging.")
                    .filePath(diff.filename())
                    .suggestion("Use a logging library like winston or pino.")
                    .build());
        }

        // Detectar var (deve usar const/let)
        if (content.matches(".*\\bvar\\s+[a-zA-Z_$].*")) {
            issues.add(Issue.builder()
                    .severity(Severity.MEDIUM)
                    .type(IssueType.CODE_SMELL)
                    .source(IssueSource.LLM)
                    .title("Use of 'var' is discouraged")
                    .description("Use 'const' or 'let' instead of 'var' for better scoping.")
                    .filePath(diff.filename())
                    .suggestion("Replace 'var' with 'const' or 'let'.")
                    .build());
        }

        // Detectar any type
        if (content.matches(".*:\\s*any[^\\w].*")) {
            issues.add(Issue.builder()
                    .severity(Severity.MEDIUM)
                    .type(IssueType.CODE_SMELL)
                    .source(IssueSource.LLM)
                    .title("Avoid using 'any' type")
                    .description("Using 'any' defeats the purpose of TypeScript type checking.")
                    .filePath(diff.filename())
                    .suggestion("Use specific types or 'unknown' with type guards.")
                    .build());
        }

        return issues;
    }

    /**
     * Análise simples de arquivos Python.
     */
    private List<Issue> analyzePythonFile(GitHubService.FileDiff diff) {
        List<Issue> issues = new ArrayList<>();
        String content = diff.patch();

        // Detectar print statements
        if (content.matches(".*print\\s*\\(.*")) {
            issues.add(Issue.builder()
                    .severity(Severity.LOW)
                    .type(IssueType.CODE_SMELL)
                    .source(IssueSource.LLM)
                    .title("print() statement found")
                    .description("Use proper logging instead of print() statements.")
                    .filePath(diff.filename())
                    .suggestion("Replace with logger.info() or logger.debug().")
                    .build());
        }

        // Detectar bare except
        if (content.matches(".*except\\s*:\\s*$.*")) {
            issues.add(Issue.builder()
                    .severity(Severity.HIGH)
                    .type(IssueType.BUG)
                    .source(IssueSource.CUSTOM)
                    .title("Bare except clause")
                    .description("Bare except clauses catch all exceptions including SystemExit.")
                    .filePath(diff.filename())
                    .ruleId("E722")
                    .suggestion("Specify the exception type: except Exception as e:")
                    .build());
        }

        return issues;
    }

    /**
     * Trigger uma nova análise no SonarQube para um branch.
     *
     * @param project Projeto a ser analisado
     * @param branch Branch para analisar
     * @return true se a análise foi iniciada
     */
    public boolean triggerAnalysis(Project project, String branch) {
        if (!sonarQubeService.isConfigured()) {
            log.warn("SonarQube is not configured, cannot trigger analysis");
            return false;
        }

        String sonarProjectKey = buildSonarProjectKey(project);

        // Primeiro verifica se o projeto existe
        if (!sonarQubeService.projectExists(sonarProjectKey)) {
            log.warn("SonarQube project {} does not exist", sonarProjectKey);
            return false;
        }

        return sonarQubeService.triggerAnalysis(sonarProjectKey, branch);
    }

    /**
     * Obtém o status da Quality Gate do SonarQube.
     *
     * @param project Projeto
     * @param branch Branch
     * @return Status da Quality Gate (OK, WARN, ERROR) ou vazio se não disponível
     */
    public java.util.Optional<String> getQualityGateStatus(Project project, String branch) {
        if (!sonarQubeService.isConfigured()) {
            return java.util.Optional.empty();
        }

        String sonarProjectKey = buildSonarProjectKey(project);
        return sonarQubeService.getQualityGateStatus(sonarProjectKey, branch);
    }
}
