package com.pullwise.api.application.service.autofix.dto;

import java.util.List;

/**
 * Resultado da validação de código gerado.
 */
public record CodeValidationResult(
        /**
         * Se o código passou na validação.
         */
        boolean isValid,

        /**
         * Lista de problemas encontrados.
         */
        List<String> issues,

        /**
         * Nível de severidade do pior problema.
         */
        Severity maxSeverity

) {
    public static CodeValidationResult ofValid() {
        return new CodeValidationResult(true, List.of(), Severity.NONE);
    }

    public static CodeValidationResult ofInvalid(List<String> issues) {
        Severity max = issues.stream()
                .map(Severity::fromMessage)
                .max(Enum::compareTo)
                .orElse(Severity.MEDIUM);

        return new CodeValidationResult(false, issues, max);
    }

    public boolean valid() {
        return isValid;
    }

    public enum Severity {
        NONE,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL;

        public static Severity fromMessage(String message) {
            String lower = message.toLowerCase();

            if (lower.contains("critical") || lower.contains("security") || lower.contains("injection")) {
                return CRITICAL;
            }
            if (lower.contains("error") || lower.contains("fail")) {
                return HIGH;
            }
            if (lower.contains("warning")) {
                return MEDIUM;
            }
            return LOW;
        }
    }
}
