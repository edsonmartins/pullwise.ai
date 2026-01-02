package com.pullwise.api.application.service.review.pipeline.synthesis;

import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.Review;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.application.service.review.pipeline.MultiPassReviewOrchestrator.ReviewResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sintetiza os resultados de todas as passadas do pipeline.
 *
 * <p>Gera:
 * - Resumo executivo
 * - Estatísticas agregadas
 * - Recomendações priorizadas
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResultSynthesizer {

    /**
     * Gera o resumo executivo do review.
     *
     * @param review  O review realizado
     * @param issues  Lista de issues encontrados
     * @param result  Resultado completo do pipeline
     * @return Resumo executivo em texto
     */
    public String generateSummary(Review review, List<Issue> issues, ReviewResult result) {
        StringBuilder summary = new StringBuilder();

        // Cabeçalho
        summary.append("## Code Review Summary\n\n");

        // Visão geral
        summary.append("**Overview**: ");
        if (issues.isEmpty()) {
            summary.append("No issues found. Great job! 🎉");
        } else {
            summary.append(String.format("Found **%d** issue%s requiring attention.",
                    issues.size(), issues.size() == 1 ? "" : "s"));
        }
        summary.append("\n\n");

        // Breakdown por severidade
        if (!issues.isEmpty()) {
            summary.append("### Issues by Severity\n\n");

            Map<Severity, Long> bySeverity = issues.stream()
                    .collect(Collectors.groupingBy(Issue::getSeverity, Collectors.counting()));

            for (Severity severity : Severity.values()) {
                Long count = bySeverity.get(severity);
                if (count != null && count > 0) {
                    String emoji = getEmojiForSeverity(severity);
                    summary.append(String.format("- **%s** %s: %d\n", emoji, severity, count));
                }
            }
            summary.append("\n");
        }

        // Top issues críticos
        List<Issue> criticalIssues = issues.stream()
                .filter(i -> i.getSeverity() == Severity.CRITICAL)
                .limit(5)
                .toList();

        if (!criticalIssues.isEmpty()) {
            summary.append("### 🚨 Critical Issues\n\n");
            for (Issue issue : criticalIssues) {
                summary.append(String.format("**%s** (%s:%d)\n- %s\n\n",
                        issue.getTitle(),
                        getShortPath(issue.getFilePath()),
                        issue.getLineStart(),
                        issue.getDescription()));
            }
        }

        // Resumo por passada
        summary.append("### Analysis Passes\n\n");
        if (result.getSastResult() != null) {
            summary.append(String.format("- **SAST**: %d issues\n",
                    result.getSastResult().getIssues().size()));
        }
        if (result.getLlmResult() != null) {
            summary.append(String.format("- **LLM Primary**: %d issues\n",
                    result.getLlmResult().getIssues().size()));
        }
        if (result.getSecurityResult() != null) {
            summary.append(String.format("- **Security**: %d issues\n",
                    result.getSecurityResult().getIssues().size()));
        }
        if (result.getImpactResult() != null) {
            summary.append(String.format("- **Impact Analysis**: %d issues\n",
                    result.getImpactResult().getIssues().size()));
        }

        // Recomendações
        if (!issues.isEmpty()) {
            summary.append("\n### Recommendations\n\n");
            summary.append(generateRecommendations(issues));
        }

        return summary.toString();
    }

    /**
     * Gera recomendações baseadas nos issues encontrados.
     */
    private String generateRecommendations(List<Issue> issues) {
        StringBuilder recs = new StringBuilder();

        long criticalCount = issues.stream().filter(i -> i.getSeverity() == Severity.CRITICAL).count();
        long highCount = issues.stream().filter(i -> i.getSeverity() == Severity.HIGH).count();

        if (criticalCount > 0) {
            recs.append(String.format("1. **Address critical issues first** - %d critical issue%s detected.\n",
                    criticalCount, criticalCount == 1 ? "" : "s"));
        }

        if (highCount > 2) {
            recs.append("2. **Consider splitting this PR** - Multiple high-priority issues detected.\n");
        }

        long securityCount = issues.stream()
                .filter(i -> i.getType() == com.pullwise.api.domain.enums.IssueType.SECURITY)
                .count();

        if (securityCount > 0) {
            recs.append(String.format("3. **Security review required** - %d security issue%s found.\n",
                    securityCount, securityCount == 1 ? "" : "s"));
        }

        return recs.toString();
    }

    /**
     * Retorna o path curto do arquivo para exibição.
     */
    private String getShortPath(String fullPath) {
        if (fullPath == null) return "unknown";

        // Remove prefixos comuns de caminho
        String[] parts = fullPath.split("/");
        if (parts.length > 3) {
            return String.join("/", java.util.Arrays.copyOfRange(parts, parts.length - 3, parts.length));
        }
        return fullPath;
    }

    /**
     * Retorna emoji para uma severidade.
     */
    private String getEmojiForSeverity(Severity severity) {
        return switch (severity) {
            case CRITICAL -> "🔴";
            case HIGH -> "🟠";
            case MEDIUM -> "🟡";
            case LOW -> "🟢";
            case INFO -> "ℹ️";
        };
    }
}
