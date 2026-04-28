package com.pullwise.api.application.service.graph.blast;

import com.pullwise.api.domain.enums.EdgeKind;

import java.util.EnumSet;
import java.util.Set;

/**
 * Opções de execução do {@link BlastRadiusService}.
 *
 * @param maxDepth    profundidade máxima da BFS (1 a {@link #DEPTH_HARD_CAP})
 * @param maxNodes    limite de nós retornados (truncated=true se excedido)
 * @param kinds       tipos de aresta percorridos pela BFS
 */
public record BlastRadiusOptions(int maxDepth, int maxNodes, Set<EdgeKind> kinds) {

    public static final int DEFAULT_DEPTH = 2;
    public static final int DEFAULT_MAX_NODES = 500;
    public static final int DEPTH_HARD_CAP = 15;
    public static final int NODES_HARD_CAP = 2000;

    /** Arestas usadas na BFS forward por padrão (impacto direto). */
    public static final Set<EdgeKind> DEFAULT_KINDS = EnumSet.of(
            EdgeKind.CALLS, EdgeKind.IMPORTS_FROM, EdgeKind.INHERITS
    );

    public BlastRadiusOptions {
        if (maxDepth < 1) maxDepth = 1;
        if (maxDepth > DEPTH_HARD_CAP) maxDepth = DEPTH_HARD_CAP;
        if (maxNodes < 1) maxNodes = 1;
        if (maxNodes > NODES_HARD_CAP) maxNodes = NODES_HARD_CAP;
        if (kinds == null || kinds.isEmpty()) kinds = DEFAULT_KINDS;
    }

    public static BlastRadiusOptions defaults() {
        return new BlastRadiusOptions(DEFAULT_DEPTH, DEFAULT_MAX_NODES, DEFAULT_KINDS);
    }
}
