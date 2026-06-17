package com.pullwise.api.application.service.review.pipeline.pass;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.service.config.ConfigurationResolver;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.domain.constants.ConfigKeys;
import com.pullwise.api.application.service.llm.router.MultiModelLLMRouter;
import com.pullwise.api.application.service.review.pipeline.rules.ReviewRuleResolver;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.model.Review;
import com.pullwise.api.domain.enums.*;
import com.pullwise.api.application.service.review.pipeline.MultiPassReviewOrchestrator.PassResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Passada 2: LLM Primary Analysis
 *
 * <p>Usa LLM para análise profunda de:
 * - Lógica de negócio
 * - Bugs complexos não detectados por SAST
 * - Code smells sutis
 * - Problemas de arquitetura
 * - Oportunidades de refatoração
 *
 * <p>Esta passada enriquece os resultados do SAST com contexto e análises
 * que ferramentas estáticas não conseguem detectar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmPrimaryPass {

    private final MultiModelLLMRouter llmRouter;
    private final ObjectMapper objectMapper;
    private final ConfigurationResolver configurationResolver;
    private final ReviewRuleResolver ruleResolver;

    /**
     * Executa a análise LLM primária.
     *
     * @param pullRequest O PR a ser analisado
     * @param review      O review associado
     * @param sastResult  Resultados da passada SAST (contexto)
     * @return PassResult com os issues encontrados
     */
    public PassResult execute(PullRequest pullRequest, Review review, PassResult sastResult,
                              List<GitHubService.FileDiff> diffs) {
        long startTime = System.currentTimeMillis();

        String repoIdentifier = pullRequest.getProject() != null
                ? pullRequest.getProject().getName()
                : "unknown";
        log.debug("Starting LLM Primary analysis for PR {}/{}", repoIdentifier, pullRequest.getPrNumber());

        List<Issue> issues = new ArrayList<>();

        try {
            // Preparar contexto do SAST
            String sastContext = buildSastContext(sastResult);

            // Para cada arquivo alterado, fazer análise LLM
            Map<String, List<FileChange>> changesByFile = groupChangesByFile(diffs);

            for (var entry : changesByFile.entrySet()) {
                String filePath = entry.getKey();
                List<FileChange> changes = entry.getValue();

                // Análise LLM do arquivo
                List<Issue> fileIssues = analyzeFileWithLLM(filePath, changes, sastContext, review);
                issues.addAll(fileIssues);
            }

            log.debug("LLM Primary pass completed: {} issues found", issues.size());

        } catch (Exception e) {
            log.warn("LLM Primary analysis encountered errors", e);
        }

        PassResult result = new PassResult();
        result.setPassName("LLM Primary Analysis");
        result.setSuccess(true);
        result.setIssues(issues);
        result.setDurationMs(System.currentTimeMillis() - startTime);

        // Metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filesAnalyzed", issues.stream()
                .map(Issue::getFilePath)
                .distinct()
                .count());
        metadata.put("modelUsed", "router-based");
        result.setMetadata(metadata);

        return result;
    }

    /**
     * Constrói um contexto resumido dos resultados SAST.
     */
    private String buildSastContext(PassResult sastResult) {
        if (sastResult == null || sastResult.getIssues() == null || sastResult.getIssues().isEmpty()) {
            return "No SAST issues found.";
        }

        Map<String, Long> issuesByFile = sastResult.getIssues().stream()
                .collect(Collectors.groupingBy(
                        Issue::getFilePath,
                        Collectors.counting()
                ));

        StringBuilder sb = new StringBuilder();
        sb.append("SAST Analysis Summary:\n");
        for (var entry : issuesByFile.entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" issues\n");
        }

        return sb.toString();
    }

    /**
     * Agrupa mudanças por arquivo a partir dos diffs do GitHub.
     */
    private Map<String, List<FileChange>> groupChangesByFile(List<GitHubService.FileDiff> diffs) {
        Map<String, List<FileChange>> changesByFile = new HashMap<>();

        for (GitHubService.FileDiff diff : diffs) {
            if (diff.patch() == null || diff.patch().isBlank()) {
                continue;
            }

            FileChange.ChangeType changeType = switch (diff.status()) {
                case "added" -> FileChange.ChangeType.ADDED;
                case "removed" -> FileChange.ChangeType.DELETED;
                default -> FileChange.ChangeType.MODIFIED;
            };

            FileChange change = FileChange.builder()
                    .filePath(diff.filename())
                    .lineStart(1)
                    .lineEnd(diff.additions() + diff.deletions())
                    .diff(diff.patch())
                    .type(changeType)
                    .build();

            changesByFile.computeIfAbsent(diff.filename(), k -> new ArrayList<>()).add(change);
        }

        return changesByFile;
    }

    /**
     * Analisa um arquivo específico usando LLM.
     */
    private List<Issue> analyzeFileWithLLM(String filePath, List<FileChange> changes,
                                           String sastContext, Review review) {
        List<Issue> issues = new ArrayList<>();

        try {
            // Resolver idioma do projeto
            Long projectId = review.getPullRequest() != null && review.getPullRequest().getProject() != null
                    ? review.getPullRequest().getProject().getId() : null;
            String language = projectId != null
                    ? configurationResolver.getConfig(projectId, ConfigKeys.REVIEW_LANGUAGE) : null;

            // Checklist determinístico por tipo de arquivo (rule matching)
            String rule = isEnabled(projectId, ConfigKeys.REVIEW_RULE_GUIDANCE_ENABLED)
                    ? ruleResolver.resolve(filePath) : "";

            // Plan phase: para arquivos grandes, gera um mapa de risco que foca
            // a análise principal (gated por threshold de linhas alteradas).
            String planGuidance = buildPlanGuidance(projectId, filePath, changes, rule);

            // Construir prompt para análise
            String systemPrompt = buildSystemPrompt(language);
            String userPrompt = buildAnalysisPrompt(filePath, changes, sastContext, rule, planGuidance);

            // Executar análise via LLM router
            var response = llmRouter.execute(
                    ReviewTaskType.BUG_DETECTION,
                    systemPrompt,
                    userPrompt
            );

            // Parse resposta e gerar issues
            issues = parseLLMResponse(response.content(), filePath, review);

        } catch (Exception e) {
            log.warn("LLM analysis failed for file {}: {}", filePath, e.getMessage());
        }

        return issues;
    }

    /**
     * Constrói o prompt do sistema para análise LLM.
     */
    private String buildSystemPrompt(String language) {
        String langInstruction = resolveLanguageInstruction(language);
        return langInstruction + """
            You are an expert code reviewer. Think step by step when analyzing code changes.

            For each potential issue you find:
            1. First, understand the context and intent of the code change
            2. Then, reason about whether the code correctly implements that intent
            3. Consider edge cases, error handling, and interactions with other code
            4. Explain your reasoning clearly so the developer understands WHY it's an issue

            Categories to analyze:
            - **Bugs**: Logic errors, null pointer exceptions, race conditions, resource leaks
            - **Code Smells**: Long methods, duplicated code, confusing names, magic numbers
            - **Architecture Issues**: Violation of SOLID principles, tight coupling, low cohesion
            - **Performance Issues**: Inefficient algorithms, N+1 queries, unnecessary allocations
            - **Maintainability**: Complex conditions, deep nesting, large parameter lists

            Format your response as JSON:
            ```json
            {
              "issues": [
                {
                  "title": "Short descriptive title",
                  "description": "Detailed explanation of the problem",
                  "reasoning": "Step-by-step reasoning of how you identified this issue and why it matters",
                  "severity": "CRITICAL|HIGH|MEDIUM|LOW",
                  "line": 123,
                  "existing_code": "the exact original line(s) this issue refers to, copied VERBATIM from the diff (without the leading +/- markers)",
                  "category": "BUG|CODE_SMELL|PERFORMANCE|ARCHITECTURE",
                  "suggestion": "Concrete suggestion for how to fix this issue"
                }
              ]
            }
            ```

            Important:
            - Only report real issues, not stylistic preferences
            - Be specific about line numbers and affected code
            - ALWAYS fill "existing_code" with the offending line(s) copied verbatim
              from the diff (no +/- markers). This is used to locate the issue
              precisely, so it must match the source exactly.
            - Provide actionable suggestions, not vague advice
            - Higher severity issues require stronger evidence in your reasoning
            """;
    }

    /**
     * Constrói o prompt de análise para um arquivo específico.
     */
    private String buildAnalysisPrompt(String filePath, List<FileChange> changes, String sastContext,
                                       String rule, String planGuidance) {
        StringBuilder sb = new StringBuilder();
        sb.append("Review the following code changes:\n\n");
        sb.append("**File**: ").append(filePath).append("\n\n");

        if (!changes.isEmpty()) {
            sb.append("**Changes**:\n");
            sb.append("```diff\n");
            for (FileChange change : changes) {
                sb.append(change.getDiff()).append("\n");
            }
            sb.append("```\n\n");
        }

        sb.append("**SAST Context**:\n");
        sb.append(sastContext).append("\n\n");

        if (rule != null && !rule.isBlank()) {
            sb.append("**Review checklist for this file type** (focus your analysis here):\n");
            sb.append(rule).append("\n\n");
        }

        if (planGuidance != null && !planGuidance.isBlank()) {
            sb.append("**Suggested focus areas (review plan)**:\n");
            sb.append(planGuidance).append("\n\n");
        }

        sb.append("Please analyze and return issues in the specified JSON format.");

        return sb.toString();
    }

    /**
     * Plan phase: para arquivos grandes (acima do threshold de linhas), pede ao
     * LLM um mapa de risco curto que será injetado na análise principal para
     * focar a atenção. Retorna "" quando desabilitada, abaixo do threshold ou
     * em caso de falha (degradação graciosa).
     */
    private String buildPlanGuidance(Long projectId, String filePath, List<FileChange> changes, String rule) {
        if (!isEnabled(projectId, ConfigKeys.REVIEW_PLAN_PHASE_ENABLED)) {
            return "";
        }
        int threshold = parseIntConfig(projectId, ConfigKeys.REVIEW_PLAN_LINE_THRESHOLD, 50);
        int changed = countChangedLines(changes);
        if (changed < threshold) {
            return "";
        }

        try {
            String systemPrompt = """
                You are a code review planner. Given a diff, produce a SHORT risk map
                that will focus a deeper review — do not review in detail yet.

                Output 2-5 bullet points. Each bullet: the specific risk area (with a
                hint of where in the diff) and why it deserves attention. Be concrete
                and concise. No preamble, no JSON, just the bullets.
                """;

            StringBuilder up = new StringBuilder();
            up.append("**File**: ").append(filePath).append(" (").append(changed)
                    .append(" changed lines)\n\n```diff\n");
            for (FileChange change : changes) {
                up.append(change.getDiff()).append("\n");
            }
            up.append("```\n");
            if (rule != null && !rule.isBlank()) {
                up.append("\nFile-type checklist to consider:\n").append(rule).append("\n");
            }
            up.append("\nList the top risk areas to review.");

            var response = llmRouter.execute(ReviewTaskType.PRE_FILTER, systemPrompt, up.toString());
            String plan = response.content();
            return plan == null ? "" : plan.strip();
        } catch (Exception e) {
            log.debug("Plan phase skipped for {}: {}", filePath, e.getMessage());
            return "";
        }
    }

    /** Conta as linhas adicionadas/removidas (marcadores +/-) no diff do arquivo. */
    private int countChangedLines(List<FileChange> changes) {
        int count = 0;
        for (FileChange change : changes) {
            String diff = change.getDiff();
            if (diff == null) {
                continue;
            }
            for (String line : diff.split("\n")) {
                if (line.isEmpty()) {
                    continue;
                }
                char c = line.charAt(0);
                if ((c == '+' && !line.startsWith("+++")) || (c == '-' && !line.startsWith("---"))) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Resolve um flag booleano de configuração (default seguro: ligado). */
    private boolean isEnabled(Long projectId, String configKey) {
        if (projectId == null) {
            return true;
        }
        return Boolean.parseBoolean(configurationResolver.getConfig(projectId, configKey));
    }

    private int parseIntConfig(Long projectId, String configKey, int fallback) {
        if (projectId == null) {
            return fallback;
        }
        try {
            String value = configurationResolver.getConfig(projectId, configKey);
            return value != null ? Integer.parseInt(value.trim()) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Parse da resposta LLM para extrair issues.
     */
    private List<Issue> parseLLMResponse(String response, String filePath, Review review) {
        List<Issue> issues = new ArrayList<>();

        try {
            String jsonBlock = extractJsonBlock(response);
            if (jsonBlock == null || jsonBlock.isBlank()) {
                if (response.length() > 50) {
                    issues.add(createGenericIssue(response, filePath, review));
                }
                return issues;
            }

            // Tentar parse como objeto com campo "issues"
            LlmIssueResponse parsed = null;
            try {
                parsed = objectMapper.readValue(jsonBlock, LlmIssueResponse.class);
            } catch (Exception e) {
                // Tentar parse como array direto
                try {
                    LlmIssue[] issueArray = objectMapper.readValue(jsonBlock, LlmIssue[].class);
                    parsed = new LlmIssueResponse(List.of(issueArray));
                } catch (Exception e2) {
                    log.debug("JSON parse failed, falling back to generic issue: {}", e2.getMessage());
                }
            }

            if (parsed != null && parsed.issues() != null) {
                for (LlmIssue llmIssue : parsed.issues()) {
                    Severity severity = parseSeverity(llmIssue.severity());
                    IssueType type = parseIssueType(llmIssue.category());

                    // Enrich description with reasoning if available
                    String description = llmIssue.description() != null ? llmIssue.description() : "";
                    if (llmIssue.reasoning() != null && !llmIssue.reasoning().isBlank()) {
                        description += "\n\n**Reasoning:** " + llmIssue.reasoning();
                    }

                    issues.add(Issue.builder()
                            .review(review)
                            .type(type)
                            .severity(severity)
                            .title(llmIssue.title() != null ? llmIssue.title() : "Code Review Issue")
                            .description(description)
                            .suggestion(llmIssue.suggestion())
                            .filePath(filePath)
                            .lineStart(llmIssue.line() != null ? llmIssue.line() : 1)
                            .lineEnd(llmIssue.line() != null ? llmIssue.line() : 1)
                            .codeSnippet(llmIssue.existingCode())
                            .ruleId("LLM_ANALYSIS")
                            .source(IssueSource.LLM)
                            .createdAt(LocalDateTime.now())
                            .build());
                }
            } else if (jsonBlock.length() > 50) {
                issues.add(createGenericIssue(jsonBlock, filePath, review));
            }

        } catch (Exception e) {
            log.debug("Failed to parse LLM response: {}", e.getMessage());
        }

        return issues;
    }

    private Severity parseSeverity(String severity) {
        if (severity == null) return Severity.MEDIUM;
        try {
            return Severity.valueOf(severity.toUpperCase());
        } catch (Exception e) {
            return Severity.MEDIUM;
        }
    }

    private IssueType parseIssueType(String category) {
        if (category == null) return IssueType.CODE_SMELL;
        try {
            return IssueType.valueOf(category.toUpperCase());
        } catch (Exception e) {
            String lower = category.toLowerCase();
            if (lower.contains("bug")) return IssueType.BUG;
            if (lower.contains("security")) return IssueType.SECURITY;
            if (lower.contains("performance")) return IssueType.PERFORMANCE;
            if (lower.contains("architecture")) return IssueType.CODE_SMELL;
            return IssueType.CODE_SMELL;
        }
    }

    // Records for JSON deserialization
    private record LlmIssueResponse(List<LlmIssue> issues) {}
    private record LlmIssue(String title, String description, String reasoning,
                             String severity, Integer line,
                             @JsonProperty("existing_code") String existingCode,
                             String category, String suggestion) {}

    /**
     * Extrai o bloco JSON de uma resposta markdown.
     */
    private String extractJsonBlock(String response) {
        int jsonStart = response.indexOf("```json");
        if (jsonStart == -1) {
            jsonStart = response.indexOf("```");
        }
        if (jsonStart == -1) {
            return response;
        }

        int contentStart = response.indexOf("\n", jsonStart) + 1;
        int jsonEnd = response.indexOf("```", contentStart);

        if (jsonEnd == -1) {
            return response.substring(contentStart);
        }

        return response.substring(contentStart, jsonEnd).trim();
    }

    /**
     * Cria um issue genérico baseado na resposta LLM.
     */
    private Issue createGenericIssue(String content, String filePath, Review review) {
        // Extrai primeira linha como título
        String title = "Code Review Suggestion";
        String description = content;

        if (content.contains("\n")) {
            title = content.substring(0, Math.min(60, content.indexOf("\n")));
            description = content;
        }

        // Tenta detectar severidade
        Severity severity = Severity.MEDIUM;
        String lowerContent = content.toLowerCase();
        if (lowerContent.contains("critical") || lowerContent.contains("security")) {
            severity = Severity.HIGH;
        } else if (lowerContent.contains("minor") || lowerContent.contains("nit")) {
            severity = Severity.LOW;
        }

        return Issue.builder()
                .review(review)
                .type(IssueType.CODE_SMELL)
                .severity(severity)
                .title(title)
                .description(description)
                .filePath(filePath)
                .lineStart(1)
                .lineEnd(1)
                .ruleId("LLM_ANALYSIS")
                .source(IssueSource.LLM)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Resolve instrução de idioma para o system prompt.
     */
    private String resolveLanguageInstruction(String language) {
        if (language == null || language.isBlank() || "en".equalsIgnoreCase(language)) {
            return "";
        }
        return switch (language.toLowerCase()) {
            case "pt", "pt-br" -> "IMPORTANT: Write all your responses (titles, descriptions, suggestions) in Brazilian Portuguese.\n\n";
            case "es" -> "IMPORTANT: Write all your responses (titles, descriptions, suggestions) in Spanish.\n\n";
            default -> "IMPORTANT: Write all your responses in " + language + ".\n\n";
        };
    }

    // ========== DTOs ==========

    /**
     * Representa uma mudança em um arquivo.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class FileChange {
        private String filePath;
        private Integer lineStart;
        private Integer lineEnd;
        private String diff;
        private ChangeType type;

        public enum ChangeType {
            ADDED, MODIFIED, DELETED
        }
    }
}
