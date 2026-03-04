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

    @Value("${pullwise.review.max-files:50}")
    private int maxFiles;

    @Value("${pullwise.review.max-lines-per-file:1000}")
    private int maxLinesPerFile;

    @Value("${pullwise.review.chunk-size:10}")
    private int chunkSize;

    /**
     * Executa análise LLM nos arquivos do PR com chunking para PRs grandes.
     * Arquivos são processados em batches configuráveis (default: 10 por chunk).
     * Arquivos grandes têm seus diffs comprimidos para manter apenas hunks relevantes.
     */
    public List<Issue> analyze(PullRequest pr, List<GitHubService.FileDiff> diffs) {
        List<Issue> issues = new ArrayList<>();

        // Filtrar e preparar arquivos para análise
        List<GitHubService.FileDiff> filesToAnalyze = diffs.stream()
                .filter(this::shouldAnalyzeFile)
                .limit(maxFiles)
                .toList();

        log.info("LLM analysis: {} of {} files selected for review (max={})",
                filesToAnalyze.size(), diffs.size(), maxFiles);

        // Comprimir diffs de arquivos grandes
        List<GitHubService.FileDiff> compressed = filesToAnalyze.stream()
                .map(this::compressDiff)
                .toList();

        // Processar em chunks
        for (int i = 0; i < compressed.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, compressed.size());
            List<GitHubService.FileDiff> chunk = compressed.subList(i, end);

            log.debug("Processing chunk {}-{} of {} files", i + 1, end, compressed.size());

            for (GitHubService.FileDiff diff : chunk) {
                try {
                    List<Issue> fileIssues = analyzeFileWithLLM(pr, diff);
                    issues.addAll(fileIssues);
                } catch (Exception e) {
                    log.warn("Failed to analyze file {} with LLM: {}", diff.filename(), e.getMessage());
                }
            }
        }

        log.info("LLM analysis found {} issues for PR {} ({} files analyzed)",
                issues.size(), pr.getPrNumber(), compressed.size());
        return issues;
    }

    /**
     * Determina se um arquivo deve ser analisado pelo LLM.
     * Não rejeita mais arquivos grandes — eles serão comprimidos.
     */
    private boolean shouldAnalyzeFile(GitHubService.FileDiff diff) {
        String filename = diff.filename().toLowerCase();

        // Ignorar arquivos de configuração, lock files, binários, etc.
        if (filename.endsWith(".lock") ||
                filename.endsWith(".md") ||
                filename.endsWith(".txt") ||
                filename.endsWith(".csv") ||
                filename.endsWith(".svg") ||
                filename.endsWith(".png") ||
                filename.endsWith(".jpg") ||
                filename.contains("package-lock.json") ||
                filename.contains("yarn.lock") ||
                filename.contains(".min.js") ||
                filename.contains(".min.css")) {
            return false;
        }

        // Apenas arquivos com mudanças reais
        return diff.additions() > 0 || diff.deletions() > 0;
    }

    /**
     * Comprime o diff de arquivos grandes, mantendo apenas hunks relevantes.
     * Para arquivos dentro do limite, retorna o diff inalterado.
     */
    private GitHubService.FileDiff compressDiff(GitHubService.FileDiff diff) {
        if (diff.patch() == null) return diff;

        int totalLines = diff.additions() + diff.deletions();
        if (totalLines <= maxLinesPerFile) {
            return diff;
        }

        // Extrair apenas hunks (seções com mudanças + contexto)
        String[] lines = diff.patch().split("\n");
        StringBuilder compressed = new StringBuilder();
        int linesKept = 0;
        boolean inHunk = false;
        int contextLines = 0;

        for (String line : lines) {
            if (line.startsWith("@@")) {
                // Hunk header — sempre manter
                compressed.append(line).append("\n");
                inHunk = true;
                contextLines = 0;
                linesKept++;
            } else if (line.startsWith("+") || line.startsWith("-")) {
                // Changed line — sempre manter
                compressed.append(line).append("\n");
                contextLines = 0;
                linesKept++;
            } else if (inHunk) {
                // Context line — manter até 3 linhas de contexto
                contextLines++;
                if (contextLines <= 3) {
                    compressed.append(line).append("\n");
                    linesKept++;
                } else if (contextLines == 4) {
                    compressed.append("... (context omitted)\n");
                    linesKept++;
                }
            }

            if (linesKept >= maxLinesPerFile) {
                compressed.append("\n... (diff truncated, ").append(totalLines - linesKept)
                        .append(" more lines)\n");
                break;
            }
        }

        log.debug("Compressed diff for {}: {} lines -> {} lines",
                diff.filename(), totalLines, linesKept);

        return new GitHubService.FileDiff(
                diff.filename(), diff.status(), diff.additions(), diff.deletions(),
                compressed.toString()
        );
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
