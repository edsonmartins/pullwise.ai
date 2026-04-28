package com.pullwise.api.application.service.review.pipeline.synthesis;

import com.pullwise.api.application.service.graph.blast.BlastRadiusResult;
import com.pullwise.api.application.service.graph.blast.ImpactedNode;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.model.Issue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Consolida o {@link BlastRadiusResult} no conjunto de issues de todas as
 * passadas: issues SAST/LLM/Security cujo arquivo cai no blast-radius do PR
 * têm a severity promovida em um nível (HIGH→CRITICAL, MEDIUM→HIGH, LOW→MEDIUM).
 *
 * <p>Trabalha por mutação para preservar o id/relacionamentos do issue.
 */
@Slf4j
@Component
public class BlastRadiusConsolidator {

    /**
     * Promove a severity de cada issue cujo {@code filePath} está em
     * {@link BlastRadiusResult#impactedFiles()}.
     *
     * @return número de issues que tiveram severity promovida
     */
    public int consolidate(List<Issue> issues, BlastRadiusResult blast) {
        if (issues == null || issues.isEmpty() || blast == null || blast.impactedFiles().isEmpty()) {
            return 0;
        }

        Set<String> impactedFiles = blast.impactedFiles();

        // Mapa filePath → max riskScore dos nós nesse arquivo (para evitar promover
        // por arestas AMBIGUOUS de baixa relevância)
        Map<String, Double> maxRiskByFile = new HashMap<>();
        for (ImpactedNode node : blast.impactedNodes()) {
            if (node.filePath() == null) continue;
            maxRiskByFile.merge(node.filePath(), node.riskScore(), Math::max);
        }

        int promoted = 0;
        for (Issue issue : issues) {
            String path = issue.getFilePath();
            if (path == null || path.isBlank()) continue;
            if (!impactedFiles.contains(path)) continue;

            // Só promove se o file tem nó atingido com risk minimamente relevante.
            double maxRisk = maxRiskByFile.getOrDefault(path, 0.0);
            if (maxRisk < 0.35) continue;

            Severity current = issue.getSeverity();
            Severity promotedSev = promote(current);
            if (promotedSev != current) {
                issue.setSeverity(promotedSev);
                promoted++;
            }
        }

        if (promoted > 0) {
            log.debug("Consolidator promoted severity of {} issue(s) based on blast radius", promoted);
        }
        return promoted;
    }

    /**
     * Eleva uma severity em um nível. CRITICAL e INFO são imutáveis (CRITICAL já
     * é o teto; INFO é meramente informativo e não deve "subir" automaticamente).
     */
    private Severity promote(Severity severity) {
        if (severity == null) return null;
        return switch (severity) {
            case LOW -> Severity.MEDIUM;
            case MEDIUM -> Severity.HIGH;
            case HIGH -> Severity.CRITICAL;
            case CRITICAL, INFO -> severity;
        };
    }
}
