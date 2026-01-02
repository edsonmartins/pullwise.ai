package com.pullwise.api.domain.enums;

import lombok.Getter;

/**
 * Nível de confiança na sugestão de correção automática.
 */
@Getter
public enum FixConfidence {
    /**
     * Correção totalmente segura, pode ser aplicada automaticamente.
     */
    HIGH("high", "Alta", 1.0),

    /**
     * Correção provavelmente correta, requer revisão rápida.
     */
    MEDIUM("medium", "Média", 0.7),

    /**
     * Correção incerta, requer revisão manual completa.
     */
    LOW("low", "Baixa", 0.4);

    private final String code;
    private final String displayName;
    private final double threshold;

    FixConfidence(String code, String displayName, double threshold) {
        this.code = code;
        this.displayName = displayName;
        this.threshold = threshold;
    }

    public static FixConfidence fromThreshold(double score) {
        if (score >= HIGH.threshold) {
            return HIGH;
        } else if (score >= MEDIUM.threshold) {
            return MEDIUM;
        } else {
            return LOW;
        }
    }

    public boolean canAutoApply() {
        return this == HIGH;
    }
}
