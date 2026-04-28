package com.pullwise.api.domain.enums;

/**
 * Tipo de aresta no grafo de código (Code Graph v2 / Blast-Radius).
 *
 * <p>CALLS, IMPORTS_FROM e INHERITS são as direções percorridas pela
 * BFS forward de blast-radius. CONTAINS é estrutural (file → classe → método).
 */
public enum EdgeKind {
    CALLS,
    IMPORTS_FROM,
    INHERITS,
    IMPLEMENTS,
    REFERENCES,
    CONTAINS
}
