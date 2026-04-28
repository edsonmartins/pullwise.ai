package com.pullwise.api.application.service.review.pipeline.synthesis;

import com.pullwise.api.application.service.graph.blast.BlastRadiusResult;
import com.pullwise.api.application.service.graph.blast.ImpactedNode;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.model.Issue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ordena issues por uma prioridade composta:
 *
 * <pre>priority = severity_weight * 0.6 + risk_score_of_file * 0.4</pre>
 *
 * <p>{@code severity_weight} é normalizado em [0..1] (CRITICAL=1.0, INFO=0.0).
 * {@code risk_score_of_file} é o {@code max(riskScore)} de qualquer nó do arquivo
 * dentro do {@link BlastRadiusResult}; 0.0 se o arquivo não está no blast.
 *
 * <p>A ordenação é estável: o issue com menor índice original vence o desempate.
 */
@Component
public class IssuePrioritizer {

    public List<Issue> prioritize(List<Issue> issues, BlastRadiusResult blast) {
        if (issues == null || issues.isEmpty()) return List.of();

        Map<String, Double> maxRiskByFile = blastRiskByFile(blast);

        // Decora cada issue com prioridade preservando ordem original (estabilidade)
        record Decorated(int originalIndex, Issue issue, double priority) {}
        List<Decorated> decorated = new ArrayList<>(issues.size());
        for (int i = 0; i < issues.size(); i++) {
            Issue issue = issues.get(i);
            double sev = severityWeight(issue.getSeverity());
            double risk = maxRiskByFile.getOrDefault(issue.getFilePath(), 0.0);
            double priority = sev * 0.6 + risk * 0.4;
            decorated.add(new Decorated(i, issue, priority));
        }

        decorated.sort(Comparator
                .comparingDouble(Decorated::priority).reversed()
                .thenComparingInt(Decorated::originalIndex));

        List<Issue> sorted = new ArrayList<>(decorated.size());
        for (Decorated d : decorated) sorted.add(d.issue());
        return sorted;
    }

    private Map<String, Double> blastRiskByFile(BlastRadiusResult blast) {
        if (blast == null || blast.impactedNodes().isEmpty()) return Map.of();
        Map<String, Double> out = new HashMap<>();
        for (ImpactedNode node : blast.impactedNodes()) {
            if (node.filePath() == null) continue;
            out.merge(node.filePath(), node.riskScore(), Math::max);
        }
        return out;
    }

    private double severityWeight(Severity severity) {
        if (severity == null) return 0.0;
        return switch (severity) {
            case CRITICAL -> 1.0;
            case HIGH -> 0.75;
            case MEDIUM -> 0.5;
            case LOW -> 0.25;
            case INFO -> 0.0;
        };
    }
}
