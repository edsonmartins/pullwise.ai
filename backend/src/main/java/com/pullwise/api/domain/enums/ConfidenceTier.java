package com.pullwise.api.domain.enums;

import lombok.Getter;

/**
 * Nível de confiança de uma aresta no grafo de código.
 *
 * <p>O peso é multiplicado ao longo do caminho na BFS forward, atenuando
 * o risk score de nós alcançados via heurísticas frágeis (cross-language,
 * late binding, reflexão).
 */
@Getter
public enum ConfidenceTier {

    /** Aresta resolvida pelo AST com 100% de certeza. */
    EXTRACTED(1.0),

    /** Aresta resolvida por heurística (simple-name match dentro do mesmo projeto). */
    INFERRED(0.7),

    /** Aresta sem resolução clara (cross-language, reflexão, late binding). */
    AMBIGUOUS(0.4);

    private final double weight;

    ConfidenceTier(double weight) {
        this.weight = weight;
    }
}
