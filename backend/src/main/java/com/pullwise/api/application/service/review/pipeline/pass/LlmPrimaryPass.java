package com.pullwise.api.application.service.review.pipeline.pass;

import com.pullwise.api.application.service.llm.router.MultiModelLLMRouter;
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

    /**
     * Executa a análise LLM primária.
     *
     * @param pullRequest O PR a ser analisado
     * @param review      O review associado
     * @param sastResult  Resultados da passada SAST (contexto)
     * @return PassResult com os issues encontrados
     */
    public PassResult execute(PullRequest pullRequest, Review review, PassResult sastResult) {
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
            Map<String, List<FileChange>> changesByFile = groupChangesByFile(pullRequest);

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
     * Agrupa mudanças por arquivo.
     */
    private Map<String, List<FileChange>> groupChangesByFile(PullRequest pullRequest) {
        // Em produção, parsearia o diff real
        // Por ora, retorna mapa vazio (será populado pelo diff parser)
        return new HashMap<>();
    }

    /**
     * Analisa um arquivo específico usando LLM.
     */
    private List<Issue> analyzeFileWithLLM(String filePath, List<FileChange> changes,
                                           String sastContext, Review review) {
        List<Issue> issues = new ArrayList<>();

        try {
            // Construir prompt para análise
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildAnalysisPrompt(filePath, changes, sastContext);

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
    private String buildSystemPrompt() {
        return """
            You are an expert code reviewer. Analyze the provided code changes and identify:

            1. **Bugs**: Logic errors, null pointer exceptions, race conditions, resource leaks
            2. **Code Smells**: Long methods, duplicated code, confusing names, magic numbers
            3. **Architecture Issues**: Violation of SOLID principles, tight coupling, low cohesion
            4. **Performance Issues**: Inefficient algorithms, N+1 queries, unnecessary allocations
            5. **Maintainability**: Complex conditions, deep nesting, large parameter lists

            Format your response as JSON:
            ```json
            {
              "issues": [
                {
                  "title": "Short descriptive title",
                  "description": "Detailed explanation",
                  "severity": "CRITICAL|HIGH|MEDIUM|LOW",
                  "line": 123,
                  "category": "BUG|CODE_SMELL|PERFORMANCE|ARCHITECTURE"
                }
              ]
            }
            ```
            """;
    }

    /**
     * Constrói o prompt de análise para um arquivo específico.
     */
    private String buildAnalysisPrompt(String filePath, List<FileChange> changes, String sastContext) {
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

        sb.append("Please analyze and return issues in the specified JSON format.");

        return sb.toString();
    }

    /**
     * Parse da resposta LLM para extrair issues.
     */
    private List<Issue> parseLLMResponse(String response, String filePath, Review review) {
        List<Issue> issues = new ArrayList<>();

        try {
            // Tenta extrair JSON da resposta
            String jsonBlock = extractJsonBlock(response);
            if (jsonBlock == null) {
                // Se não conseguiu extrair JSON, tenta criar um issue genérico
                if (response.length() > 50) {
                    issues.add(createGenericIssue(response, filePath, review));
                }
                return issues;
            }

            // TODO: Parse JSON e criar issues
            // Por ora, cria um issue genérico com o conteúdo da resposta
            if (jsonBlock.length() > 50) {
                issues.add(createGenericIssue(jsonBlock, filePath, review));
            }

        } catch (Exception e) {
            log.debug("Failed to parse LLM response: {}", e.getMessage());
        }

        return issues;
    }

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
