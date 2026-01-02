package com.pullwise.api.application.service.review.pipeline.pass;

import com.pullwise.api.application.service.integration.SonarQubeService;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Passada 1: SAST Aggregator
 *
 * <p>Executa múltiplas ferramentas de análise estática em paralelo e agrega os resultados.
 *
 * <p>Ferramentas suportadas por linguagem:
 * <ul>
 *   <li><b>Java:</b> Checkstyle, PMD, SpotBugs, SonarQube, Error Prone</li>
 *   <li><b>JavaScript/TypeScript:</b> ESLint, Biome (Rust), TSLint</li>
 *   <li><b>Python:</b> Ruff (Rust), Pylint, Flake8, Black, MyPy</li>
 *   <li><b>Go:</b> Golint, Gofmt, Staticcheck</li>
 *   <li><b>Ruby:</b> Rubocop, Brakeman (security)</li>
 *   <li><b>PHP:</b> PHPStan, Psalm, PHP CS Fixer</li>
 *   <li><b>C#:</b> Roslyn Analyzers, StyleCop</li>
 * </ul>
 *
 * <p>Nota: Rust-based tools (Biome, Ruff) são 10-100x mais rápidos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SastAggregatorPass {

    private final SonarQubeService sonarQubeService;
    private final SastToolExecutor toolExecutor;

    /**
     * Executa todas as ferramentas SAST em paralelo.
     *
     * @param pullRequest O PR a ser analisado
     * @param review      O review associado
     * @return PassResult com os issues encontrados
     */
    public PassResult execute(PullRequest pullRequest, Review review) {
        long startTime = System.currentTimeMillis();

        String repoIdentifier = pullRequest.getProject() != null
                ? pullRequest.getProject().getName()
                : "unknown";
        log.debug("Starting SAST aggregation for PR {}/{}", repoIdentifier, pullRequest.getPrNumber());

        // Detectar linguagens usadas no PR
        Set<ProgrammingLanguage> languages = detectLanguages(pullRequest);
        log.debug("Detected languages: {}", languages);

        // Selecionar ferramentas apropriadas
        List<SastTool> tools = selectTools(languages);
        log.debug("Selected {} tools for analysis", tools.size());

        // Executar ferramentas em paralelo
        Map<SastTool, CompletableFuture<List<ToolIssue>>> futures = new ConcurrentHashMap<>();

        for (SastTool tool : tools) {
            CompletableFuture<List<ToolIssue>> future = CompletableFuture.supplyAsync(
                    () -> executeTool(tool, pullRequest, review)
            );
            futures.put(tool, future);
        }

        // Aguardar todos os resultados
        Map<SastTool, List<ToolIssue>> toolResults = new ConcurrentHashMap<>();
        futures.forEach((tool, future) -> {
            try {
                List<ToolIssue> issues = future.get();
                if (!issues.isEmpty()) {
                    toolResults.put(tool, issues);
                    log.debug("{} found {} issues", tool.getName(), issues.size());
                }
            } catch (Exception e) {
                log.warn("Tool {} failed: {}", tool.getName(), e.getMessage());
            }
        });

        // Converter ToolIssues para Issues do domínio
        List<Issue> issues = convertToDomainIssues(toolResults, review);
        log.debug("SAST pass completed: {} issues from {} tools", issues.size(), toolResults.size());

        // Criar resultado
        PassResult result = new PassResult();
        result.setPassName("SAST Aggregation");
        result.setSuccess(true);
        result.setIssues(issues);
        result.setDurationMs(System.currentTimeMillis() - startTime);

        // Metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("languages", languages.stream().map(Enum::name).toList());
        metadata.put("toolsExecuted", tools.size());
        metadata.put("toolsWithIssues", toolResults.size());
        metadata.put("totalIssues", issues.size());

        // Breakdown por severidade
        Map<String, Long> severityBreakdown = issues.stream()
                .collect(Collectors.groupingBy(i -> i.getSeverity().name(), Collectors.counting()));
        metadata.put("severityBreakdown", severityBreakdown);

        // Breakdown por ferramenta
        Map<String, Integer> toolBreakdown = toolResults.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().getName(),
                        e -> e.getValue().size()
                ));
        metadata.put("toolBreakdown", toolBreakdown);

        result.setMetadata(metadata);

        return result;
    }

    /**
     * Detecta as linguagens de programação usadas no PR.
     */
    private Set<ProgrammingLanguage> detectLanguages(PullRequest pullRequest) {
        Set<ProgrammingLanguage> languages = new HashSet<>();

        // TODO: Implementar detecção baseada nas extensões dos arquivos alterados
        // Por ora, usa Java como padrão
        languages.add(ProgrammingLanguage.JAVA);

        // Fallback final
        if (languages.isEmpty()) {
            languages.add(ProgrammingLanguage.JAVA);
        }

        return languages;
    }

    /**
     * Seleciona as ferramentas apropriadas para as linguagens detectadas.
     */
    private List<SastTool> selectTools(Set<ProgrammingLanguage> languages) {
        List<SastTool> tools = new ArrayList<>();

        for (ProgrammingLanguage language : languages) {
            tools.addAll(getToolsForLanguage(language));
        }

        return tools;
    }

    /**
     * Retorna as ferramentas disponíveis para uma linguagem.
     */
    private List<SastTool> getToolsForLanguage(ProgrammingLanguage language) {
        return switch (language) {
            case JAVA -> List.of(
                    SastTool.CHECKSTYLE,
                    SastTool.PMD,
                    SastTool.SPOTBUGS,
                    SastTool.SONARQUBE,
                    SastTool.ERROR_PRONE
            );
            case JAVASCRIPT, TYPESCRIPT -> List.of(
                    SastTool.ESLINT,
                    SastTool.BIOME,
                    SastTool.TSLINT
            );
            case PYTHON -> List.of(
                    SastTool.RUFF,
                    SastTool.PYLINT,
                    SastTool.FLAKE8,
                    SastTool.MYPY
            );
            case GO -> List.of(
                    SastTool.GOLINT,
                    SastTool.GOFMT,
                    SastTool.STATICCHECK
            );
            case RUBY -> List.of(
                    SastTool.RUBOCOP,
                    SastTool.BRAKEMAN
            );
            case PHP -> List.of(
                    SastTool.PHPSTAN,
                    SastTool.PSALM,
                    SastTool.PHPCSFIXER
            );
            case CSHARP -> List.of(
                    SastTool.ROSLYN,
                    SastTool.STYLECOP
            );
            default -> List.of(); // Linguagem não suportada
        };
    }

    /**
     * Executa uma ferramenta específica.
     */
    private List<ToolIssue> executeTool(SastTool tool, PullRequest pullRequest, Review review) {
        try {
            return toolExecutor.execute(tool, pullRequest, review);
        } catch (Exception e) {
            log.warn("Error executing {}: {}", tool.getName(), e.getMessage());
            return List.of();
        }
    }

    /**
     * Converter issues das ferramentas para Issues do domínio.
     */
    private List<Issue> convertToDomainIssues(Map<SastTool, List<ToolIssue>> toolResults, Review review) {
        List<Issue> issues = new ArrayList<>();

        for (var entry : toolResults.entrySet()) {
            SastTool tool = entry.getKey();
            for (ToolIssue toolIssue : entry.getValue()) {
                Issue issue = Issue.builder()
                        .review(review)
                        .type(mapToIssueType(toolIssue.getRule()))
                        .severity(toolIssue.getSeverity())
                        .title(toolIssue.getTitle())
                        .description(toolIssue.getDescription())
                        .filePath(toolIssue.getFilePath())
                        .lineStart(toolIssue.getLine())
                        .lineEnd(toolIssue.getEndLine())
                        .ruleId(toolIssue.getRule())
                        .source(mapToIssueSource(tool))
                        .createdAt(LocalDateTime.now())
                        .build();

                issues.add(issue);
            }
        }

        return issues;
    }

    /**
     * Mapeia uma ferramenta para uma IssueSource.
     */
    private IssueSource mapToIssueSource(SastTool tool) {
        return switch (tool) {
            case CHECKSTYLE -> IssueSource.CHECKSTYLE;
            case PMD -> IssueSource.PMD;
            case SPOTBUGS -> IssueSource.SPOTBUGS;
            case SONARQUBE -> IssueSource.SONARQUBE;
            default -> IssueSource.CUSTOM;
        };
    }

    /**
     * Mapeia uma regra para um IssueType.
     */
    private IssueType mapToIssueType(String rule) {
        if (rule == null) return IssueType.CODE_SMELL;

        String lowerRule = rule.toLowerCase();
        if (lowerRule.contains("security") || lowerRule.contains("xss") || lowerRule.contains("sql")) {
            return IssueType.SECURITY;
        } else if (lowerRule.contains("bug") || lowerRule.contains("npe") || lowerRule.contains("null")) {
            return IssueType.BUG;
        } else if (lowerRule.contains("perf") || lowerRule.contains("slow")) {
            return IssueType.PERFORMANCE;
        } else if (lowerRule.contains("style") || lowerRule.contains("format")) {
            return IssueType.STYLE;
        }
        return IssueType.CODE_SMELL;
    }

    // ========== Inner Classes ==========

    /**
     * Representa uma ferramenta SAST.
     */
    public enum SastTool {
        // Java
        CHECKSTYLE("Checkstyle", "style", List.of("java")),
        PMD("PMD", "quality", List.of("java")),
        SPOTBUGS("SpotBugs", "bug", List.of("java")),
        SONARQUBE("SonarQube", "quality", List.of("java", "js", "ts", "py")),
        ERROR_PRONE("Error Prone", "bug", List.of("java")),

        // JavaScript/TypeScript
        ESLINT("ESLint", "style", List.of("js", "ts")),
        BIOME("Biome", "style", List.of("js", "ts")),  // Rust-based, muito rápido
        TSLINT("TSLint", "style", List.of("ts")),

        // Python
        RUFF("Ruff", "style", List.of("py")),  // Rust-based, 100x mais rápido
        PYLINT("Pylint", "quality", List.of("py")),
        FLAKE8("Flake8", "style", List.of("py")),
        MYPY("MyPy", "type", List.of("py")),

        // Go
        GOLINT("Golint", "style", List.of("go")),
        GOFMT("Gofmt", "format", List.of("go")),
        STATICCHECK("Staticcheck", "bug", List.of("go")),

        // Ruby
        RUBOCOP("RuboCop", "style", List.of("rb")),
        BRAKEMAN("Brakeman", "security", List.of("rb")),

        // PHP
        PHPSTAN("PHPStan", "quality", List.of("php")),
        PSALM("Psalm", "type", List.of("php")),
        PHPCSFIXER("PHP CS Fixer", "format", List.of("php")),

        // C#
        ROSLYN("Roslyn Analyzers", "quality", List.of("cs")),
        STYLECOP("StyleCop", "style", List.of("cs"));

        private final String name;
        private final String category;
        private final List<String> languageExtensions;

        SastTool(String name, String category, List<String> languageExtensions) {
            this.name = name;
            this.category = category;
            this.languageExtensions = languageExtensions;
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public List<String> getLanguageExtensions() {
            return languageExtensions;
        }
    }

    /**
     * Issue retornado por uma ferramenta SAST.
     */
    @lombok.Data
    @lombok.Builder
    public static class ToolIssue {
        private String filePath;
        private Integer line;
        private Integer endLine;
        private Severity severity;
        private String title;
        private String description;
        private String rule;
        private String category;
    }
}
