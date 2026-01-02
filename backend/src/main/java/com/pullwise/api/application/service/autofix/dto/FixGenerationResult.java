package com.pullwise.api.application.service.autofix.dto;

import com.pullwise.api.domain.enums.FixConfidence;

import java.util.List;

/**
 * Resultado da geração de correção.
 */
public record FixGenerationResult(
        /**
         * Código corrigido.
         */
        String fixedCode,

        /**
         * Código original.
         */
        String originalCode,

        /**
         * Explicação da correção.
         */
        String explanation,

        /**
         * Nível de confiança.
         */
        FixConfidence confidence,

        /**
         * Se a correção pode ser aplicada automaticamente.
         */
        boolean canAutoApply,

        /**
         * Razões pelas quais não pode aplicar automaticamente.
         */
        List<String> blockingReasons,

        /**
         * Modelo usado para geração.
         */
        String modelUsed,

        /**
         * Tokens de entrada.
         */
        int inputTokens,

        /**
         * Tokens de saída.
         */
        int outputTokens,

        /**
         * Custo estimado em USD.
         */
        double cost
) {
    public static FixGenerationResult highConfidence(
            String fixedCode,
            String originalCode,
            String explanation,
            String modelUsed,
            int inputTokens,
            int outputTokens,
            double cost
    ) {
        return new FixGenerationResult(
                fixedCode,
                originalCode,
                explanation,
                FixConfidence.HIGH,
                true,
                List.of(),
                modelUsed,
                inputTokens,
                outputTokens,
                cost
        );
    }

    public static FixGenerationResult mediumConfidence(
            String fixedCode,
            String originalCode,
            String explanation,
            String modelUsed,
            int inputTokens,
            int outputTokens,
            double cost
    ) {
        return new FixGenerationResult(
                fixedCode,
                originalCode,
                explanation,
                FixConfidence.MEDIUM,
                false,
                List.of("Confidence level requires manual review"),
                modelUsed,
                inputTokens,
                outputTokens,
                cost
        );
    }

    public static FixGenerationResult lowConfidence(
            String explanation,
            String modelUsed
    ) {
        return new FixGenerationResult(
                null,
                null,
                explanation,
                FixConfidence.LOW,
                false,
                List.of("Low confidence - manual review required"),
                modelUsed,
                0,
                0,
                0.0
        );
    }

    public static FixGenerationResult failed(String error) {
        return new FixGenerationResult(
                null,
                null,
                "Failed to generate fix: " + error,
                FixConfidence.LOW,
                false,
                List.of(error),
                null,
                0,
                0,
                0.0
        );
    }
}
