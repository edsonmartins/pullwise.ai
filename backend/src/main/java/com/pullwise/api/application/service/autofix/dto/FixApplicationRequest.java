package com.pullwise.api.application.service.autofix.dto;

import com.pullwise.api.domain.model.FixSuggestion;

/**
 * Request para aplicação de correção.
 */
public record FixApplicationRequest(
        /**
         * Sugestão a ser aplicada.
         */
        FixSuggestion suggestion,

        /**
         * Usuário que está aplicando.
         */
        String appliedBy,

        /**
         * Se deve criar commit após aplicar.
         */
        boolean createCommit,

        /**
         * Se deve fazer push para remoto.
         */
        boolean push,

        /**
         * Token de autenticação para push.
         */
        String authToken
) {
    public static FixApplicationRequest autoApply(FixSuggestion suggestion, String authToken) {
        return new FixApplicationRequest(
                suggestion,
                "pullwise-auto-fix",
                true,
                true,
                authToken
        );
    }

    public static FixApplicationRequest manualApply(FixSuggestion suggestion, String username) {
        return new FixApplicationRequest(
                suggestion,
                username,
                false,
                false,
                null
        );
    }
}
