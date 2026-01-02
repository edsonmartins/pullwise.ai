package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.service.sandbox.CodeSandboxService;
import com.pullwise.api.application.service.sandbox.SandboxExecutor;
import com.pullwise.api.application.service.sandbox.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para execução de código em sandbox.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/sandbox/execute - Executa código</li>
 *   <li>POST /api/sandbox/validate - Valida correção de código</li>
 *   <li>POST /api/sandbox/test - Executa testes</li>
 *   <li>POST /api/sandbox/security - Verifica segurança</li>
 *   <li>POST /api/sandbox/analyze - Analisa código</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/sandbox")
@RequiredArgsConstructor
public class SandboxController {

    private final SandboxExecutor sandboxExecutor;
    private final CodeSandboxService codeSandboxService;

    /**
     * Executa código em sandbox.
     */
    @PostMapping("/execute")
    public ResponseEntity<SandboxExecutionResponse> execute(
            @Valid @RequestBody SandboxExecuteRequest request
    ) {
        SandboxExecutionRequest execRequest = new SandboxExecutionRequest(
                request.code(),
                request.language(),
                request.input(),
                request.allowFileSystem() != null ? request.allowFileSystem() : false,
                request.memoryLimitMb() != null ? request.memoryLimitMb() : 256
        );

        SandboxExecutionResult result = sandboxExecutor.execute(
                execRequest,
                request.timeoutSeconds() != null ? request.timeoutSeconds() : 30
        );

        return ResponseEntity.ok(SandboxExecutionResponse.from(result));
    }

    /**
     * Valida uma correção de código comparando com o original.
     */
    @PostMapping("/validate")
    public ResponseEntity<SandboxValidationResponse> validateFix(
            @Valid @RequestBody SandboxValidationRequest request
    ) {
        CodeSandboxService.CodeValidationResult result =
                codeSandboxService.validateFix(
                        request.originalCode(),
                        request.fixedCode(),
                        request.language()
                );

        return ResponseEntity.ok(SandboxValidationResponse.from(result));
    }

    /**
     * Executa testes em código.
     */
    @PostMapping("/test")
    public ResponseEntity<SandboxTestResponse> runTests(
            @Valid @RequestBody SandboxTestRequest request
    ) {
        SandboxTestResult result = codeSandboxService.runTests(
                request.code(),
                request.tests(),
                request.language()
        );

        return ResponseEntity.ok(SandboxTestResponse.from(result));
    }

    /**
     * Verifica se código é seguro.
     */
    @PostMapping("/security")
    public ResponseEntity<SandboxSecurityResponse> checkSecurity(
            @Valid @RequestBody SandboxSecurityRequest request
    ) {
        boolean isSecure = codeSandboxService.isSecure(
                request.code(),
                request.language()
        );

        SandboxExecutionRequest execRequest = SandboxExecutionRequest.of(
                request.code(),
                request.language()
        );

        List<String> issues = sandboxExecutor.validateSecurity(execRequest);

        return ResponseEntity.ok(new SandboxSecurityResponse(
                isSecure,
                issues
        ));
    }

    /**
     * Analisa código para detectar problemas.
     */
    @PostMapping("/analyze")
    public ResponseEntity<SandboxAnalysisResponse> analyzeCode(
            @Valid @RequestBody SandboxAnalysisRequest request
    ) {
        List<CodeSandboxService.SandboxIssue> issues =
                codeSandboxService.analyzeCode(
                        request.code(),
                        request.language()
                );

        List<SandboxIssueDTO> issueDTOs = issues.stream()
                .map(SandboxIssueDTO::from)
                .toList();

        return ResponseEntity.ok(new SandboxAnalysisResponse(issueDTOs));
    }

    /**
     * Verifica apenas a sintaxe do código.
     */
    @PostMapping("/syntax")
    public ResponseEntity<SandboxSyntaxResponse> checkSyntax(
            @Valid @RequestBody SandboxSyntaxRequest request
    ) {
        boolean valid = sandboxExecutor.validateSyntax(
                request.code(),
                request.language()
        );

        return ResponseEntity.ok(new SandboxSyntaxResponse(valid));
    }

    // ========== DTOs ==========

    public record SandboxExecuteRequest(
            String code,
            String language,
            String input,
            Boolean allowFileSystem,
            Integer memoryLimitMb,
            Integer timeoutSeconds
    ) {}

    public record SandboxExecutionResponse(
            boolean success,
            String output,
            String error,
            int exitCode,
            long durationMs,
            String metadata
    ) {
        public static SandboxExecutionResponse from(SandboxExecutionResult result) {
            return new SandboxExecutionResponse(
                    result.success(),
                    result.output(),
                    result.error(),
                    result.exitCode(),
                    result.duration().toMillis(),
                    result.metadata().toString()
            );
        }
    }

    public record SandboxValidationRequest(
            String originalCode,
            String fixedCode,
            String language
    ) {}

    public record SandboxValidationResponse(
            boolean valid,
            boolean behaviorPreserved,
            boolean outputSimilar,
            List<String> improvements,
            String error
    ) {
        public static SandboxValidationResponse from(CodeSandboxService.CodeValidationResult result) {
            return new SandboxValidationResponse(
                    result.isValid(),
                    result.isBehaviorPreserved(),
                    result.isOutputSimilar(),
                    result.getImprovements(),
                    result.getError()
            );
        }
    }

    public record SandboxTestRequest(
            String code,
            String tests,
            String language
    ) {}

    public record SandboxTestResponse(
            int passed,
            int failed,
            int total,
            double successRate,
            boolean allPassed,
            String output,
            long durationMs,
            String error
    ) {
        public static SandboxTestResponse from(SandboxTestResult result) {
            return new SandboxTestResponse(
                    result.passed(),
                    result.failed(),
                    result.total(),
                    Math.round(result.successRate() * 10000) / 100.0,
                    result.allPassed(),
                    result.output(),
                    result.duration().toMillis(),
                    result.error()
            );
        }
    }

    public record SandboxSecurityRequest(
            String code,
            String language
    ) {}

    public record SandboxSecurityResponse(
            boolean secure,
            List<String> issues
    ) {}

    public record SandboxAnalysisRequest(
            String code,
            String language
    ) {}

    public record SandboxAnalysisResponse(
            List<SandboxIssueDTO> issues
    ) {}

    public record SandboxSyntaxRequest(
            String code,
            String language
    ) {}

    public record SandboxSyntaxResponse(
            boolean valid
    ) {}

    public record SandboxIssueDTO(
            String severity,
            String category,
            String message,
            String suggestion
    ) {
        public static SandboxIssueDTO from(CodeSandboxService.SandboxIssue issue) {
            return new SandboxIssueDTO(
                    issue.getSeverity().name(),
                    issue.getCategory(),
                    issue.getMessage(),
                    issue.getSuggestion()
            );
        }
    }
}
