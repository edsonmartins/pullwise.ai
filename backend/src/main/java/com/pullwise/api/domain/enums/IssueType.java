package com.pullwise.api.domain.enums;

import lombok.Getter;

/**
 * Categorias de problemas que podem ser encontrados no código.
 */
@Getter
public enum IssueType {
    /**
     * Defeito ou bug no código que pode causar comportamento incorreto.
     */
    BUG("bug", "Bug"),

    /**
     * Vulnerabilidade de segurança.
     */
    VULNERABILITY("vulnerability", "Vulnerabilidade"),

    /**
     * Problema relacionado à qualidade e manutenibilidade do código.
     * Ex: complexidade ciclomática alta, duplicação, código morto.
     */
    CODE_SMELL("code_smell", "Code Smell"),

    /**
     * Problema na lógica de negócio ou fluxo do código.
     */
    LOGIC("logic", "Lógica"),

    /**
     * Problema relacionado a performance e otimização.
     */
    PERFORMANCE("performance", "Performance"),

    /**
     * Problema relacionado à segurança do código.
     */
    SECURITY("security", "Segurança"),

    /**
     * Problema relacionado a estilo e formatação do código.
     */
    STYLE("style", "Estilo"),

    /**
     * Sugestão de melhoria ou refatoração.
     */
    SUGGESTION("suggestion", "Sugestão"),

    /**
     * Problema relacionado a testes e cobertura.
     */
    TEST("test", "Teste"),

    /**
     * Problema relacionado à documentação.
     */
    DOCUMENTATION("documentation", "Documentação");

    private final String code;
    private final String label;

    IssueType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static IssueType fromCode(String code) {
        for (IssueType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown issue type: " + code);
    }
}
