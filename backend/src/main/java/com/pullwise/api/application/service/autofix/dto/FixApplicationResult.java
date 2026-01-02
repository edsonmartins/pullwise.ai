package com.pullwise.api.application.service.autofix.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado da aplicação de correção.
 */
public record FixApplicationResult(
        /**
         * Se a aplicação foi bem-sucedida.
         */
        boolean success,

        /**
         * Hash do commit criado (se houver).
         */
        String commitHash,

        /**
         * Branch onde foi aplicado.
         */
        String branchName,

        /**
         * Arquivos modificados.
         */
        List<String> modifiedFiles,

        /**
         * Mensagem de erro (se falhou).
         */
        String errorMessage,

        /**
         * Timestamp da aplicação.
         */
        LocalDateTime appliedAt
) {
    public static FixApplicationResult success(String commitHash, String branchName, List<String> modifiedFiles) {
        return new FixApplicationResult(
                true,
                commitHash,
                branchName,
                modifiedFiles,
                null,
                LocalDateTime.now()
        );
    }

    public static FixApplicationResult failed(String errorMessage) {
        return new FixApplicationResult(
                false,
                null,
                null,
                List.of(),
                errorMessage,
                LocalDateTime.now()
        );
    }
}
