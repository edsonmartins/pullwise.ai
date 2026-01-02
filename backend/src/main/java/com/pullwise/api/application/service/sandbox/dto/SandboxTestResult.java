package com.pullwise.api.application.service.sandbox.dto;

import java.time.Duration;

/**
 * Resultado da execução de testes em sandbox.
 */
public record SandboxTestResult(
        /**
         * Número de testes que passaram.
         */
        int passed,

        /**
         * Número de testes que falharam.
         */
        int failed,

        /**
         * Output completo da execução.
         */
        String output,

        /**
         * Duração total dos testes.
         */
        Duration duration,

        /**
         * Erro se houve falha na execução.
         */
        String error
) {
    public static SandboxTestResult success(int passed, int failed, String output, Duration duration) {
        return new SandboxTestResult(passed, failed, output, duration, null);
    }

    public static SandboxTestResult failed(String errorMessage) {
        return new SandboxTestResult(0, 0, "", Duration.ZERO, errorMessage);
    }

    /**
     * Total de testes executados.
     */
    public int total() {
        return passed + failed;
    }

    /**
     * Se todos os testes passaram.
     */
    public boolean allPassed() {
        return failed == 0 && passed > 0;
    }

    /**
     * Taxa de sucesso (0.0 a 1.0).
     */
    public double successRate() {
        int total = total();
        return total > 0 ? (double) passed / total : 0.0;
    }
}
