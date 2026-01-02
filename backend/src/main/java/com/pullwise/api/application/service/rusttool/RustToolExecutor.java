package com.pullwise.api.application.service.rusttool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Executor para ferramentas Rust baseadas (Biome, Ruff).
 *
 * <p>Benefícios das ferramentas Rust:
 * <ul>
 *   <li>10-100x mais rápidas que equivalentes JavaScript/Python</li>
 *   <li>Baixa latência para análise incremental</li>
 *   <li>Menor consumo de memória</li>
 *   <li>Melhor paralelização</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RustToolExecutor {

    @Value("${rust-tools.biome.path:biome}")
    private String biomePath;

    @Value("${rust-tools.ruff.path:ruff}")
    private String ruffPath;

    @Value("${rust-tools.timeout-seconds:60}")
    private int timeoutSeconds;

    private static final String GIT_BINARY = "git";

    /**
     * Executa análise Biome em arquivos JavaScript/TypeScript.
     *
     * @param request Request de análise
     * @return Resultado da análise
     */
    public BiomeResult executeBiome(BiomeRequest request) {
        List<String> command = buildBiomeCommand(request);

        try {
            ProcessResult result = executeCommand(
                    command,
                    request.workingDir() != null ? request.workingDir() : "."
            );

            return parseBiomeResult(result, request);

        } catch (Exception e) {
            log.error("Failed to execute Biome", e);
            return BiomeResult.failed(e.getMessage());
        }
    }

    /**
     * Executa análise Ruff em arquivos Python.
     *
     * @param request Request de análise
     * @return Resultado da análise
     */
    public RuffResult executeRuff(RuffRequest request) {
        List<String> command = buildRuffCommand(request);

        try {
            ProcessResult result = executeCommand(
                    command,
                    request.workingDir() != null ? request.workingDir() : "."
            );

            return parseRuffResult(result, request);

        } catch (Exception e) {
            log.error("Failed to execute Ruff", e);
            return RuffResult.failed(e.getMessage());
        }
    }

    /**
     * Verifica se Biome está disponível.
     */
    public boolean isBiomeAvailable() {
        try {
            ProcessResult result = executeCommand(
                    List.of(biomePath, "--version"),
                    ".",
                    5
            );
            return result.exitCode() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica se Ruff está disponível.
     */
    public boolean isRuffAvailable() {
        try {
            ProcessResult result = executeCommand(
                    List.of(ruffPath, "--version"),
                    ".",
                    5
            );
            return result.exitCode() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retorna versão do Biome.
     */
    public String getBiomeVersion() {
        try {
            ProcessResult result = executeCommand(
                    List.of(biomePath, "--version"),
                    ".",
                    5
            );
            return result.stdout().trim();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Retorna versão do Ruff.
     */
    public String getRuffVersion() {
        try {
            ProcessResult result = executeCommand(
                    List.of(ruffPath, "--version"),
                    ".",
                    5
            );
            return result.stdout().trim();
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ========== Private Methods ==========

    private List<String> buildBiomeCommand(BiomeRequest request) {
        List<String> command = new ArrayList<>();
        command.add(biomePath);

        // Subcomando
        if (request.check()) {
            command.add("check");
        } else if (request.format()) {
            command.add("format");
        } else {
            command.add("lint");
        }

        // Arquivos a analisar
        if (request.files() != null && !request.files().isEmpty()) {
            command.addAll(request.files());
        } else {
            command.add(".");
        }

        // Opções de saída
        command.add("--diagnostic-level=info");
        command.add("--verbose");

        // Formato JSON se solicitado
        if (request.jsonOutput()) {
            command.add("--json");
        }

        // Arquivo de configuração customizada
        if (request.configPath() != null) {
            command.add("--config-path=" + request.configPath());
        }

        return command;
    }

    private List<String> buildRuffCommand(RuffRequest request) {
        List<String> command = new ArrayList<>();
        command.add(ruffPath);

        // Subcomando
        if (request.check()) {
            command.add("check");
        } else if (request.format()) {
            command.add("format");
        } else {
            command.add("lint");
        }

        // Arquivos a analisar
        if (request.files() != null && !request.files().isEmpty()) {
            command.addAll(request.files());
        } else {
            command.add(".");
        }

        // Opções de saída
        if (request.jsonOutput()) {
            command.add("--output-format=json");
        }

        // Nível de severidade
        if (request.severity() != null) {
            command.add("--severity=" + request.severity());
        }

        // Select de regras
        if (request.selectRules() != null && !request.selectRules().isEmpty()) {
            command.add("--select=" + String.join(",", request.selectRules()));
        }

        // Ignore de regras
        if (request.ignoreRules() != null && !request.ignoreRules().isEmpty()) {
            command.add("--ignore=" + String.join(",", request.ignoreRules()));
        }

        // Fix automático
        if (request.fix()) {
            command.add("--fix");
        }

        return command;
    }

    private BiomeResult parseBiomeResult(ProcessResult result, BiomeRequest request) {
        boolean success = result.exitCode() == 0;

        if (request.jsonOutput()) {
            // Parse JSON output
            return parseBiomeJson(result.stdout(), success);
        }

        // Parse texto simples
        List<BiomeIssue> issues = new ArrayList<>();

        if (!result.stdout().isBlank()) {
            String[] lines = result.stdout().split("\n");
            for (String line : lines) {
                BiomeIssue issue = parseBiomeLine(line);
                if (issue != null) {
                    issues.add(issue);
                }
            }
        }

        return new BiomeResult(
                success,
                issues,
                result.stderr(),
                result.durationMs()
        );
    }

    private BiomeResult parseBiomeJson(String json, boolean success) {
        // Parse simplificado - em produção usaria ObjectMapper
        List<BiomeIssue> issues = new ArrayList<>();

        if (json.contains("\"diagnostics\"")) {
            // Extrai diagnostics do JSON
            // Para simplificar, retorna um resultado genérico
            return new BiomeResult(
                    success,
                    issues,
                    json,
                    0
            );
        }

        return new BiomeResult(
                success,
                issues,
                json,
                0
        );
    }

    private BiomeIssue parseBiomeLine(String line) {
        // Formato esperado: file:line:col: severity: message
        try {
            String[] parts = line.split(":", 5);
            if (parts.length >= 5) {
                return new BiomeIssue(
                        parts[0].trim(),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim()),
                        parts[3].trim(),
                        parts[4].trim()
                );
            }
        } catch (NumberFormatException e) {
            // Ignora linhas que não seguem o formato
        }
        return null;
    }

    private RuffResult parseRuffResult(ProcessResult result, RuffRequest request) {
        boolean success = result.exitCode() == 0;

        if (request.jsonOutput()) {
            return parseRuffJson(result.stdout(), success);
        }

        // Parse texto simples
        List<RuffIssue> issues = new ArrayList<>();

        if (!result.stdout().isBlank()) {
            String[] lines = result.stdout().split("\n");
            for (String line : lines) {
                RuffIssue issue = parseRuffLine(line);
                if (issue != null) {
                    issues.add(issue);
                }
            }
        }

        return new RuffResult(
                success,
                issues,
                result.stderr(),
                result.durationMs()
        );
    }

    private RuffResult parseRuffJson(String json, boolean success) {
        // Parse simplificado do JSON do Ruff
        List<RuffIssue> issues = new ArrayList<>();

        return new RuffResult(
                success,
                issues,
                json,
                0
        );
    }

    private RuffIssue parseRuffLine(String line) {
        // Formato esperado: file:line:col: code message
        try {
            String[] parts = line.split(":", 5);
            if (parts.length >= 5) {
                String codeAndMessage = parts[4].trim();
                String code = "";
                String message = codeAndMessage;

                if (codeAndMessage.matches("[A-Z]+\\d+\\s.*")) {
                    int spaceIndex = codeAndMessage.indexOf(' ');
                    code = codeAndMessage.substring(0, spaceIndex);
                    message = codeAndMessage.substring(spaceIndex + 1);
                }

                return new RuffIssue(
                        parts[0].trim(),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim()),
                        code,
                        message
                );
            }
        } catch (NumberFormatException e) {
            // Ignora linhas que não seguem o formato
        }
        return null;
    }

    private ProcessResult executeCommand(List<String> command, String workingDir) {
        return executeCommand(command, workingDir, timeoutSeconds);
    }

    private ProcessResult executeCommand(List<String> command, String workingDir, int timeout) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(Path.of(workingDir).toFile());

            Process process = pb.start();

            StreamGobbler stdoutGobbler = new StreamGobbler(process.getInputStream());
            StreamGobbler stderrGobbler = new StreamGobbler(process.getErrorStream());

            new Thread(stdoutGobbler).start();
            new Thread(stderrGobbler).start();

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RustToolException("Command timed out after " + timeout + "s");
            }

            int exitCode = process.exitValue();

            return new ProcessResult(
                    exitCode,
                    stdoutGobbler.getOutput(),
                    stderrGobbler.getOutput()
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute command: " + command, e);
        }
    }

    // ========== Inner Classes ==========

    private static class StreamGobbler implements Runnable {
        private final java.io.InputStream inputStream;
        private final StringBuilder output = new StringBuilder();

        public StreamGobbler(java.io.InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        public String getOutput() {
            return output.toString();
        }
    }

    // ========== DTOs ==========

    public record BiomeRequest(
            List<String> files,
            String workingDir,
            boolean check,
            boolean format,
            boolean jsonOutput,
            String configPath
    ) {
        public static BiomeRequest lint(List<String> files) {
            return new BiomeRequest(files, null, false, false, true, null);
        }

        public static BiomeRequest lint(List<String> files, String workingDir) {
            return new BiomeRequest(files, workingDir, false, false, true, null);
        }
    }

    public record BiomeResult(
            boolean success,
            List<BiomeIssue> issues,
            String rawOutput,
            long durationMs
    ) {
        public static BiomeResult failed(String error) {
            return new BiomeResult(false, List.of(), error, 0);
        }

        public int getIssueCount() {
            return issues != null ? issues.size() : 0;
        }

        public long getErrorCount() {
            return issues != null ? issues.stream().filter(i -> "error".equals(i.severity())).count() : 0;
        }

        public long getWarningCount() {
            return issues != null ? issues.stream().filter(i -> "warning".equals(i.severity())).count() : 0;
        }
    }

    public record BiomeIssue(
            String filePath,
            int line,
            int column,
            String severity,
            String message
    ) {}

    public record RuffRequest(
            List<String> files,
            String workingDir,
            boolean check,
            boolean format,
            boolean jsonOutput,
            String severity,
            List<String> selectRules,
            List<String> ignoreRules,
            boolean fix
    ) {
        public static RuffRequest lint(List<String> files) {
            return new RuffRequest(files, null, false, false, true, null, null, null, false);
        }

        public static RuffRequest lint(List<String> files, String workingDir) {
            return new RuffRequest(files, workingDir, false, false, true, null, null, null, false);
        }
    }

    public record RuffResult(
            boolean success,
            List<RuffIssue> issues,
            String rawOutput,
            long durationMs
    ) {
        public static RuffResult failed(String error) {
            return new RuffResult(false, List.of(), error, 0);
        }

        public int getIssueCount() {
            return issues != null ? issues.size() : 0;
        }

        public long getErrorCount() {
            return issues != null ? issues.stream().filter(i -> i.code().startsWith("E")).count() : 0;
        }

        public long getWarningCount() {
            return issues != null ? issues.stream().filter(i -> i.code().startsWith("W")).count() : 0;
        }
    }

    public record RuffIssue(
            String filePath,
            int line,
            int column,
            String code,
            String message
    ) {}

    public record ProcessResult(
            int exitCode,
            String stdout,
            String stderr
    ) {
        public long durationMs() {
            return 0; // Placeholder
        }
    }

    public static class RustToolException extends RuntimeException {
        public RustToolException(String message) {
            super(message);
        }
    }
}
