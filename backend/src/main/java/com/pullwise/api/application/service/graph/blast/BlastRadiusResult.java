package com.pullwise.api.application.service.graph.blast;

import java.util.List;
import java.util.Set;

/**
 * Resultado de um cálculo de blast-radius.
 *
 * @param impactedNodes  nós alcançados ordenados por (depth ASC, confidence DESC)
 * @param impactedFiles  paths únicos dos arquivos atingidos (derivado de impactedNodes)
 * @param truncated      true se a busca atingiu o cap {@code maxNodes}
 * @param seedCount      número de seeds que iniciaram a BFS
 */
public record BlastRadiusResult(
        List<ImpactedNode> impactedNodes,
        Set<String> impactedFiles,
        boolean truncated,
        int seedCount
) {
    public static BlastRadiusResult empty() {
        return new BlastRadiusResult(List.of(), Set.of(), false, 0);
    }
}
