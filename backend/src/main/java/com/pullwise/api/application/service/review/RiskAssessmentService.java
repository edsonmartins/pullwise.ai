package com.pullwise.api.application.service.review;

import com.pullwise.api.application.service.config.ConfigurationResolver;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.domain.constants.ConfigKeys;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.PullRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Serviço de avaliação de risco de PRs.
 *
 * <p>Calcula um score de risco (0-100) baseado em heurísticas:
 * <ul>
 *   <li>Tipo de arquivos alterados (docs/testes vs código core)</li>
 *   <li>Volume de mudanças (poucas linhas = menos risco)</li>
 *   <li>Presença de issues de segurança</li>
 *   <li>Blast radius (quantos módulos afetados)</li>
 * </ul>
 *
 * <p>Se o score estiver abaixo do threshold configurável E não houver issues
 * CRITICAL/HIGH, o PR pode ser auto-aprovado.
 *
 * <p>Feature opt-in — desligada por default. Configurável por projeto via:
 * {@code review.auto_approve_enabled} e {@code review.auto_approve_threshold}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskAssessmentService {

    private static final int DEFAULT_THRESHOLD = 20;

    private static final Set<String> LOW_RISK_EXTENSIONS = Set.of(
            ".md", ".txt", ".rst", ".adoc",
            ".yml", ".yaml", ".toml",
            ".properties", ".env.example",
            ".gitignore", ".editorconfig",
            ".dockerignore"
    );

    private static final Set<String> TEST_PATTERNS = Set.of(
            "test/", "tests/", "spec/", "specs/",
            "__tests__/", "__test__/",
            "Test.java", "Tests.java", "Spec.java",
            ".test.ts", ".test.js", ".spec.ts", ".spec.js",
            "_test.go", "_test.py"
    );

    private final ConfigurationResolver configurationResolver;

    /**
     * Avalia o risco de um PR e retorna o resultado.
     */
    public RiskAssessment assess(PullRequest pr, List<GitHubService.FileDiff> diffs, List<Issue> issues) {
        int score = calculateRiskScore(diffs, issues);

        boolean hasCriticalOrHigh = issues.stream()
                .anyMatch(i -> i.getSeverity() == Severity.CRITICAL || i.getSeverity() == Severity.HIGH);

        boolean allDocsOrTests = diffs.stream()
                .allMatch(d -> isDocFile(d.filename()) || isTestFile(d.filename()));

        return new RiskAssessment(
                score,
                hasCriticalOrHigh,
                allDocsOrTests,
                diffs.size(),
                diffs.stream().mapToInt(d -> d.additions() + d.deletions()).sum(),
                issues.size()
        );
    }

    /**
     * Verifica se o PR deve ser auto-aprovado baseado no risco e configuração.
     */
    public boolean shouldAutoApprove(PullRequest pr, RiskAssessment assessment) {
        Long projectId = pr.getProject() != null ? pr.getProject().getId() : null;
        if (projectId == null) return false;

        // Verificar se auto-approve está habilitado
        String enabled = configurationResolver.getConfig(projectId, ConfigKeys.REVIEW_AUTO_APPROVE_ENABLED);
        if (!"true".equalsIgnoreCase(enabled)) {
            return false;
        }

        // Obter threshold
        int threshold = DEFAULT_THRESHOLD;
        String thresholdConfig = configurationResolver.getConfig(projectId, ConfigKeys.REVIEW_AUTO_APPROVE_THRESHOLD);
        if (thresholdConfig != null) {
            try {
                threshold = Integer.parseInt(thresholdConfig);
            } catch (NumberFormatException e) {
                log.warn("Invalid auto-approve threshold config: {}", thresholdConfig);
            }
        }

        // Não auto-aprovar se houver issues CRITICAL ou HIGH
        if (assessment.hasCriticalOrHigh()) {
            log.debug("PR #{} has critical/high issues, skipping auto-approve", pr.getPrNumber());
            return false;
        }

        // Verificar score
        boolean approved = assessment.score() <= threshold;
        log.info("PR #{} risk assessment: score={}, threshold={}, autoApprove={}",
                pr.getPrNumber(), assessment.score(), threshold, approved);

        return approved;
    }

    /**
     * Calcula o score de risco (0 = seguro, 100 = alto risco).
     */
    private int calculateRiskScore(List<GitHubService.FileDiff> diffs, List<Issue> issues) {
        int score = 0;

        // 1. Volume de mudanças (0-30 pontos)
        int totalLines = diffs.stream().mapToInt(d -> d.additions() + d.deletions()).sum();
        if (totalLines > 500) score += 30;
        else if (totalLines > 200) score += 20;
        else if (totalLines > 50) score += 10;
        else if (totalLines > 10) score += 5;

        // 2. Número de arquivos (0-20 pontos)
        int fileCount = diffs.size();
        if (fileCount > 20) score += 20;
        else if (fileCount > 10) score += 15;
        else if (fileCount > 5) score += 10;
        else if (fileCount > 2) score += 5;

        // 3. Tipo de arquivos (0-25 pontos)
        boolean hasCodeFiles = diffs.stream()
                .anyMatch(d -> !isDocFile(d.filename()) && !isTestFile(d.filename()));
        boolean hasSecurityFiles = diffs.stream()
                .anyMatch(d -> isSecuritySensitiveFile(d.filename()));

        if (hasSecurityFiles) score += 25;
        else if (hasCodeFiles) score += 15;
        // Docs/tests only: 0 pontos

        // 4. Issues encontradas (0-25 pontos)
        long criticalCount = issues.stream().filter(i -> i.getSeverity() == Severity.CRITICAL).count();
        long highCount = issues.stream().filter(i -> i.getSeverity() == Severity.HIGH).count();
        long mediumCount = issues.stream().filter(i -> i.getSeverity() == Severity.MEDIUM).count();

        score += (int) (criticalCount * 25);
        score += (int) (highCount * 15);
        score += (int) (mediumCount * 5);

        return Math.min(100, score);
    }

    private boolean isDocFile(String filename) {
        String lower = filename.toLowerCase();
        return LOW_RISK_EXTENSIONS.stream().anyMatch(lower::endsWith)
                || lower.contains("readme")
                || lower.contains("changelog")
                || lower.contains("license")
                || lower.startsWith("docs/")
                || lower.startsWith("doc/");
    }

    private boolean isTestFile(String filename) {
        String lower = filename.toLowerCase();
        return TEST_PATTERNS.stream().anyMatch(lower::contains);
    }

    private boolean isSecuritySensitiveFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.contains("security")
                || lower.contains("auth")
                || lower.contains("crypto")
                || lower.contains("password")
                || lower.contains("token")
                || lower.contains("secret")
                || lower.contains("permission")
                || lower.contains("role")
                || lower.endsWith("securityconfig.java")
                || lower.endsWith("webSecurityConfig.java");
    }

    /**
     * Resultado da avaliação de risco.
     */
    public record RiskAssessment(
            int score,
            boolean hasCriticalOrHigh,
            boolean allDocsOrTests,
            int filesChanged,
            int linesChanged,
            int issuesFound
    ) {
        public String getSummary() {
            String level = score <= 20 ? "Low" : (score <= 50 ? "Medium" : "High");
            return String.format("Risk: %s (%d/100) — %d files, %d lines, %d issues",
                    level, score, filesChanged, linesChanged, issuesFound);
        }
    }
}
