package com.pullwise.api.domain.enums;

import lombok.Getter;

/**
 * Tipos de tarefas de review que podem ser roteadas para diferentes modelos LLM.
 *
 * <p>Cada tipo de tarefa tem características específicas que influenciam a escolha do modelo:
 * - complexidade exigida
 * - tamanho do contexto necessário
 * - tolerância a latência
 * - custo por token
 *
 * @see com.pullwise.api.application.service.llm.MultiModelLLMRouter
 */
@Getter
public enum ReviewTaskType {

    /**
     * Detecção complexa de bugs que requer raciocínio profundo.
     * Modelo recomendado: o3-mini (maior capacidade de reasoning)
     */
    COMPLEX_REASONING(
        "Complex Reasoning",
        "Análise profunda de lógica complexa, edge cases e raciocínio multi-step",
        4, // prioridade de qualidade
        3  // latência esperada (1-5)
    ),

    /**
     * Detecção de bugs através de análise estática + LLM.
     * Modelo recomendado: o3-mini ou Claude 3.5
     */
    BUG_DETECTION(
        "Bug Detection",
        "Identificação de bugs potenciais, null pointer exceptions, race conditions",
        5,
        3
    ),

    /**
     * Análise de arquitetura e design patterns.
     * Modelo recomendado: Claude 3.5 Sonnet (forte em arquitetura)
     */
    ARCHITECTURE_REVIEW(
        "Architecture Review",
        "Análise de design patterns, princípios SOLID, coesão e acoplamento",
        4,
        3
    ),

    /**
     * Análise de segurança (OWASP Top 10).
     * Modelo recomendado: Claude 3.5 Sonnet (melhor em segurança)
     */
    SECURITY_ANALYSIS(
        "Security Analysis",
        "Vulnerabilidades de segurança, SQL injection, XSS, autenticação/autorização",
        5,
        3
    ),

    /**
     * Refatoração de código.
     * Modelo recomendado: o3-mini (sugestões de refatoração inteligentes)
     */
    REFACTORING(
        "Refactoring",
        "Sugestões de refatoração, código limpo, melhorias de performance",
        4,
        3
    ),

    /**
     * Geração de sumários executivos.
     * Modelo recomendado: GPT-4.1-turbo (rápido e econômico)
     */
    SUMMARIZATION(
        "Summarization",
        "Geração de resumos executivos dos reviews",
        2,
        1
    ),

    /**
     * perguntas e respostas sobre o código.
     * Modelo recomendado: GPT-4.1-turbo ou Claude 3.5
     */
    QA(
        "Q&A",
        "Resposta a perguntas específicas sobre o código",
        3,
        2
    ),

    /**
     * Geração de metadados (docstrings, comentários).
     * Modelo recomendado: GPT-4.1-turbo (econômico)
     */
    METADATA_GENERATION(
        "Metadata Generation",
        "Geração de docstrings, comentários, Javadoc",
        2,
        1
    ),

    /**
     * Análise rápida de estilo e convenções.
     * Modelo recomendado: Gemma 3 4B local (mais rápido)
     */
    FAST_LINT(
        "Fast Lint",
        "Verificação rápida de estilo e convenções de código",
        1,
        1
    ),

    /**
     * Verificação de style guide.
     * Modelo recomendado: Gemma 3 4B local
     */
    STYLE_CHECK(
        "Style Check",
        "Verificação de conformidade com style guide e convenções",
        1,
        1
    ),

    /**
     * Pré-filtragem de código antes da análise completa.
     * Modelo recomendado: Gemma 3 4B local
     */
    PRE_FILTER(
        "Pre-filter",
        "Análise inicial para identificar áreas que merecem análise profunda",
        1,
        1
    ),

    /**
     * Explicação de código.
     * Modelo recomendado: Claude 3.5 Sonnet (melhor em explicações)
     */
    CODE_EXPLANATION(
        "Code Explanation",
        "Explicação detalhada de como o código funciona",
        3,
        2
    );

    private final String displayName;
    private final String description;
    private final int qualityPriority;  // 1-5, maior = precisa de modelo mais capaz
    private final int expectedLatency;   // 1-5, maior = mais lento

    ReviewTaskType(String displayName, String description, int qualityPriority, int expectedLatency) {
        this.displayName = displayName;
        this.description = description;
        this.qualityPriority = qualityPriority;
        this.expectedLatency = expectedLatency;
    }

    /**
     * Determina se esta tarefa requer um modelo de alta capacidade.
     */
    public boolean requiresHighCapability() {
        return qualityPriority >= 4;
    }

    /**
     * Determina se esta tarefa pode usar um modelo local (menos preciso).
     */
    public boolean canUseLocalModel() {
        return qualityPriority <= 2;
    }

    /**
     * Retorna o custo máximo aceitável por 1K tokens para esta tarefa.
     */
    public double maxCostPer1kTokens() {
        return switch (this) {
            case COMPLEX_REASONING, BUG_DETECTION, SECURITY_ANALYSIS -> 0.005;
            case ARCHITECTURE_REVIEW, REFACTORING -> 0.003;
            case SUMMARIZATION, METADATA_GENERATION -> 0.002;
            case FAST_LINT, STYLE_CHECK, PRE_FILTER -> 0.0; // local model
            default -> 0.003;
        };
    }
}
