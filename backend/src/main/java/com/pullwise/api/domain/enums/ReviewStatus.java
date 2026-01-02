package com.pullwise.api.domain.enums;

import lombok.Getter;

/**
 * Status possíveis de um review de pull request.
 */
@Getter
public enum ReviewStatus {
    /**
     * Review criado e aguardando processamento.
     */
    PENDING("pending", "Pendente"),

    /**
     * Review em processamento (análises em andamento).
     */
    IN_PROGRESS("in_progress", "Em Progresso"),

    /**
     * Review completado com sucesso.
     */
    COMPLETED("completed", "Completado"),

    /**
     * Review falhou devido a erro no processamento.
     */
    FAILED("failed", "Falhou"),

    /**
     * Review cancelado pelo usuário ou sistema.
     */
    CANCELLED("cancelled", "Cancelado"),

    /**
     * Review aguardando aprovação manual.
     */
    AWAITING_APPROVAL("awaiting_approval", "Aguardando Aprovação");

    private final String code;
    private final String label;

    ReviewStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    public boolean isActive() {
        return this == PENDING || this == IN_PROGRESS || this == AWAITING_APPROVAL;
    }

    public static ReviewStatus fromCode(String code) {
        for (ReviewStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown review status: " + code);
    }
}
