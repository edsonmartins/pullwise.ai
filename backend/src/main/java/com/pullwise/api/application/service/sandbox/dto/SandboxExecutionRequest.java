package com.pullwise.api.application.service.sandbox.dto;

/**
 * Request para execução em sandbox.
 */
public record SandboxExecutionRequest(
        /**
         * Código a ser executado.
         */
        String code,

        /**
         * Linguagem de programação.
         */
        String language,

        /**
         * Input para o programa (stdin).
         */
        String input,

        /**
         * Se permite acesso ao sistema de arquivos.
         */
        boolean allowFileSystem,

        /**
         * Limite de memória em MB.
         */
        int memoryLimitMb
) {
    public SandboxExecutionRequest {
        if (memoryLimitMb <= 0) {
            memoryLimitMb = 256;
        }
    }

    public static SandboxExecutionRequest of(String code, String language) {
        return new SandboxExecutionRequest(code, language, null, false, 256);
    }

    public static SandboxExecutionRequest withInput(String code, String language, String input) {
        return new SandboxExecutionRequest(code, language, input, false, 256);
    }
}
