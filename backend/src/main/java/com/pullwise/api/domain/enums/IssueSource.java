package com.pullwise.api.domain.enums;

import lombok.Getter;

/**
 * Fontes de onde as issues podem ser originadas.
 */
@Getter
public enum IssueSource {
    /**
     * Análise estática via SonarQube.
     */
    SONARQUBE("sonarqube", "SonarQube"),

    /**
     * Análise estática via Checkstyle.
     */
    CHECKSTYLE("checkstyle", "Checkstyle"),

    /**
     * Análise estática via PMD.
     */
    PMD("pmd", "PMD"),

    /**
     * Análise estática via SpotBugs.
     */
    SPOTBUGS("spotbugs", "SpotBugs"),

    /**
     * Análise via LLM (Large Language Model).
     */
    LLM("llm", "LLM"),

    /**
     * Análise personalizada/customizada.
     */
    CUSTOM("custom", "Custom");

    private final String code;
    private final String displayName;

    IssueSource(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public boolean isSastTool() {
        return this == SONARQUBE || this == CHECKSTYLE || this == PMD || this == SPOTBUGS;
    }

    public boolean isAI() {
        return this == LLM;
    }

    public static IssueSource fromCode(String code) {
        for (IssueSource source : values()) {
            if (source.code.equalsIgnoreCase(code)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown issue source: " + code);
    }
}
