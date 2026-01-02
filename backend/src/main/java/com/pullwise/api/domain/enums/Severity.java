package com.pullwise.api.domain.enums;

import lombok.Getter;

/**
 * Níveis de severidade para issues encontradas no código.
 */
@Getter
public enum Severity {
    /**
     * Vulnerabilidade crítica que compromete a segurança do sistema.
     * Ex: SQL Injection, exposição de credenciais, falhas de autenticação.
     */
    CRITICAL(1, "Crítico", "#DC2626"),

    /**
     * Problema de alta prioridade que pode causar bugs em produção.
     * Ex: Memory leak, race condition, resource leak.
     */
    HIGH(2, "Alto", "#EA580C"),

    /**
     * Problema de média prioridade que afeta manutenibilidade.
     * Ex: Code duplication, complexidade alta, naming ruim.
     */
    MEDIUM(3, "Médio", "#F59E0B"),

    /**
     * Sugestão de melhoria de baixa prioridade.
     * Ex: Style, otimizações não críticas, boas práticas.
     */
    LOW(4, "Baixo", "#10B981"),

    /**
     * Apenas informativo, não representa um problema.
     * Ex: Documentação, observações sobre o código.
     */
    INFO(5, "Informativo", "#6B7280");

    private final int level;
    private final String label;
    private final String color;

    Severity(int level, String label, String color) {
        this.level = level;
        this.label = label;
        this.color = color;
    }

    public static Severity fromLevel(int level) {
        for (Severity severity : values()) {
            if (severity.level == level) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Unknown severity level: " + level);
    }

    public boolean isHigherThan(Severity other) {
        return this.level < other.level;
    }
}
