package com.pullwise.api.application.service.plugin.api;

import lombok.Getter;

/**
 * Tipos de plugins suportados pelo Pullwise.ai.
 */
@Getter
public enum PluginType {
    /**
     * Análise estática de código (SAST).
     */
    SAST("sast", "Static Application Security Testing"),

    /**
     * Linter e análise de estilo.
     */
    LINTER("linter", "Code Style Linter"),

    /**
     * Análise de segurança.
     */
    SECURITY("security", "Security Vulnerability Scanner"),

    /**
     * Análise de performance.
     */
    PERFORMANCE("performance", "Performance Analysis"),

    /**
     * Plugin com LLM customizado.
     */
    CUSTOM_LLM("custom_llm", "Custom LLM Model"),

    /**
     * Integrações externas.
     */
    INTEGRATION("integration", "External Integration");

    private final String code;
    private final String displayName;

    PluginType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static PluginType fromCode(String code) {
        for (PluginType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown plugin type: " + code);
    }
}
