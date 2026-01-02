package com.pullwise.api.domain.enums;

import lombok.Getter;

/**
 * Status de uma sugestão de correção.
 */
@Getter
public enum FixStatus {
    /**
     * Sugestão criada, pendente de revisão.
     */
    PENDING("pending", "Pendente", 1),

    /**
     * Sugestão aprovada, pronta para aplicação.
     */
    APPROVED("approved", "Aprovada", 2),

    /**
     * Sugestão rejeitada pelo usuário.
     */
    REJECTED("rejected", "Rejeitada", 3),

    /**
     * Sugestão aplicada com sucesso.
     */
    APPLIED("applied", "Aplicada", 4),

    /**
     * Aplicação falhou.
     */
    FAILED("failed", "Falhou", 5),

    /**
     * Sugestão expirada (PR foi mergeado).
     */
    EXPIRED("expired", "Expirada", 6);

    private final String code;
    private final String displayName;
    private final int order;

    FixStatus(String code, String displayName, int order) {
        this.code = code;
        this.displayName = displayName;
        this.order = order;
    }

    public boolean isFinalState() {
        return this == APPLIED || this == FAILED || this == EXPIRED || this == REJECTED;
    }

    public boolean canApply() {
        return this == APPROVED;
    }
}
