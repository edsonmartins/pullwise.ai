package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.service.rusttool.RustToolService;
import com.pullwise.api.application.service.rusttool.RustToolService.RustAnalysisResult;
import com.pullwise.api.application.service.rusttool.RustToolService.RustIssue;
import com.pullwise.api.application.service.rusttool.RustToolService.RustToolsStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST para ferramentas Rust (Biome, Ruff).
 */
@Slf4j
@RestController
@RequestMapping("/api/rust-tools")
@RequiredArgsConstructor
public class RustToolsController {

    private final RustToolService rustToolService;

    /**
     * Retorna status das ferramentas Rust disponíveis.
     */
    @GetMapping("/status")
    public ResponseEntity<RustToolsStatus> getStatus() {
        return ResponseEntity.ok(rustToolService.getStatus());
    }

    /**
     * Analisa arquivos com Biome (JavaScript/TypeScript).
     */
    @PostMapping("/biome/analyze")
    public ResponseEntity<RustAnalysisResponse> analyzeWithBiome(
            @Valid @RequestBody RustAnalysisRequest request
    ) {
        RustAnalysisResult result = rustToolService.analyzeWithBiome(
                request.files(),
                request.workingDir()
        );

        return ResponseEntity.ok(RustAnalysisResponse.from(result));
    }

    /**
     * Analisa arquivos com Ruff (Python).
     */
    @PostMapping("/ruff/analyze")
    public ResponseEntity<RustAnalysisResponse> analyzeWithRuff(
            @Valid @RequestBody RustAnalysisRequest request
    ) {
        RustAnalysisResult result = rustToolService.analyzeWithRuff(
                request.files(),
                request.workingDir()
        );

        return ResponseEntity.ok(RustAnalysisResponse.from(result));
    }

    /**
     * Analisa arquivos com a ferramenta apropriada automaticamente.
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, RustAnalysisResponse>> analyzeWithAppropriateTools(
            @Valid @RequestBody RustAnalysisRequest request
    ) {
        Map<String, RustAnalysisResult> results =
                rustToolService.analyzeWithAppropriateTools(
                        request.files(),
                        request.workingDir()
                );

        Map<String, RustAnalysisResponse> responses = Map.of(
                "biome", results.containsKey("biome") ? RustAnalysisResponse.from(results.get("biome")) : null,
                "ruff", results.containsKey("ruff") ? RustAnalysisResponse.from(results.get("ruff")) : null
        );

        return ResponseEntity.ok(responses);
    }

    /**
     * Formata arquivos com Biome.
     */
    @PostMapping("/biome/format")
    public ResponseEntity<FormatResponse> formatWithBiome(
            @Valid @RequestBody RustFormatRequest request
    ) {
        boolean success = rustToolService.formatWithBiome(
                request.files(),
                request.workingDir()
        );

        return ResponseEntity.ok(new FormatResponse(success));
    }

    /**
     * Formata arquivos com Ruff.
     */
    @PostMapping("/ruff/format")
    public ResponseEntity<FormatResponse> formatWithRuff(
            @Valid @RequestBody RustFormatRequest request
    ) {
        boolean success = rustToolService.formatWithRuff(
                request.files(),
                request.workingDir()
        );

        return ResponseEntity.ok(new FormatResponse(success));
    }

    /**
     * Executa check de tipo com Biome.
     */
    @PostMapping("/biome/check")
    public ResponseEntity<RustAnalysisResponse> checkWithBiome(
            @Valid @RequestBody RustAnalysisRequest request
    ) {
        RustAnalysisResult result = rustToolService.checkWithBiome(
                request.files(),
                request.workingDir()
        );

        return ResponseEntity.ok(RustAnalysisResponse.from(result));
    }

    // ========== DTOs ==========

    public record RustAnalysisRequest(
            List<String> files,
            String workingDir
    ) {}

    public record RustAnalysisResponse(
            String toolName,
            String toolVersion,
            boolean success,
            boolean unavailable,
            String unavailabilityReason,
            List<IssueDTO> issues,
            int issueCount,
            long errorCount,
            long warningCount,
            long durationMs,
            String rawOutput
    ) {
        public static RustAnalysisResponse from(RustAnalysisResult result) {
            if (result.isUnavailable()) {
                return new RustAnalysisResponse(
                        "unknown",
                        "unknown",
                        false,
                        true,
                        result.getUnavailabilityReason(),
                        List.of(),
                        0,
                        0,
                        0,
                        0,
                        null
                );
            }

            List<IssueDTO> issueDTOs = result.getIssues().stream()
                    .map(IssueDTO::from)
                    .toList();

            return new RustAnalysisResponse(
                    result.getToolName(),
                    result.getToolVersion(),
                    result.isSuccess(),
                    false,
                    null,
                    issueDTOs,
                    result.getIssueCount(),
                    result.getErrorCount(),
                    result.getWarningCount(),
                    result.getDurationMs(),
                    result.getRawOutput()
            );
        }
    }

    public record IssueDTO(
            String filePath,
            int line,
            int column,
            String severity,
            String code,
            String message,
            String location,
            String formattedMessage
    ) {
        public static IssueDTO from(RustToolService.RustIssue issue) {
            return new IssueDTO(
                    issue.getFilePath(),
                    issue.getLine(),
                    issue.getColumn(),
                    issue.getSeverity(),
                    issue.getCode(),
                    issue.getMessage(),
                    issue.getLocation(),
                    issue.getFormattedMessage()
            );
        }
    }

    public record RustFormatRequest(
            List<String> files,
            String workingDir
    ) {}

    public record FormatResponse(
            boolean success
    ) {}
}
