package com.pullwise.api.application.service.review;

import com.pullwise.api.application.service.integration.OpenRouterService;
import com.pullwise.api.domain.enums.IssueSource;
import com.pullwise.api.domain.enums.IssueType;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.model.Review;
import com.pullwise.api.application.service.integration.GitHubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Serviço de review usando LLM (Large Language Model).
 * Usa OpenRouter API ou Ollama para análise inteligente de código.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMReviewService {

    private final OpenRouterService openRouterService;

    @Value("${integrations.ollama.enabled:false}")
    private boolean ollamaEnabled;

    @Value("${integrations.openrouter.enabled:true}")
    private boolean openRouterEnabled;

    /**
     * Executa análise LLM nos arquivos do PR.
     */
    public List<Issue> analyze(PullRequest pr, List<GitHubService.FileDiff> diffs) {
        List<Issue> issues = new ArrayList<>();

        // Limitar a 5 arquivos para não sobrecarregar a API
        List<GitHubService.FileDiff> filesToAnalyze = diffs.stream()
                .limit(5)
                .toList();

        for (GitHubService.FileDiff diff : filesToAnalyze) {
            if (shouldAnalyzeFile(diff)) {
                try {
                    List<Issue> fileIssues = analyzeFileWithLLM(pr, diff);
                    issues.addAll(fileIssues);
                } catch (Exception e) {
                    log.warn("Failed to analyze file {} with LLM: {}", diff.filename(), e.getMessage());
                }
            }
        }

        log.info("LLM analysis found {} issues for PR {}", issues.size(), pr.getPrNumber());
        return issues;
    }

    /**
     * Determina se um arquivo deve ser analisado pelo LLM.
     */
    private boolean shouldAnalyzeFile(GitHubService.FileDiff diff) {
        String filename = diff.filename().toLowerCase();

        // Ignorar arquivos de configuração, lock files, etc.
        if (filename.endsWith(".lock") ||
                filename.endsWith(".md") ||
                filename.endsWith(".txt") ||
                filename.contains("package-lock.json") ||
                filename.contains("yarn.lock") ||
                filename.contains("pom.xml") ||
                filename.contains(".gradle")) {
            return false;
        }

        // Apenas arquivos de código com mudanças significativas
        return diff.additions() > 0 && diff.additions() < 500;
    }

    /**
     * Analisa um arquivo usando LLM.
     */
    private List<Issue> analyzeFileWithLLM(PullRequest pr, GitHubService.FileDiff diff) {
        String prompt = buildReviewPrompt(diff);

        String response;
        if (ollamaEnabled) {
            response = openRouterService.analyzeWithOllama(prompt);
        } else if (openRouterEnabled) {
            response = openRouterService.analyzeWithOpenRouter(prompt);
        } else {
            return List.of();
        }

        return parseLLMResponse(diff, response);
    }

    /**
     * Constrói o prompt para o LLM.
     */
    private String buildReviewPrompt(GitHubService.FileDiff diff) {
        return String.format("""
                You are a code reviewer. Analyze the following code diff and provide feedback.

                File: %s
                Changes:
                ```diff
                %s
                ```

                Provide feedback in the following JSON format:
                [
                  {
                    "severity": "CRITICAL|HIGH|MEDIUM|LOW|INFO",
                    "type": "BUG|VULNERABILITY|CODE_SMELL|PERFORMANCE|SECURITY|SUGGESTION",
                    "title": "Brief description",
                    "description": "Detailed explanation",
                    "suggestion": "How to fix"
                  }
                ]

                Focus on:
                1. Security vulnerabilities
                2. Bug risks
                3. Performance issues
                4. Code smells
                5. Best practices violations

                Return only the JSON array, no other text.
                """, diff.filename(), truncatePatch(diff.patch(), 3000));
    }

    /**
     * Trunca o patch se for muito grande.
     */
    private String truncatePatch(String patch, int maxLength) {
        if (patch.length() <= maxLength) {
            return patch;
        }
        return patch.substring(0, maxLength) + "\n... (truncated)";
    }

    /**
     * Faz parse da resposta do LLM.
     */
    private List<Issue> parseLLMResponse(GitHubService.FileDiff diff, String response) {
        List<Issue> issues = new ArrayList<>();

        try {
            // Extrair JSON da resposta
            String jsonPart = extractJson(response);

            if (jsonPart == null || jsonPart.isBlank()) {
                return issues;
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            LLMIssue[] llmIssues = mapper.readValue(jsonPart, LLMIssue[].class);

            for (LLMIssue llmIssue : llmIssues) {
                issues.add(Issue.builder()
                        .severity(parseSeverity(llmIssue.severity))
                        .type(parseType(llmIssue.type))
                        .source(IssueSource.LLM)
                        .title(llmIssue.title)
                        .description(llmIssue.description)
                        .filePath(diff.filename())
                        .suggestion(llmIssue.suggestion)
                        .build());
            }

        } catch (Exception e) {
            log.warn("Failed to parse LLM response: {}", e.getMessage());
        }

        return issues;
    }

    /**
     * Extrai o JSON da resposta do LLM.
     */
    private String extractJson(String response) {
        // Tentar encontrar um array JSON
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');

        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        return null;
    }

    private Severity parseSeverity(String severity) {
        try {
            return Severity.valueOf(severity.toUpperCase());
        } catch (Exception e) {
            return Severity.MEDIUM;
        }
    }

    private IssueType parseType(String type) {
        try {
            return IssueType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            return IssueType.CODE_SMELL;
        }
    }

    /**
     * Estima o número de tokens consumidos por um review.
     */
    public long getEstimatedTokens(Review review) {
        // Estimativa simples: ~1000 tokens por arquivo analisado
        int files = review.getFilesAnalyzed() != null ? review.getFilesAnalyzed() : 1;
        return files * 1000L;
    }

    private record LLMIssue(
            String severity,
            String type,
            String title,
            String description,
            String suggestion
    ) {}
}
