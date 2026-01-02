package com.pullwise.api.application.service.sandbox.dto;

import java.time.Duration;
import java.util.Map;

/**
 * Resultado da execução em sandbox.
 */
public record SandboxExecutionResult(
        /**
         * Se a execução foi bem-sucedida.
         */
        boolean success,

        /**
         * Output padrão (stdout).
         */
        String output,

        /**
         * Output de erro (stderr).
         */
        String error,

        /**
         * Código de saída.
         */
        int exitCode,

        /**
         * Duração da execução.
         */
        Duration duration,

        /**
         * Metadados adicionais.
         */
        Map<String, Object> metadata
) {
    public static SandboxExecutionResult success(
            String output,
            String error,
            int exitCode,
            Duration duration,
            Map<String, Object> metadata
    ) {
        return new SandboxExecutionResult(
                exitCode == 0,
                output != null ? output : "",
                error != null ? error : "",
                exitCode,
                duration,
                metadata != null ? metadata : Map.of()
        );
    }

    public static SandboxExecutionResult failed(String errorMessage, Duration duration) {
        return new SandboxExecutionResult(
                false,
                "",
                errorMessage,
                1,
                duration,
                Map.of()
        );
    }

    /**
     * Retorna a mensagem de erro formatada.
     */
    public String error() {
        return error != null && !error.isBlank() ? error : "No error";
    }
}
