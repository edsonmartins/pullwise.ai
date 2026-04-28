package com.pullwise.api.application.service.graph.blast;

import java.util.Map;

/**
 * Nó alcançado pela BFS de blast-radius.
 *
 * @param qualifiedName        qualified name do nó atingido
 * @param filePath             path do arquivo (pode ser null se não resolvido)
 * @param depth                distância em hops desde o seed mais próximo
 * @param propagatedConfidence produto dos pesos de aresta no caminho mais "forte" (0..1)
 * @param riskScore            score 0..1 calculado pelo {@link RiskScoringService}
 * @param riskBreakdown        contribuição individual de cada dimensão
 */
public record ImpactedNode(
        String qualifiedName,
        String filePath,
        int depth,
        double propagatedConfidence,
        double riskScore,
        Map<String, Double> riskBreakdown
) {
}
