package com.pullwise.api.application.service.review;

import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.integration.OpenRouterService;
import com.pullwise.api.domain.enums.IssueSource;
import com.pullwise.api.domain.enums.IssueType;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.model.Issue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serviço de review leve para staged changes (pre-commit).
 * Opera sobre diffs raw sem entidade PullRequest — execução síncrona.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StagedReviewService {

    private final OpenRouterService openRouterService;

    @Value("${integrations.ollama.enabled:false}")
    private boolean ollamaEnabled;

    @Value("${integrations.openrouter.enabled:true}")
    private boolean openRouterEnabled;

    private static final Pattern DIFF_FILE_HEADER = Pattern.compile("^diff --git a/(.*?) b/(.*)$", Pattern.MULTILINE);
    private static final Pattern HUNK_HEADER = Pattern.compile("^@@.*@@.*$", Pattern.MULTILINE);

    /**
     * Analisa staged changes a partir de um diff raw.
     * Parseia o diff em FileDiffs individuais e executa review LLM em cada um.
     */
    public List<StagedIssue> analyzeStaged(String rawDiff, List<String> filePaths, String commitMessage) {
        List<FilePatch> patches = parseDiff(rawDiff);
        List<StagedIssue> allIssues = new ArrayList<>();

        log.info("Staged review: analyzing {} files", patches.size());

        for (FilePatch patch : patches) {
            if (!shouldAnalyze(patch.filename)) {
                continue;
            }

            try {
                String prompt = buildPrompt(patch, commitMessage);
                String response = callLLM(prompt);
                List<StagedIssue> issues = parseResponse(patch.filename, response);
                allIssues.addAll(issues);
            } catch (Exception e) {
                log.warn("Failed to analyze staged file {}: {}", patch.filename, e.getMessage());
            }
        }

        log.info("Staged review completed: {} issues found in {} files", allIssues.size(), patches.size());
        return allIssues;
    }

    /**
     * Parseia um diff unificado em patches por arquivo.
     */
    List<FilePatch> parseDiff(String rawDiff) {
        List<FilePatch> patches = new ArrayList<>();
        String[] sections = rawDiff.split("(?=diff --git)");

        for (String section : sections) {
            if (section.isBlank()) continue;

            Matcher matcher = DIFF_FILE_HEADER.matcher(section);
            if (matcher.find()) {
                String filename = matcher.group(2);
                // Extrair apenas o conteúdo do patch (hunks)
                int headerEnd = section.indexOf("\n@@");
                String patchContent = headerEnd >= 0 ? section.substring(headerEnd) : section;

                int additions = 0;
                int deletions = 0;
                for (String line : patchContent.split("\n")) {
                    if (line.startsWith("+") && !line.startsWith("+++")) additions++;
                    if (line.startsWith("-") && !line.startsWith("---")) deletions++;
                }

                patches.add(new FilePatch(filename, patchContent.trim(), additions, deletions));
            }
        }

        return patches;
    }

    private boolean shouldAnalyze(String filename) {
        String lower = filename.toLowerCase();
        return !(lower.endsWith(".lock") || lower.endsWith(".md") || lower.endsWith(".txt") ||
                lower.endsWith(".csv") || lower.endsWith(".svg") || lower.endsWith(".png") ||
                lower.endsWith(".jpg") || lower.contains("package-lock.json") ||
                lower.contains("yarn.lock") || lower.contains(".min.js") ||
                lower.contains(".min.css"));
    }

    private String buildPrompt(FilePatch patch, String commitMessage) {
        String commitContext = commitMessage != null && !commitMessage.isBlank()
                ? "Commit message: " + commitMessage + "\n\n"
                : "";

        return String.format("""
                You are a code reviewer performing a pre-commit review. Analyze the following staged changes \
                and provide quick, actionable feedback. Focus on critical issues that should be fixed BEFORE committing.

                %sFile: %s
                Changes:
                ```diff
                %s
                ```

                Provide feedback in the following JSON format:
                [
                  {
                    "severity": "CRITICAL|HIGH|MEDIUM|LOW",
                    "type": "BUG|VULNERABILITY|CODE_SMELL|PERFORMANCE|SECURITY|SUGGESTION",
                    "title": "Brief description",
                    "description": "Detailed explanation",
                    "lineNumber": null,
                    "suggestion": "How to fix"
                  }
                ]

                Focus on issues that would block a commit:
                1. Security vulnerabilities (credentials, injection, etc.)
                2. Clear bugs (null pointer, off-by-one, etc.)
                3. Critical code smells

                Return only the JSON array, no other text. If no issues, return [].
                """, commitContext, patch.filename, truncate(patch.content, 3000));
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "\n... (truncated)";
    }

    private String callLLM(String prompt) {
        if (ollamaEnabled) {
            return openRouterService.analyzeWithOllama(prompt);
        } else if (openRouterEnabled) {
            return openRouterService.analyzeWithOpenRouter(prompt);
        }
        throw new IllegalStateException("No LLM provider enabled");
    }

    List<StagedIssue> parseResponse(String filename, String response) {
        List<StagedIssue> issues = new ArrayList<>();

        try {
            String jsonPart = extractJson(response);
            if (jsonPart == null || jsonPart.isBlank()) return issues;

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            RawIssue[] rawIssues = mapper.readValue(jsonPart, RawIssue[].class);

            for (RawIssue raw : rawIssues) {
                issues.add(new StagedIssue(
                        filename,
                        parseSeverity(raw.severity),
                        parseType(raw.type),
                        raw.title,
                        raw.description,
                        raw.lineNumber,
                        raw.suggestion
                ));
            }
        } catch (Exception e) {
            log.warn("Failed to parse staged review response for {}: {}", filename, e.getMessage());
        }

        return issues;
    }

    private String extractJson(String response) {
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return null;
    }

    private Severity parseSeverity(String severity) {
        try { return Severity.valueOf(severity.toUpperCase()); }
        catch (Exception e) { return Severity.MEDIUM; }
    }

    private IssueType parseType(String type) {
        try { return IssueType.valueOf(type.toUpperCase()); }
        catch (Exception e) { return IssueType.CODE_SMELL; }
    }

    /**
     * Resultado de uma issue encontrada em staged changes.
     */
    public record StagedIssue(
            String filePath,
            Severity severity,
            IssueType type,
            String title,
            String description,
            Integer lineNumber,
            String suggestion
    ) {}

    record FilePatch(String filename, String content, int additions, int deletions) {}

    private record RawIssue(
            String severity,
            String type,
            String title,
            String description,
            Integer lineNumber,
            String suggestion
    ) {}
}
