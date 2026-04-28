package com.pullwise.api.application.service.review.pipeline.pass;

import com.pullwise.api.application.service.graph.blast.BlastRadiusOptions;
import com.pullwise.api.application.service.graph.blast.BlastRadiusResult;
import com.pullwise.api.application.service.graph.blast.BlastRadiusService;
import com.pullwise.api.application.service.graph.blast.ImpactedNode;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.review.pipeline.MultiPassReviewOrchestrator.PassResult;
import com.pullwise.api.domain.enums.IssueSource;
import com.pullwise.api.domain.enums.IssueType;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.model.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Passada 4: Code Graph Impact Analysis (Blast-Radius v2).
 *
 * <p>Calcula o blast-radius do PR via {@link BlastRadiusService} (BFS forward
 * em CALLS / IMPORTS_FROM / INHERITS, com confidence tiers e risk scoring).
 * Gera um issue agregado por arquivo alterado quando há nós atingidos com
 * risk >= {@link #IMPACT_ISSUE_THRESHOLD}.
 *
 * <p>O {@link BlastRadiusResult} é exposto em {@code PassResult.metadata.blastRadius}
 * para a fase de Consolidation usar (promoção de severidade de issues SAST/LLM
 * cujo arquivo cai no blast radius).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeGraphImpactPass {

    /** riskScore mínimo para criar issue dedicado de impacto. */
    public static final double IMPACT_ISSUE_THRESHOLD = 0.55;

    /** Quantos top nodes incluir no description do issue. */
    private static final int TOP_N_IN_DESCRIPTION = 5;

    public static final String METADATA_BLAST_RADIUS = "blastRadius";

    private final BlastRadiusService blastRadiusService;

    public PassResult execute(PullRequest pullRequest, Review review,
                              PassResult sastResult, PassResult llmResult,
                              List<GitHubService.FileDiff> diffs) {
        long startTime = System.currentTimeMillis();

        String repoIdentifier = pullRequest.getProject() != null
                ? pullRequest.getProject().getName()
                : "unknown";
        log.debug("Starting Code Graph Impact analysis for PR {}/{}",
                repoIdentifier, pullRequest.getPrNumber());

        PassResult result = new PassResult();
        result.setPassName("Code Graph Impact Analysis");
        result.setSuccess(true);
        result.setIssues(new ArrayList<>());

        Map<String, Object> metadata = new HashMap<>();
        result.setMetadata(metadata);

        if (pullRequest.getProject() == null || pullRequest.getProject().getId() == null) {
            log.debug("PR has no project; skipping impact analysis");
            result.setDurationMs(System.currentTimeMillis() - startTime);
            return result;
        }

        Long projectId = pullRequest.getProject().getId();
        List<String> changedFiles = diffs.stream()
                .map(GitHubService.FileDiff::filename)
                .filter(s -> s != null && !s.isBlank())
                .toList();

        if (changedFiles.isEmpty()) {
            result.setDurationMs(System.currentTimeMillis() - startTime);
            return result;
        }

        try {
            BlastRadiusResult blast = blastRadiusService.computeBlastRadius(
                    projectId, changedFiles, BlastRadiusOptions.defaults());

            metadata.put(METADATA_BLAST_RADIUS, blast);
            metadata.put("filesAnalyzed", changedFiles.size());
            metadata.put("seedCount", blast.seedCount());
            metadata.put("impactedNodes", blast.impactedNodes().size());
            metadata.put("impactedFiles", blast.impactedFiles().size());
            metadata.put("truncated", blast.truncated());

            List<ImpactedNode> highRisk = blast.impactedNodes().stream()
                    .filter(n -> n.riskScore() >= IMPACT_ISSUE_THRESHOLD)
                    .toList();

            if (!highRisk.isEmpty()) {
                Issue issue = createAggregatedIssue(blast, highRisk, review, changedFiles);
                result.getIssues().add(issue);
            }

            log.debug("Impact pass completed: {} impacted nodes, {} high-risk, {} files",
                    blast.impactedNodes().size(), highRisk.size(), blast.impactedFiles().size());

        } catch (Exception e) {
            log.warn("Impact analysis failed: {}", e.getMessage(), e);
        }

        result.setDurationMs(System.currentTimeMillis() - startTime);
        return result;
    }

    private Issue createAggregatedIssue(BlastRadiusResult blast, List<ImpactedNode> highRisk,
                                        Review review, List<String> changedFiles) {
        ImpactedNode top = highRisk.get(0);
        Severity severity = severityFromRiskScore(top.riskScore());

        // Se o top tem confidence baixa (caminho dominado por arestas AMBIGUOUS),
        // rebaixa um nível para evitar ruído de cross-language/late binding.
        if (top.propagatedConfidence() < 0.5 && severity == Severity.CRITICAL) {
            severity = Severity.HIGH;
        }

        String anchorFile = changedFiles.get(0);

        return Issue.builder()
                .review(review)
                .type(IssueType.CODE_SMELL)
                .severity(severity)
                .title(buildTitle(blast, highRisk))
                .description(buildDescription(blast, highRisk))
                .filePath(anchorFile)
                .lineStart(1)
                .lineEnd(1)
                .ruleId("CODE_GRAPH_IMPACT_V2")
                .source(IssueSource.LLM)
                .build();
    }

    private static Severity severityFromRiskScore(double riskScore) {
        if (riskScore >= 0.75) return Severity.CRITICAL;
        if (riskScore >= 0.55) return Severity.HIGH;
        if (riskScore >= 0.35) return Severity.MEDIUM;
        return Severity.LOW;
    }

    private String buildTitle(BlastRadiusResult blast, List<ImpactedNode> highRisk) {
        return String.format("Blast radius: %d high-risk impact%s across %d file%s",
                highRisk.size(), highRisk.size() == 1 ? "" : "s",
                blast.impactedFiles().size(), blast.impactedFiles().size() == 1 ? "" : "s");
    }

    private String buildDescription(BlastRadiusResult blast, List<ImpactedNode> highRisk) {
        StringBuilder sb = new StringBuilder();
        sb.append("This change has potential downstream impact:\n\n");
        sb.append("- **Seeds analysed**: ").append(blast.seedCount()).append("\n");
        sb.append("- **Nodes reached**: ").append(blast.impactedNodes().size()).append("\n");
        sb.append("- **Files affected**: ").append(blast.impactedFiles().size()).append("\n");
        if (blast.truncated()) {
            sb.append("- _Result was truncated; consider narrowing the change_\n");
        }
        sb.append("\n**Top high-risk impacted symbols**:\n");

        int limit = Math.min(TOP_N_IN_DESCRIPTION, highRisk.size());
        for (int i = 0; i < limit; i++) {
            ImpactedNode node = highRisk.get(i);
            sb.append(String.format("- `%s` (depth=%d, risk=%.2f, confidence=%.2f)",
                    node.qualifiedName(), node.depth(),
                    node.riskScore(), node.propagatedConfidence()));
            if (node.filePath() != null) {
                sb.append(" — ").append(node.filePath());
            }
            sb.append("\n");
        }
        if (highRisk.size() > limit) {
            sb.append("- … and ").append(highRisk.size() - limit).append(" more\n");
        }
        return sb.toString();
    }
}
