package com.pullwise.api.application.service.review.pipeline.pass;

import com.pullwise.api.application.service.graph.CodeGraphService;
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

/**
 * Passada 4: Code Graph Impact Analysis
 *
 * <p>Analisa o impacto das mudanças através de:
 * - Call graphs (quais funções chamam quais)
 * - Dependency graphs (dependências entre módulos)
 * - Blast radius (impacto potencial da mudança)
 * - Risk score (baseado em criticidade dos afetados)
 *
 * <p>Esta passada identifica problemas que afetam outros arquivos/componentes
 * que não foram diretamente modificados no PR.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeGraphImpactPass {

    private final CodeGraphService codeGraphService;
    private final MultiModelLLMRouter llmRouter;

    /**
     * Executa a análise de impacto no grafo de código.
     *
     * @param pullRequest  O PR a ser analisado
     * @param review       O review associado
     * @param sastResult   Resultados do SAST
     * @param llmResult    Resultados da análise LLM primária
     * @return PassResult com issues de impacto
     */
    public PassResult execute(PullRequest pullRequest, Review review,
                             PassResult sastResult, PassResult llmResult) {
        long startTime = System.currentTimeMillis();

        String repoIdentifier = pullRequest.getProject() != null
                ? pullRequest.getProject().getName()
                : "unknown";
        log.debug("Starting Code Graph Impact analysis for PR {}/{}", repoIdentifier, pullRequest.getPrNumber());

        List<Issue> issues = new ArrayList<>();

        try {
            // Analisar impacto no grafo de código
            List<ImpactAnalysis> impacts = analyzeImpact(pullRequest, review);

            // Gerar issues baseados em alto impacto
            for (ImpactAnalysis impact : impacts) {
                if (impact.getRiskScore() >= 7) {  // Alto risco
                    Issue issue = createImpactIssue(impact, review);
                    issues.add(issue);
                }
            }

            log.debug("Impact pass completed: {} high-impact issues found", issues.size());

        } catch (Exception e) {
            log.warn("Impact analysis encountered errors", e);
        }

        PassResult result = new PassResult();
        result.setPassName("Code Graph Impact Analysis");
        result.setSuccess(true);
        result.setIssues(issues);
        result.setDurationMs(System.currentTimeMillis() - startTime);

        // Metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filesAnalyzed", 0);  // TODO: implementar contagem de arquivos
        result.setMetadata(metadata);

        return result;
    }

    /**
     * Analisa o impacto das mudanças no grafo de código.
     */
    private List<ImpactAnalysis> analyzeImpact(PullRequest pullRequest, Review review) {
        List<ImpactAnalysis> impacts = new ArrayList<>();

        try {
            // TODO: Obter arquivos alterados do diff do PR
            // Por ora, retorna lista vazia
            List<String> changedFiles = List.of();

            String repoIdentifier = pullRequest.getProject() != null
                    ? pullRequest.getProject().getName()
                    : "unknown";

            for (String filePath : changedFiles) {
                // Calcular blast radius
                int blastRadius = codeGraphService.calculateBlastRadius(
                        repoIdentifier,
                        filePath
                );

                if (blastRadius > 0) {
                    ImpactAnalysis impact = ImpactAnalysis.builder()
                            .filePath(filePath)
                            .blastRadius(blastRadius)
                            .riskScore(calculateRiskScore(blastRadius, filePath))
                            .affectedFiles(codeGraphService.getAffectedFiles(
                                    repoIdentifier,
                                    filePath
                            ))
                            .build();

                    impacts.add(impact);
                }
            }

        } catch (Exception e) {
            log.warn("Failed to analyze code graph impact: {}", e.getMessage());
        }

        return impacts;
    }

    /**
     * Calcula score de risco baseado no blast radius e criticidade do arquivo.
     */
    private int calculateRiskScore(int blastRadius, String filePath) {
        int baseScore = Math.min(10, blastRadius / 5);  // 0-10 baseado no blast radius

        // Arquivo crítico?
        if (isCriticalFile(filePath)) {
            baseScore = Math.min(10, baseScore + 3);
        }

        return baseScore;
    }

    /**
     * Verifica se o arquivo é crítico.
     */
    private boolean isCriticalFile(String filePath) {
        String lower = filePath.toLowerCase();
        return lower.contains("auth") || lower.contains("security") ||
                lower.contains("payment") || lower.contains("config") ||
                lower.contains("controller") && lower.contains("base");
    }

    /**
     * Cria um issue baseado em análise de impacto.
     */
    private Issue createImpactIssue(ImpactAnalysis impact, Review review) {
        Severity severity = switch (impact.getRiskScore()) {
            case 9, 10 -> Severity.CRITICAL;
            case 7, 8 -> Severity.HIGH;
            case 5, 6 -> Severity.MEDIUM;
            default -> Severity.LOW;
        };

        return Issue.builder()
                .review(review)
                .type(IssueType.CODE_SMELL)
                .severity(severity)
                .title("High-impact change detected")
                .description(buildImpactDescription(impact))
                .filePath(impact.getFilePath())
                .lineStart(1)
                .lineEnd(1)
                .ruleId("CODE_GRAPH_IMPACT")
                .source(IssueSource.LLM)
                .build();
    }

    /**
     * Constrói descrição do impacto.
     */
    private String buildImpactDescription(ImpactAnalysis impact) {
        StringBuilder sb = new StringBuilder();
        sb.append("This change affects approximately **").append(impact.getBlastRadius())
                .append("** other files/components.\n\n");

        if (impact.getAffectedFiles() != null && !impact.getAffectedFiles().isEmpty()) {
            sb.append("**Affected files**:\n");
            impact.getAffectedFiles().stream()
                    .limit(10)  // Limita a 10 arquivos
                    .forEach(f -> sb.append("- ").append(f).append("\n"));

            if (impact.getAffectedFiles().size() > 10) {
                sb.append("- ... and ").append(impact.getAffectedFiles().size() - 10).append(" more\n");
            }
        }

        sb.append("\n**Risk Score**: ").append(impact.getRiskScore()).append("/10");

        return sb.toString();
    }

    // ========== DTOs ==========

    /**
     * Resultado da análise de impacto.
     */
    @lombok.Data
    @lombok.Builder
    public static class ImpactAnalysis {
        private String filePath;
        private int blastRadius;  // Número de arquivos afetados
        private int riskScore;     // 0-10
        private List<String> affectedFiles;
    }
}
