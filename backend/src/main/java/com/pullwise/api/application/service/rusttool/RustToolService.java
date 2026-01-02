package com.pullwise.api.application.service.rusttool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de integração das ferramentas Rust (Biome, Ruff) no pipeline de review.
 *
 * <p>Vantagens:
 * <ul>
 *   <li>Biome: 10-100x mais rápido que ESLint/Prettier</li>
 *   <li>Ruff: 10-100x mais rápido que Flake8/Black</li>
 *   <li>Análise incremental extremamente rápida</li>
 *   <li>Low latency para feedback em tempo real</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RustToolService {

    private final RustToolExecutor rustToolExecutor;

    /**
     * Executa análise Biome em arquivos JavaScript/TypeScript.
     *
     * @param files Lista de arquivos
     * @param workingDir Diretório de trabalho
     * @return Resultado da análise
     */
    public RustAnalysisResult analyzeWithBiome(List<String> files, String workingDir) {
        if (!rustToolExecutor.isBiomeAvailable()) {
            log.warn("Biome is not available");
            return RustAnalysisResult.unavailable("Biome not found");
        }

        long startTime = System.currentTimeMillis();

        RustToolExecutor.BiomeRequest request =
                new RustToolExecutor.BiomeRequest(files, workingDir, false, false, true, null);

        RustToolExecutor.BiomeResult result = rustToolExecutor.executeBiome(request);

        long duration = System.currentTimeMillis() - startTime;

        return new RustAnalysisResult(
                "biome",
                rustToolExecutor.getBiomeVersion(),
                result.success(),
                convertBiomeIssues(result.issues()),
                duration,
                result.rawOutput()
        );
    }

    /**
     * Executa análise Ruff em arquivos Python.
     *
     * @param files Lista de arquivos
     * @param workingDir Diretório de trabalho
     * @return Resultado da análise
     */
    public RustAnalysisResult analyzeWithRuff(List<String> files, String workingDir) {
        if (!rustToolExecutor.isRuffAvailable()) {
            log.warn("Ruff is not available");
            return RustAnalysisResult.unavailable("Ruff not found");
        }

        long startTime = System.currentTimeMillis();

        RustToolExecutor.RuffRequest request =
                new RustToolExecutor.RuffRequest(files, workingDir, false, false, true, null, null, null, false);

        RustToolExecutor.RuffResult result = rustToolExecutor.executeRuff(request);

        long duration = System.currentTimeMillis() - startTime;

        return new RustAnalysisResult(
                "ruff",
                rustToolExecutor.getRuffVersion(),
                result.success(),
                convertRuffIssues(result.issues()),
                duration,
                result.rawOutput()
        );
    }

    /**
     * Executa análise usando a ferramenta apropriada baseado na extensão do arquivo.
     *
     * @param files Lista de arquivos
     * @param workingDir Diretório de trabalho
     * @return Mapa de ferramenta para resultado
     */
    public Map<String, RustAnalysisResult> analyzeWithAppropriateTools(List<String> files, String workingDir) {
        Map<String, RustAnalysisResult> results = new HashMap<>();

        // Separa arquivos por tipo
        List<String> jsTsFiles = new ArrayList<>();
        List<String> pythonFiles = new ArrayList<>();

        for (String file : files) {
            String lower = file.toLowerCase();
            if (lower.endsWith(".js") || lower.endsWith(".jsx") ||
                lower.endsWith(".ts") || lower.endsWith(".tsx") ||
                lower.endsWith(".mjs") || lower.endsWith(".cjs")) {
                jsTsFiles.add(file);
            } else if (lower.endsWith(".py")) {
                pythonFiles.add(file);
            }
        }

        // Executa Biome para JS/TS
        if (!jsTsFiles.isEmpty()) {
            RustAnalysisResult biomeResult = analyzeWithBiome(jsTsFiles, workingDir);
            results.put("biome", biomeResult);
        }

        // Executa Ruff para Python
        if (!pythonFiles.isEmpty()) {
            RustAnalysisResult ruffResult = analyzeWithRuff(pythonFiles, workingDir);
            results.put("ruff", ruffResult);
        }

        return results;
    }

    /**
     * Formata arquivos usando Biome.
     *
     * @param files Lista de arquivos
     * @param workingDir Diretório de trabalho
     * @return true se formatado com sucesso
     */
    public boolean formatWithBiome(List<String> files, String workingDir) {
        if (!rustToolExecutor.isBiomeAvailable()) {
            return false;
        }

        RustToolExecutor.BiomeRequest request =
                new RustToolExecutor.BiomeRequest(files, workingDir, false, true, false, null);

        RustToolExecutor.BiomeResult result = rustToolExecutor.executeBiome(request);

        return result.success();
    }

    /**
     * Formata arquivos usando Ruff.
     *
     * @param files Lista de arquivos
     * @param workingDir Diretório de trabalho
     * @return true se formatado com sucesso
     */
    public boolean formatWithRuff(List<String> files, String workingDir) {
        if (!rustToolExecutor.isRuffAvailable()) {
            return false;
        }

        RustToolExecutor.RuffRequest request =
                new RustToolExecutor.RuffRequest(files, workingDir, false, true, false, null, null, null, true);

        RustToolExecutor.RuffResult result = rustToolExecutor.executeRuff(request);

        return result.success();
    }

    /**
     * Verifica se as ferramentas estão disponíveis.
     */
    public RustToolsStatus getStatus() {
        return new RustToolsStatus(
                rustToolExecutor.isBiomeAvailable(),
                rustToolExecutor.isRuffAvailable(),
                rustToolExecutor.getBiomeVersion(),
                rustToolExecutor.getRuffVersion()
        );
    }

    /**
     * Executa check de tipo com Biome.
     *
     * @param files Lista de arquivos
     * @param workingDir Diretório de trabalho
     * @return Resultado do check
     */
    public RustAnalysisResult checkWithBiome(List<String> files, String workingDir) {
        if (!rustToolExecutor.isBiomeAvailable()) {
            return RustAnalysisResult.unavailable("Biome not found");
        }

        long startTime = System.currentTimeMillis();

        RustToolExecutor.BiomeRequest request =
                new RustToolExecutor.BiomeRequest(files, workingDir, true, false, true, null);

        RustToolExecutor.BiomeResult result = rustToolExecutor.executeBiome(request);

        long duration = System.currentTimeMillis() - startTime;

        return new RustAnalysisResult(
                "biome",
                rustToolExecutor.getBiomeVersion(),
                result.success(),
                convertBiomeIssues(result.issues()),
                duration,
                result.rawOutput()
        );
    }

    // ========== Private Methods ==========

    private List<RustIssue> convertBiomeIssues(List<RustToolExecutor.BiomeIssue> biomeIssues) {
        if (biomeIssues == null) {
            return List.of();
        }

        return biomeIssues.stream()
                .map(biome -> new RustIssue(
                        biome.filePath(),
                        biome.line(),
                        biome.column(),
                        biome.severity(),
                        "BIOME",
                        biome.message()
                ))
                .toList();
    }

    private List<RustIssue> convertRuffIssues(List<RustToolExecutor.RuffIssue> ruffIssues) {
        if (ruffIssues == null) {
            return List.of();
        }

        return ruffIssues.stream()
                .map(ruff -> {
                    String severity = inferSeverityFromCode(ruff.code());
                    return new RustIssue(
                            ruff.filePath(),
                            ruff.line(),
                            ruff.column(),
                            severity,
                            ruff.code(),
                            ruff.message()
                    );
                })
                .toList();
    }

    private String inferSeverityFromCode(String code) {
        if (code == null) return "warning";

        return switch (code.charAt(0)) {
            case 'E' -> "error";
            case 'F' -> "error";
            case 'W' -> "warning";
            default -> "info";
        };
    }

    // ========== DTOs ==========

    /**
     * Resultado da análise com ferramenta Rust.
     */
    public static class RustAnalysisResult {
        private final String toolName;
        private final String toolVersion;
        private final boolean success;
        private final List<RustIssue> issues;
        private final long durationMs;
        private final String rawOutput;
        private final boolean unavailable;
        private final String unavailabilityReason;

        public RustAnalysisResult(String toolName, String toolVersion, boolean success,
                                   List<RustIssue> issues, long durationMs, String rawOutput) {
            this.toolName = toolName;
            this.toolVersion = toolVersion;
            this.success = success;
            this.issues = issues;
            this.durationMs = durationMs;
            this.rawOutput = rawOutput;
            this.unavailable = false;
            this.unavailabilityReason = null;
        }

        private RustAnalysisResult(String unavailabilityReason) {
            this.toolName = "unknown";
            this.toolVersion = "unknown";
            this.success = false;
            this.issues = List.of();
            this.durationMs = 0;
            this.rawOutput = null;
            this.unavailable = true;
            this.unavailabilityReason = unavailabilityReason;
        }

        public static RustAnalysisResult unavailable(String reason) {
            return new RustAnalysisResult(reason);
        }

        public String getToolName() { return toolName; }
        public String getToolVersion() { return toolVersion; }
        public boolean isSuccess() { return success; }
        public List<RustIssue> getIssues() { return issues; }
        public long getDurationMs() { return durationMs; }
        public String getRawOutput() { return rawOutput; }
        public boolean isUnavailable() { return unavailable; }
        public String getUnavailabilityReason() { return unavailabilityReason; }

        public int getIssueCount() { return issues.size(); }
        public long getErrorCount() {
            return issues.stream().filter(i -> "error".equals(i.getSeverity())).count();
        }
        public long getWarningCount() {
            return issues.stream().filter(i -> "warning".equals(i.getSeverity())).count();
        }
    }

    /**
     * Issue detectado pela ferramenta Rust.
     */
    public static class RustIssue {
        private final String filePath;
        private final int line;
        private final int column;
        private final String severity;
        private final String code;
        private final String message;

        public RustIssue(String filePath, int line, int column, String severity, String code, String message) {
            this.filePath = filePath;
            this.line = line;
            this.column = column;
            this.severity = severity;
            this.code = code;
            this.message = message;
        }

        public String getFilePath() { return filePath; }
        public int getLine() { return line; }
        public int getColumn() { return column; }
        public String getSeverity() { return severity; }
        public String getCode() { return code; }
        public String getMessage() { return message; }

        public String getLocation() {
            return filePath + ":" + line + ":" + column;
        }

        public String getFormattedMessage() {
            return String.format("[%s] %s: %s", code, getLocation(), message);
        }
    }

    /**
     * Status das ferramentas Rust.
     */
    public record RustToolsStatus(
            boolean biomeAvailable,
            boolean ruffAvailable,
            String biomeVersion,
            String ruffVersion
    ) {
        public boolean isAnyAvailable() {
            return biomeAvailable || ruffAvailable;
        }
    }
}
