package com.pullwise.api.application.service.sandbox;

import com.pullwise.api.application.service.sandbox.dto.SandboxExecutionRequest;
import com.pullwise.api.application.service.sandbox.dto.SandboxExecutionResult;
import com.pullwise.api.application.service.sandbox.dto.SandboxTestResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço de Sandbox para execução segura de código gerado por IA.
 *
 * <p>Use cases:
 * <ul>
 *   <li>Validar correções automáticas antes de aplicar</li>
 *   <li>Executar testes em código modificado</li>
 *   <li>Verificar comportamento de código suspeito</li>
 *   <li>Testar snippets de código antes de sugerir</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeSandboxService {

    private final SandboxExecutor sandboxExecutor;

    /**
     * Valida uma correção de código executando-a em sandbox.
     *
     * @param originalCode Código original
     * @param fixedCode Código corrigido
     * @param language Linguagem
     * @return Resultado da validação
     */
    public CodeValidationResult validateFix(String originalCode, String fixedCode, String language) {
        // 1. Valida sintaxe do código corrigido
        if (!sandboxExecutor.validateSyntax(fixedCode, language)) {
            return CodeValidationResult.syntaxError("Fixed code has syntax errors");
        }

        // 2. Verifica segurança
        SandboxExecutionRequest securityRequest = SandboxExecutionRequest.of(fixedCode, language);
        List<String> securityIssues = sandboxExecutor.validateSecurity(securityRequest);

        if (!securityIssues.isEmpty()) {
            return CodeValidationResult.securityIssue(String.join(", ", securityIssues));
        }

        // 3. Executa ambos para comparar comportamento
        SandboxExecutionResult originalResult = executeSafely(originalCode, language);
        SandboxExecutionResult fixedResult = executeSafely(fixedCode, language);

        // 4. Compara resultados
        ValidationComparison comparison = compareResults(originalResult, fixedResult);

        return CodeValidationResult.valid(
                comparison.behaviorPreserved(),
                comparison.outputSimilar(),
                comparison.improvements()
        );
    }

    /**
     * Executa testes em código.
     *
     * @param code Código a testar
     * @param tests Código dos testes
     * @param language Linguagem
     * @return Resultado dos testes
     */
    public SandboxTestResult runTests(String code, String tests, String language) {
        return sandboxExecutor.runTests(code, tests, language);
    }

    /**
     * Executa código e retorna resultado.
     *
     * @param code Código
     * @param language Linguagem
     * @return Resultado da execução
     */
    public SandboxExecutionResult execute(String code, String language) {
        SandboxExecutionRequest request = SandboxExecutionRequest.of(code, language);
        return sandboxExecutor.execute(request);
    }

    /**
     * Executa código com input.
     *
     * @param code Código
     * @param input Input
     * @param language Linguagem
     * @return Resultado da execução
     */
    public SandboxExecutionResult execute(String code, String input, String language) {
        SandboxExecutionRequest request = SandboxExecutionRequest.withInput(code, language, input);
        return sandboxExecutor.execute(request);
    }

    /**
     * Executa código com timeout específico.
     *
     * @param code Código
     * @param language Linguagem
     * @param timeoutSeconds Timeout
     * @return Resultado da execução
     */
    public SandboxExecutionResult execute(String code, String language, int timeoutSeconds) {
        SandboxExecutionRequest request = SandboxExecutionRequest.of(code, language);
        return sandboxExecutor.execute(request, timeoutSeconds);
    }

    /**
     * Verifica se código é seguro para execução.
     *
     * @param code Código
     * @param language Linguagem
     * @return true se seguro
     */
    public boolean isSecure(String code, String language) {
        SandboxExecutionRequest request = SandboxExecutionRequest.of(code, language);
        List<String> issues = sandboxExecutor.validateSecurity(request);

        boolean syntaxValid = sandboxExecutor.validateSyntax(code, language);

        return syntaxValid && issues.isEmpty();
    }

    /**
     * Analisa código para detectar problemas potenciais.
     *
     * @param code Código
     * @param language Linguagem
     * @return Lista de problemas encontrados
     */
    public List<SandboxIssue> analyzeCode(String code, String language) {
        List<SandboxIssue> issues = new java.util.ArrayList<>();

        // 1. Valida sintaxe
        if (!sandboxExecutor.validateSyntax(code, language)) {
            issues.add(new SandboxIssue(
                    SandboxIssue.Severity.ERROR,
                    "SYNTAX",
                    "Code has syntax errors",
                    null
            ));
        }

        // 2. Verifica segurança
        SandboxExecutionRequest request = SandboxExecutionRequest.of(code, language);
        List<String> securityIssues = sandboxExecutor.validateSecurity(request);

        for (String securityIssue : securityIssues) {
            issues.add(new SandboxIssue(
                    SandboxIssue.Severity.CRITICAL,
                    "SECURITY",
                    securityIssue,
                    null
            ));
        }

        // 3. Analisa qualidade
        issues.addAll(analyzeQuality(code, language));

        return issues;
    }

    // ========== Private Methods ==========

    /**
     * Executa código capturando erros silenciosamente.
     */
    private SandboxExecutionResult executeSafely(String code, String language) {
        try {
            SandboxExecutionRequest request = SandboxExecutionRequest.of(code, language);
            return sandboxExecutor.execute(request, 10);
        } catch (Exception e) {
            return SandboxExecutionResult.failed(e.getMessage(), java.time.Duration.ZERO);
        }
    }

    /**
     * Compara resultados de execução.
     */
    private ValidationComparison compareResults(
            SandboxExecutionResult original,
            SandboxExecutionResult fixed
    ) {
        boolean behaviorPreserved = true;
        boolean outputSimilar = true;
        List<String> improvements = new java.util.ArrayList<>();

        // Se ambos falharam com mesmo erro, comportamento preservado
        if (!original.success() && !fixed.success()) {
            behaviorPreserved = original.error().equals(fixed.error());
        }
        // Se original falhava e fixado funciona, é uma melhoria
        else if (!original.success() && fixed.success()) {
            improvements.add("Fixed execution error: " + original.error());
            behaviorPreserved = true;
            outputSimilar = false; // Output mudou (de erro para sucesso)
        }
        // Se original funcionava e fixado falha, problema
        else if (original.success() && !fixed.success()) {
            behaviorPreserved = false;
            improvements.add("Regression: now fails with " + fixed.error());
        }
        // Se ambos funcionam, compara output
        else if (original.success() && fixed.success()) {
            outputSimilar = original.output().equals(fixed.output());

            if (!outputSimilar) {
                improvements.add("Output changed from execution");

                // Verifica se a mudança parece intencional (ex: formatação)
                if (normalizedOutput(original.output()).equals(normalizedOutput(fixed.output()))) {
                    outputSimilar = true;
                    improvements.add("Output difference is only formatting");
                }
            }
        }

        return new ValidationComparison(behaviorPreserved, outputSimilar, improvements);
    }

    /**
     * Normaliza output para comparação (remove espaços extras, etc).
     */
    private String normalizedOutput(String output) {
        if (output == null) return "";
        return output.trim().replaceAll("\\s+", " ");
    }

    /**
     * Analisa qualidade do código.
     */
    private List<SandboxIssue> analyzeQuality(String code, String language) {
        List<SandboxIssue> issues = new java.util.ArrayList<>();

        // Verifica tamanho do código
        if (code.length() > 5000) {
            issues.add(new SandboxIssue(
                    SandboxIssue.Severity.WARNING,
                    "COMPLEXITY",
                    "Code is very long (" + code.length() + " chars)",
                    "Consider breaking into smaller functions"
            ));
        }

        // Verifica linhas muito longas
        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].length() > 120) {
                issues.add(new SandboxIssue(
                        SandboxIssue.Severity.INFO,
                        "STYLE",
                        "Line " + (i + 1) + " is too long (" + lines[i].length() + " chars)",
                        "Consider breaking long lines"
                ));
            }
        }

        // Verifica comentários
        boolean hasComments = code.contains("//") || code.contains("/*") || code.contains("*") || code.contains("#");
        if (!hasComments && code.length() > 500) {
            issues.add(new SandboxIssue(
                    SandboxIssue.Severity.INFO,
                    "DOCUMENTATION",
                    "Code lacks comments",
                    "Consider adding documentation"
            ));
        }

        return issues;
    }

    // ========== Inner Classes ==========

    /**
     * Resultado da comparação de validação.
     */
    private record ValidationComparison(
            boolean behaviorPreserved,
            boolean outputSimilar,
            List<String> improvements
    ) {}

    /**
     * Resultado da validação de código.
     */
    public static class CodeValidationResult {
        private final boolean valid;
        private final boolean behaviorPreserved;
        private final boolean outputSimilar;
        private final List<String> improvements;
        private final String error;

        private CodeValidationResult(
                boolean valid,
                boolean behaviorPreserved,
                boolean outputSimilar,
                List<String> improvements,
                String error
        ) {
            this.valid = valid;
            this.behaviorPreserved = behaviorPreserved;
            this.outputSimilar = outputSimilar;
            this.improvements = improvements;
            this.error = error;
        }

        public static CodeValidationResult valid(boolean behaviorPreserved, boolean outputSimilar, List<String> improvements) {
            return new CodeValidationResult(true, behaviorPreserved, outputSimilar, improvements, null);
        }

        public static CodeValidationResult syntaxError(String error) {
            return new CodeValidationResult(false, false, false, List.of(), error);
        }

        public static CodeValidationResult securityIssue(String issue) {
            return new CodeValidationResult(false, false, false, List.of(), "Security: " + issue);
        }

        public boolean isValid() { return valid; }
        public boolean isBehaviorPreserved() { return behaviorPreserved; }
        public boolean isOutputSimilar() { return outputSimilar; }
        public List<String> getImprovements() { return improvements; }
        public String getError() { return error; }
    }

    /**
     * Issue detectado na análise de código.
     */
    public static class SandboxIssue {
        private final Severity severity;
        private final String category;
        private final String message;
        private final String suggestion;

        public SandboxIssue(Severity severity, String category, String message, String suggestion) {
            this.severity = severity;
            this.category = category;
            this.message = message;
            this.suggestion = suggestion;
        }

        public Severity getSeverity() { return severity; }
        public String getCategory() { return category; }
        public String getMessage() { return message; }
        public String getSuggestion() { return suggestion; }

        public enum Severity {
            INFO,
            WARNING,
            ERROR,
            CRITICAL
        }
    }
}
