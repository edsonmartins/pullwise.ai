package com.pullwise.api.application.service.autofix.dto;

import com.pullwise.api.domain.model.Issue;

/**
 * Request para geração de sugestão de correção.
 */
public record FixGenerationRequest(
        /**
         * Issue que será corrigida.
         */
        Issue issue,

        /**
         * Código completo do arquivo (para contexto).
         */
        String fileContent,

        /**
         * Branch de trabalho.
         */
        String branchName,

        /**
         * Timeout em segundos para geração.
         */
        int timeoutSeconds,

        /**
         * Modelo LLM a ser usado (opcional).
         * Se não especificado, o router escolhe.
         */
        String preferredModel
) {
    public FixGenerationRequest {
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 60;
        }
    }

    public static FixGenerationRequest of(Issue issue, String fileContent, String branchName) {
        return new FixGenerationRequest(issue, fileContent, branchName, 60, null);
    }
}
