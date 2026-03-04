package com.pullwise.api.application.service.review.pipeline.pass;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.integration.SonarQubeService;
import com.pullwise.api.application.dto.SonarQubeResponse;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.model.Review;
import com.pullwise.api.domain.enums.Severity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Executor de ferramentas SAST.
 *
 * <p>Executa ferramentas de análise estática via CLI ou API e parse os resultados.
 * Cada ferramenta recebe os diffs do PR, escreve os patches em arquivos temporários,
 * executa a ferramenta e faz parse da saída (JSON/XML).
 *
 * <p>Ferramentas suportadas:
 * <ul>
 *   <li><b>Java:</b> Checkstyle (XML), PMD (JSON), SpotBugs (XML)</li>
 *   <li><b>JavaScript/TypeScript:</b> ESLint (JSON), Biome (JSON)</li>
 *   <li><b>Python:</b> Ruff (JSON), Pylint (JSON)</li>
 *   <li><b>Multi-linguagem:</b> SonarQube (REST API)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SastToolExecutor {

    private final SonarQubeService sonarQubeService;
    private final ObjectMapper objectMapper;

    @Value("${pullwise.sast.timeout-seconds:120}")
    private int timeoutSeconds;

    /** Regex to extract added lines from unified diff patches. */
    private static final Pattern HUNK_HEADER = Pattern.compile("^@@\\s+-\\d+(?:,\\d+)?\\s+\\+(\\d+)(?:,(\\d+))?\\s+@@");

    /**
     * Executa uma ferramenta SAST específica.
     */
    public List<SastAggregatorPass.ToolIssue> execute(SastAggregatorPass.SastTool tool,
                                                       PullRequest pullRequest,
                                                       Review review,
                                                       List<GitHubService.FileDiff> diffs) {
        return switch (tool) {
            case SONARQUBE -> executeSonarQube(pullRequest, review);
            case CHECKSTYLE -> executeCheckstyle(pullRequest, review, diffs);
            case PMD -> executePMD(pullRequest, review, diffs);
            case SPOTBUGS -> executeSpotBugs(pullRequest, review, diffs);
            case ESLINT -> executeEslint(pullRequest, review, diffs);
            case BIOME -> executeBiome(pullRequest, review, diffs);
            case RUFF -> executeRuff(pullRequest, review, diffs);
            case PYLINT -> executePylint(pullRequest, review, diffs);
            default -> {
                log.debug("Tool {} not implemented, returning empty results", tool.getName());
                yield List.of();
            }
        };
    }

    // ========== SonarQube (REST API) ==========

    /**
     * Executa SonarQube via API REST.
     * Usa o SonarQubeService existente para buscar issues por PR ou branch.
     */
    private List<SastAggregatorPass.ToolIssue> executeSonarQube(PullRequest pullRequest, Review review) {
        if (!sonarQubeService.isConfigured()) {
            return List.of();
        }

        try {
            String projectKey = buildSonarProjectKey(pullRequest);

            // Tenta buscar issues da PR, depois do branch, depois do projeto
            List<SonarQubeResponse.Issue> sqIssues = sonarQubeService.getPullRequestIssues(
                    projectKey, String.valueOf(pullRequest.getPrNumber()));

            if (sqIssues.isEmpty() && pullRequest.getSourceBranch() != null) {
                sqIssues = sonarQubeService.getBranchIssues(projectKey, pullRequest.getSourceBranch());
            }

            if (sqIssues.isEmpty()) {
                sqIssues = sonarQubeService.getProjectIssues(projectKey);
            }

            log.debug("SonarQube returned {} issues for PR {}", sqIssues.size(), pullRequest.getPrNumber());

            return sqIssues.stream().map(sq -> SastAggregatorPass.ToolIssue.builder()
                    .filePath(extractSonarFilePath(sq.getComponent()))
                    .line(sq.getTextRange() != null ? sq.getTextRange().getStartLine() : null)
                    .endLine(sq.getTextRange() != null ? sq.getTextRange().getEndLine() : null)
                    .severity(mapSonarSeverity(sq.getSeverity()))
                    .title(sq.getMessage())
                    .description(sq.getMessage())
                    .rule(sq.getRule())
                    .category(sq.getType() != null ? sq.getType().toLowerCase() : "quality")
                    .build()
            ).collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("SonarQube analysis failed: {}", e.getMessage());
            return List.of();
        }
    }

    // ========== Checkstyle (Java — XML output) ==========

    /**
     * Executa Checkstyle via CLI: java -jar checkstyle.jar -c /google_checks.xml -f xml files/
     * Parse do XML de saída para extrair violations.
     */
    private List<SastAggregatorPass.ToolIssue> executeCheckstyle(PullRequest pullRequest,
                                                                   Review review,
                                                                   List<GitHubService.FileDiff> diffs) {
        List<GitHubService.FileDiff> javaFiles = filterByExtension(diffs, ".java");
        if (javaFiles.isEmpty()) return List.of();

        Path tempDir = null;
        try {
            tempDir = prepareTempFiles(javaFiles);
            if (!isToolAvailable("checkstyle")) {
                log.debug("Checkstyle not available on PATH, skipping");
                return List.of();
            }

            String[] cmd = {"checkstyle", "-c", "/google_checks.xml", "-f", "xml", tempDir.toString()};
            String output = executeCommand(cmd, tempDir.toFile());

            return parseCheckstyleXml(output, javaFiles);

        } catch (Exception e) {
            log.warn("Checkstyle execution failed: {}", e.getMessage());
            return List.of();
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    private List<SastAggregatorPass.ToolIssue> parseCheckstyleXml(String xml,
                                                                    List<GitHubService.FileDiff> diffs) {
        List<SastAggregatorPass.ToolIssue> issues = new ArrayList<>();
        Map<String, String> tempToOriginal = buildTempToOriginalMap(diffs);

        try {
            // Simple XML parsing — Checkstyle XML format:
            // <file name="path"><error line="10" column="5" severity="warning" message="..." source="..."/></file>
            Pattern filePattern = Pattern.compile("<file\\s+name=\"([^\"]+)\"");
            Pattern errorPattern = Pattern.compile(
                    "<error\\s+line=\"(\\d+)\"(?:\\s+column=\"(\\d+)\")?\\s+severity=\"(\\w+)\"\\s+message=\"([^\"]+)\"(?:\\s+source=\"([^\"]+)\")?"
            );

            String currentFile = null;
            for (String line : xml.split("\n")) {
                Matcher fileMatcher = filePattern.matcher(line);
                if (fileMatcher.find()) {
                    currentFile = fileMatcher.group(1);
                }

                Matcher errorMatcher = errorPattern.matcher(line);
                if (errorMatcher.find() && currentFile != null) {
                    String originalPath = resolveOriginalPath(currentFile, tempToOriginal);
                    issues.add(SastAggregatorPass.ToolIssue.builder()
                            .filePath(originalPath)
                            .line(Integer.parseInt(errorMatcher.group(1)))
                            .severity(mapCheckstyleSeverity(errorMatcher.group(3)))
                            .title(unescapeXml(errorMatcher.group(4)))
                            .description(unescapeXml(errorMatcher.group(4)))
                            .rule(errorMatcher.group(5))
                            .category("style")
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing Checkstyle XML output: {}", e.getMessage());
        }

        return issues;
    }

    // ========== PMD (Java — JSON output) ==========

    /**
     * Executa PMD via CLI: pmd check -d path -R rulesets/java/quickstart.xml -f json
     * Parse do JSON de saída.
     */
    private List<SastAggregatorPass.ToolIssue> executePMD(PullRequest pullRequest,
                                                            Review review,
                                                            List<GitHubService.FileDiff> diffs) {
        List<GitHubService.FileDiff> javaFiles = filterByExtension(diffs, ".java");
        if (javaFiles.isEmpty()) return List.of();

        Path tempDir = null;
        try {
            tempDir = prepareTempFiles(javaFiles);
            if (!isToolAvailable("pmd")) {
                log.debug("PMD not available on PATH, skipping");
                return List.of();
            }

            String[] cmd = {"pmd", "check", "-d", tempDir.toString(),
                    "-R", "rulesets/java/quickstart.xml", "-f", "json", "--no-cache"};
            String output = executeCommand(cmd, tempDir.toFile());

            return parsePmdJson(output, diffs);

        } catch (Exception e) {
            log.warn("PMD execution failed: {}", e.getMessage());
            return List.of();
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    private List<SastAggregatorPass.ToolIssue> parsePmdJson(String json,
                                                              List<GitHubService.FileDiff> diffs) {
        List<SastAggregatorPass.ToolIssue> issues = new ArrayList<>();
        Map<String, String> tempToOriginal = buildTempToOriginalMap(diffs);

        try {
            // PMD JSON format: { "files": [{ "filename": "...", "violations": [{ "beginline", "endline", "description", "rule", "ruleset", "priority" }] }] }
            JsonNode root = objectMapper.readTree(json);
            JsonNode filesNode = root.get("files");
            if (filesNode == null || !filesNode.isArray()) return issues;

            for (JsonNode fileNode : filesNode) {
                String filename = fileNode.has("filename") ? fileNode.get("filename").asText() : null;
                String originalPath = resolveOriginalPath(filename, tempToOriginal);

                JsonNode violations = fileNode.get("violations");
                if (violations == null || !violations.isArray()) continue;

                for (JsonNode v : violations) {
                    issues.add(SastAggregatorPass.ToolIssue.builder()
                            .filePath(originalPath)
                            .line(v.has("beginline") ? v.get("beginline").asInt() : null)
                            .endLine(v.has("endline") ? v.get("endline").asInt() : null)
                            .severity(mapPmdPriority(v.has("priority") ? v.get("priority").asInt() : 3))
                            .title(v.has("description") ? v.get("description").asText() : "PMD violation")
                            .description(v.has("description") ? v.get("description").asText() : "")
                            .rule(v.has("rule") ? v.get("rule").asText() : null)
                            .category("quality")
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing PMD JSON output: {}", e.getMessage());
        }

        return issues;
    }

    // ========== SpotBugs (Java — XML output) ==========

    /**
     * Executa SpotBugs via CLI: spotbugs -textui -xml -output report.xml path
     * Parse do XML de saída.
     */
    private List<SastAggregatorPass.ToolIssue> executeSpotBugs(PullRequest pullRequest,
                                                                 Review review,
                                                                 List<GitHubService.FileDiff> diffs) {
        List<GitHubService.FileDiff> javaFiles = filterByExtension(diffs, ".java");
        if (javaFiles.isEmpty()) return List.of();

        Path tempDir = null;
        try {
            tempDir = prepareTempFiles(javaFiles);
            if (!isToolAvailable("spotbugs")) {
                log.debug("SpotBugs not available on PATH, skipping");
                return List.of();
            }

            Path reportFile = tempDir.resolve("spotbugs-report.xml");
            String[] cmd = {"spotbugs", "-textui", "-xml", "-output", reportFile.toString(),
                    tempDir.toString()};
            executeCommand(cmd, tempDir.toFile());

            if (Files.exists(reportFile)) {
                String output = Files.readString(reportFile);
                return parseSpotBugsXml(output, diffs);
            }

            return List.of();

        } catch (Exception e) {
            log.warn("SpotBugs execution failed: {}", e.getMessage());
            return List.of();
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    private List<SastAggregatorPass.ToolIssue> parseSpotBugsXml(String xml,
                                                                   List<GitHubService.FileDiff> diffs) {
        List<SastAggregatorPass.ToolIssue> issues = new ArrayList<>();
        Map<String, String> tempToOriginal = buildTempToOriginalMap(diffs);

        try {
            // SpotBugs XML: <BugInstance type="..." priority="..." category="...">
            //   <ShortMessage>...</ShortMessage><LongMessage>...</LongMessage>
            //   <SourceLine classname="..." start="..." end="..." sourcepath="..."/>
            // </BugInstance>
            Pattern bugPattern = Pattern.compile(
                    "<BugInstance\\s+type=\"([^\"]+)\"\\s+priority=\"(\\d+)\"(?:\\s+category=\"([^\"]+)\")?"
            );
            Pattern sourceLinePattern = Pattern.compile(
                    "<SourceLine[^>]*\\sstart=\"(\\d+)\"[^>]*\\send=\"(\\d+)\"[^>]*\\ssourcepath=\"([^\"]+)\""
            );
            Pattern messagePattern = Pattern.compile("<ShortMessage>([^<]+)</ShortMessage>");

            String[] blocks = xml.split("</BugInstance>");
            for (String block : blocks) {
                Matcher bugMatcher = bugPattern.matcher(block);
                if (!bugMatcher.find()) continue;

                String type = bugMatcher.group(1);
                int priority = Integer.parseInt(bugMatcher.group(2));
                String category = bugMatcher.group(3);

                Matcher srcMatcher = sourceLinePattern.matcher(block);
                Integer startLine = null;
                Integer endLine = null;
                String sourcePath = null;
                if (srcMatcher.find()) {
                    startLine = Integer.parseInt(srcMatcher.group(1));
                    endLine = Integer.parseInt(srcMatcher.group(2));
                    sourcePath = srcMatcher.group(3);
                }

                Matcher msgMatcher = messagePattern.matcher(block);
                String message = msgMatcher.find() ? msgMatcher.group(1) : type;

                String originalPath = resolveOriginalPath(sourcePath, tempToOriginal);

                issues.add(SastAggregatorPass.ToolIssue.builder()
                        .filePath(originalPath)
                        .line(startLine)
                        .endLine(endLine)
                        .severity(mapSpotBugsPriority(priority))
                        .title(message)
                        .description(type + ": " + message)
                        .rule("spotbugs:" + type)
                        .category(category != null ? category.toLowerCase() : "bug")
                        .build());
            }
        } catch (Exception e) {
            log.warn("Error parsing SpotBugs XML output: {}", e.getMessage());
        }

        return issues;
    }

    // ========== ESLint (JavaScript/TypeScript — JSON output) ==========

    /**
     * Executa ESLint via CLI: npx eslint --format json path
     * Parse do JSON: [{ filePath, messages: [{ severity, message, ruleId, line }] }]
     */
    private List<SastAggregatorPass.ToolIssue> executeEslint(PullRequest pullRequest,
                                                               Review review,
                                                               List<GitHubService.FileDiff> diffs) {
        List<GitHubService.FileDiff> jsFiles = filterByExtensions(diffs, List.of(
                ".js", ".jsx", ".ts", ".tsx", ".mjs", ".cjs"));
        if (jsFiles.isEmpty()) return List.of();

        Path tempDir = null;
        try {
            tempDir = prepareTempFiles(jsFiles);
            if (!isToolAvailable("eslint") && !isToolAvailable("npx")) {
                log.debug("ESLint not available on PATH, skipping");
                return List.of();
            }

            String tool = isToolAvailable("eslint") ? "eslint" : "npx";
            String[] cmd;
            if ("npx".equals(tool)) {
                cmd = new String[]{"npx", "eslint", "--format", "json", "--no-eslintrc", tempDir.toString()};
            } else {
                cmd = new String[]{"eslint", "--format", "json", "--no-eslintrc", tempDir.toString()};
            }
            String output = executeCommand(cmd, tempDir.toFile());

            return parseEslintJson(output, diffs);

        } catch (Exception e) {
            log.warn("ESLint execution failed: {}", e.getMessage());
            return List.of();
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    private List<SastAggregatorPass.ToolIssue> parseEslintJson(String json,
                                                                 List<GitHubService.FileDiff> diffs) {
        List<SastAggregatorPass.ToolIssue> issues = new ArrayList<>();
        Map<String, String> tempToOriginal = buildTempToOriginalMap(diffs);

        try {
            // ESLint JSON: [{ "filePath": "...", "messages": [{ "ruleId", "severity", "message", "line", "endLine" }] }]
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) return issues;

            for (JsonNode fileNode : root) {
                String filePath = fileNode.has("filePath") ? fileNode.get("filePath").asText() : null;
                String originalPath = resolveOriginalPath(filePath, tempToOriginal);

                JsonNode messages = fileNode.get("messages");
                if (messages == null || !messages.isArray()) continue;

                for (JsonNode msg : messages) {
                    int severity = msg.has("severity") ? msg.get("severity").asInt() : 1;
                    issues.add(SastAggregatorPass.ToolIssue.builder()
                            .filePath(originalPath)
                            .line(msg.has("line") ? msg.get("line").asInt() : null)
                            .endLine(msg.has("endLine") ? msg.get("endLine").asInt() : null)
                            .severity(mapEslintSeverity(severity))
                            .title(msg.has("message") ? msg.get("message").asText() : "ESLint violation")
                            .description(msg.has("message") ? msg.get("message").asText() : "")
                            .rule(msg.has("ruleId") && !msg.get("ruleId").isNull()
                                    ? msg.get("ruleId").asText() : null)
                            .category("style")
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing ESLint JSON output: {}", e.getMessage());
        }

        return issues;
    }

    // ========== Biome (JavaScript/TypeScript — JSON output) ==========

    /**
     * Executa Biome via CLI: biome lint --reporter json path
     * Parse do JSON diagnostics.
     */
    private List<SastAggregatorPass.ToolIssue> executeBiome(PullRequest pullRequest,
                                                              Review review,
                                                              List<GitHubService.FileDiff> diffs) {
        List<GitHubService.FileDiff> jsFiles = filterByExtensions(diffs, List.of(
                ".js", ".jsx", ".ts", ".tsx", ".mjs", ".cjs"));
        if (jsFiles.isEmpty()) return List.of();

        Path tempDir = null;
        try {
            tempDir = prepareTempFiles(jsFiles);
            if (!isToolAvailable("biome")) {
                log.debug("Biome not available on PATH, skipping");
                return List.of();
            }

            String[] cmd = {"biome", "lint", "--reporter", "json", tempDir.toString()};
            String output = executeCommand(cmd, tempDir.toFile());

            return parseBiomeJson(output, diffs);

        } catch (Exception e) {
            log.warn("Biome execution failed: {}", e.getMessage());
            return List.of();
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    private List<SastAggregatorPass.ToolIssue> parseBiomeJson(String json,
                                                                List<GitHubService.FileDiff> diffs) {
        List<SastAggregatorPass.ToolIssue> issues = new ArrayList<>();
        Map<String, String> tempToOriginal = buildTempToOriginalMap(diffs);

        try {
            // Biome JSON: { "diagnostics": [{ "category": "...", "severity": "...", "description": "...",
            //   "location": { "path": { "file": "..." }, "span": { "start": N, "end": N } } }] }
            JsonNode root = objectMapper.readTree(json);
            JsonNode diagnostics = root.get("diagnostics");
            if (diagnostics == null || !diagnostics.isArray()) return issues;

            for (JsonNode diag : diagnostics) {
                String filePath = null;
                Integer line = null;

                JsonNode location = diag.get("location");
                if (location != null) {
                    JsonNode pathNode = location.get("path");
                    if (pathNode != null && pathNode.has("file")) {
                        filePath = pathNode.get("file").asText();
                    }
                }

                String originalPath = resolveOriginalPath(filePath, tempToOriginal);
                String category = diag.has("category") ? diag.get("category").asText() : "lint";
                String severityStr = diag.has("severity") ? diag.get("severity").asText() : "warning";
                String description = diag.has("description") ? diag.get("description").asText() : "";
                // Biome may use a nested message object
                if (description.isEmpty() && diag.has("message")) {
                    JsonNode msgNode = diag.get("message");
                    description = msgNode.isTextual() ? msgNode.asText() : msgNode.toString();
                }

                issues.add(SastAggregatorPass.ToolIssue.builder()
                        .filePath(originalPath)
                        .line(line)
                        .severity(mapBiomeSeverity(severityStr))
                        .title(description.length() > 120 ? description.substring(0, 120) + "..." : description)
                        .description(description)
                        .rule(category)
                        .category("style")
                        .build());
            }
        } catch (Exception e) {
            log.warn("Error parsing Biome JSON output: {}", e.getMessage());
        }

        return issues;
    }

    // ========== Ruff (Python — JSON output) ==========

    /**
     * Executa Ruff via CLI: ruff check --output-format json path
     * Parse do JSON: [{ "code", "message", "filename", "location": { "row", "column" } }]
     */
    private List<SastAggregatorPass.ToolIssue> executeRuff(PullRequest pullRequest,
                                                             Review review,
                                                             List<GitHubService.FileDiff> diffs) {
        List<GitHubService.FileDiff> pyFiles = filterByExtensions(diffs, List.of(".py", ".pyi"));
        if (pyFiles.isEmpty()) return List.of();

        Path tempDir = null;
        try {
            tempDir = prepareTempFiles(pyFiles);
            if (!isToolAvailable("ruff")) {
                log.debug("Ruff not available on PATH, skipping");
                return List.of();
            }

            String[] cmd = {"ruff", "check", "--output-format", "json", "--no-cache", tempDir.toString()};
            String output = executeCommand(cmd, tempDir.toFile());

            return parseRuffJson(output, diffs);

        } catch (Exception e) {
            log.warn("Ruff execution failed: {}", e.getMessage());
            return List.of();
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    private List<SastAggregatorPass.ToolIssue> parseRuffJson(String json,
                                                               List<GitHubService.FileDiff> diffs) {
        List<SastAggregatorPass.ToolIssue> issues = new ArrayList<>();
        Map<String, String> tempToOriginal = buildTempToOriginalMap(diffs);

        try {
            // Ruff JSON: [{ "code": "E501", "message": "...", "filename": "...",
            //   "location": { "row": 10, "column": 5 }, "end_location": { "row": 10, "column": 80 } }]
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) return issues;

            for (JsonNode item : root) {
                String filename = item.has("filename") ? item.get("filename").asText() : null;
                String originalPath = resolveOriginalPath(filename, tempToOriginal);

                Integer row = null;
                Integer endRow = null;
                if (item.has("location")) {
                    JsonNode loc = item.get("location");
                    row = loc.has("row") ? loc.get("row").asInt() : null;
                }
                if (item.has("end_location")) {
                    JsonNode endLoc = item.get("end_location");
                    endRow = endLoc.has("row") ? endLoc.get("row").asInt() : null;
                }

                String code = item.has("code") ? item.get("code").asText() : null;
                String message = item.has("message") ? item.get("message").asText() : "Ruff violation";

                issues.add(SastAggregatorPass.ToolIssue.builder()
                        .filePath(originalPath)
                        .line(row)
                        .endLine(endRow)
                        .severity(mapRuffCode(code))
                        .title(message)
                        .description(code + ": " + message)
                        .rule(code)
                        .category("style")
                        .build());
            }
        } catch (Exception e) {
            log.warn("Error parsing Ruff JSON output: {}", e.getMessage());
        }

        return issues;
    }

    // ========== Pylint (Python — JSON output) ==========

    /**
     * Executa Pylint via CLI: pylint --output-format json path
     * Parse do JSON: [{ "type", "module", "obj", "line", "column", "message", "message-id" }]
     */
    private List<SastAggregatorPass.ToolIssue> executePylint(PullRequest pullRequest,
                                                               Review review,
                                                               List<GitHubService.FileDiff> diffs) {
        List<GitHubService.FileDiff> pyFiles = filterByExtensions(diffs, List.of(".py", ".pyi"));
        if (pyFiles.isEmpty()) return List.of();

        Path tempDir = null;
        try {
            tempDir = prepareTempFiles(pyFiles);
            if (!isToolAvailable("pylint")) {
                log.debug("Pylint not available on PATH, skipping");
                return List.of();
            }

            // Collect all .py files in temp dir
            List<String> pyPaths = new ArrayList<>();
            Files.walk(tempDir).filter(p -> p.toString().endsWith(".py"))
                    .forEach(p -> pyPaths.add(p.toString()));

            if (pyPaths.isEmpty()) return List.of();

            List<String> cmdList = new ArrayList<>();
            cmdList.add("pylint");
            cmdList.add("--output-format");
            cmdList.add("json");
            cmdList.add("--disable=C0114,C0115,C0116"); // Skip missing docstrings for partial files
            cmdList.addAll(pyPaths);

            String output = executeCommand(cmdList.toArray(new String[0]), tempDir.toFile());

            return parsePylintJson(output, diffs);

        } catch (Exception e) {
            log.warn("Pylint execution failed: {}", e.getMessage());
            return List.of();
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    private List<SastAggregatorPass.ToolIssue> parsePylintJson(String json,
                                                                 List<GitHubService.FileDiff> diffs) {
        List<SastAggregatorPass.ToolIssue> issues = new ArrayList<>();
        Map<String, String> tempToOriginal = buildTempToOriginalMap(diffs);

        try {
            // Pylint JSON: [{ "type": "convention|refactor|warning|error|fatal",
            //   "module": "...", "obj": "...", "line": 10, "column": 0,
            //   "message": "...", "message-id": "C0301", "symbol": "line-too-long" }]
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) return issues;

            for (JsonNode item : root) {
                String path = item.has("path") ? item.get("path").asText() : null;
                String originalPath = resolveOriginalPath(path, tempToOriginal);

                String type = item.has("type") ? item.get("type").asText() : "convention";
                String messageId = item.has("message-id") ? item.get("message-id").asText() : null;
                String symbol = item.has("symbol") ? item.get("symbol").asText() : null;
                String message = item.has("message") ? item.get("message").asText() : "Pylint violation";
                Integer line = item.has("line") ? item.get("line").asInt() : null;

                String rule = messageId != null ? messageId : symbol;

                issues.add(SastAggregatorPass.ToolIssue.builder()
                        .filePath(originalPath)
                        .line(line)
                        .severity(mapPylintType(type))
                        .title(message)
                        .description((symbol != null ? symbol + ": " : "") + message)
                        .rule(rule)
                        .category("quality")
                        .build());
            }
        } catch (Exception e) {
            log.warn("Error parsing Pylint JSON output: {}", e.getMessage());
        }

        return issues;
    }

    // ========== Utility Methods ==========

    /**
     * Prepara arquivos temporários a partir dos diffs.
     * Extrai as linhas adicionadas de cada patch e grava em arquivos no temp dir,
     * preservando a estrutura de diretórios original.
     */
    private Path prepareTempFiles(List<GitHubService.FileDiff> diffs) throws IOException {
        Path tempDir = Files.createTempDirectory("pullwise-sast-");

        for (GitHubService.FileDiff diff : diffs) {
            if (diff.patch() == null || diff.patch().isBlank()) continue;

            // Preservar estrutura de diretórios
            Path filePath = tempDir.resolve(diff.filename());
            Files.createDirectories(filePath.getParent());

            // Extrair conteúdo reconstruído do patch (linhas adicionadas + contexto)
            String content = extractContentFromPatch(diff.patch());
            Files.writeString(filePath, content);
        }

        return tempDir;
    }

    /**
     * Extrai conteúdo aproximado do arquivo a partir do unified diff patch.
     * Usa linhas de contexto e linhas adicionadas para reconstruir o conteúdo.
     */
    private String extractContentFromPatch(String patch) {
        StringBuilder content = new StringBuilder();

        for (String line : patch.split("\n")) {
            if (line.startsWith("+++") || line.startsWith("---")) {
                continue; // Skip diff headers
            }
            if (line.startsWith("@@")) {
                continue; // Skip hunk headers
            }
            if (line.startsWith("-")) {
                continue; // Skip removed lines
            }
            if (line.startsWith("+")) {
                content.append(line.substring(1)).append("\n"); // Added lines
            } else if (!line.startsWith("\\")) {
                content.append(line.length() > 0 ? line.substring(Math.min(1, line.length())) : "").append("\n"); // Context lines
            }
        }

        return content.toString();
    }

    /**
     * Verifica se uma ferramenta está disponível no PATH.
     */
    private boolean isToolAvailable(String tool) {
        try {
            String[] cmd = System.getProperty("os.name").toLowerCase().contains("win")
                    ? new String[]{"where", tool}
                    : new String[]{"which", tool};
            Process process = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Executa um comando CLI e retorna o output.
     */
    private String executeCommand(String[] cmd, File workingDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Command timed out after " + timeoutSeconds + "s");
        }

        int exitCode = process.exitValue();
        // Exit code 1 is normal for linters (issues found)
        if (exitCode != 0 && exitCode != 1 && exitCode != 2) {
            log.debug("Command {} exited with code {} — output: {}", cmd[0], exitCode,
                    output.length() > 500 ? output.substring(0, 500) : output);
        }

        return output.toString();
    }

    /**
     * Limpa o diretório temporário.
     */
    private void cleanupTempDir(Path tempDir) {
        if (tempDir == null) return;
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (IOException e) { log.trace("Failed to delete temp file: {}", path); }
                    });
        } catch (IOException e) {
            log.debug("Failed to cleanup temp dir: {}", tempDir);
        }
    }

    /**
     * Filtra diffs por extensão de arquivo.
     */
    private List<GitHubService.FileDiff> filterByExtension(List<GitHubService.FileDiff> diffs, String ext) {
        return diffs.stream()
                .filter(d -> d.filename().toLowerCase().endsWith(ext))
                .filter(d -> !"removed".equals(d.status()))
                .collect(Collectors.toList());
    }

    /**
     * Filtra diffs por múltiplas extensões.
     */
    private List<GitHubService.FileDiff> filterByExtensions(List<GitHubService.FileDiff> diffs,
                                                              List<String> extensions) {
        return diffs.stream()
                .filter(d -> {
                    String lower = d.filename().toLowerCase();
                    return extensions.stream().anyMatch(lower::endsWith);
                })
                .filter(d -> !"removed".equals(d.status()))
                .collect(Collectors.toList());
    }

    /**
     * Constrói mapa de caminhos temporários para caminhos originais.
     */
    private Map<String, String> buildTempToOriginalMap(List<GitHubService.FileDiff> diffs) {
        Map<String, String> map = new HashMap<>();
        for (GitHubService.FileDiff diff : diffs) {
            // O nome do arquivo no temp dir terá o mesmo path relativo
            String basename = Path.of(diff.filename()).getFileName().toString();
            map.put(basename, diff.filename());
            map.put(diff.filename(), diff.filename());
        }
        return map;
    }

    /**
     * Resolve o caminho original do arquivo a partir do caminho temporário.
     */
    private String resolveOriginalPath(String tempPath, Map<String, String> tempToOriginal) {
        if (tempPath == null) return null;

        // Tentar match direto
        String original = tempToOriginal.get(tempPath);
        if (original != null) return original;

        // Tentar pelo basename
        String basename = Path.of(tempPath).getFileName().toString();
        original = tempToOriginal.get(basename);
        if (original != null) return original;

        // Tentar match por sufixo
        for (Map.Entry<String, String> entry : tempToOriginal.entrySet()) {
            if (tempPath.endsWith(entry.getKey()) || tempPath.endsWith(entry.getValue())) {
                return entry.getValue();
            }
        }

        return tempPath; // Fallback: retorna o path do temp
    }

    // ========== SonarQube helpers ==========

    private String buildSonarProjectKey(PullRequest pullRequest) {
        if (pullRequest.getProject() != null && pullRequest.getProject().getRepositoryUrl() != null) {
            String url = pullRequest.getProject().getRepositoryUrl();
            String[] parts = url.split("/");
            if (parts.length >= 2) {
                String owner = parts[parts.length - 2].replaceAll("[^a-zA-Z0-9_-]", "");
                String repo = parts[parts.length - 1].replace(".git", "").replaceAll("[^a-zA-Z0-9_-]", "");
                return owner + "_" + repo;
            }
        }
        return pullRequest.getProject() != null
                ? pullRequest.getProject().getName().replaceAll("[^a-zA-Z0-9_-]", "_")
                : "unknown";
    }

    private String extractSonarFilePath(String component) {
        if (component == null) return null;
        String[] parts = component.split(":");
        if (parts.length >= 3) {
            return component.substring(component.indexOf(":", component.indexOf(":") + 1) + 1);
        } else if (parts.length == 2) {
            return parts[1];
        }
        return component;
    }

    // ========== Severity Mappers ==========

    private Severity mapSonarSeverity(String sonarSeverity) {
        if (sonarSeverity == null) return Severity.MEDIUM;
        return switch (sonarSeverity.toUpperCase()) {
            case "BLOCKER", "CRITICAL" -> Severity.CRITICAL;
            case "MAJOR" -> Severity.HIGH;
            case "MINOR" -> Severity.MEDIUM;
            case "INFO" -> Severity.LOW;
            default -> Severity.MEDIUM;
        };
    }

    private Severity mapCheckstyleSeverity(String severity) {
        if (severity == null) return Severity.MEDIUM;
        return switch (severity.toLowerCase()) {
            case "error" -> Severity.HIGH;
            case "warning" -> Severity.MEDIUM;
            case "info" -> Severity.LOW;
            default -> Severity.MEDIUM;
        };
    }

    private Severity mapPmdPriority(int priority) {
        return switch (priority) {
            case 1 -> Severity.CRITICAL;
            case 2 -> Severity.HIGH;
            case 3 -> Severity.MEDIUM;
            case 4 -> Severity.LOW;
            case 5 -> Severity.INFO;
            default -> Severity.MEDIUM;
        };
    }

    private Severity mapSpotBugsPriority(int priority) {
        return switch (priority) {
            case 1 -> Severity.HIGH;
            case 2 -> Severity.MEDIUM;
            case 3 -> Severity.LOW;
            default -> Severity.MEDIUM;
        };
    }

    private Severity mapEslintSeverity(int severity) {
        // ESLint: 1 = warning, 2 = error
        return severity >= 2 ? Severity.HIGH : Severity.MEDIUM;
    }

    private Severity mapBiomeSeverity(String severity) {
        if (severity == null) return Severity.MEDIUM;
        return switch (severity.toLowerCase()) {
            case "error" -> Severity.HIGH;
            case "warning" -> Severity.MEDIUM;
            case "information", "hint" -> Severity.LOW;
            default -> Severity.MEDIUM;
        };
    }

    private Severity mapRuffCode(String code) {
        if (code == null) return Severity.MEDIUM;
        // Security-related rules
        if (code.startsWith("S")) return Severity.HIGH;
        // Error rules
        if (code.startsWith("E") || code.startsWith("F")) return Severity.MEDIUM;
        // Warning/convention
        if (code.startsWith("W") || code.startsWith("C")) return Severity.LOW;
        return Severity.MEDIUM;
    }

    private Severity mapPylintType(String type) {
        if (type == null) return Severity.MEDIUM;
        return switch (type.toLowerCase()) {
            case "fatal", "error" -> Severity.HIGH;
            case "warning" -> Severity.MEDIUM;
            case "refactor" -> Severity.LOW;
            case "convention" -> Severity.INFO;
            default -> Severity.MEDIUM;
        };
    }

    private String unescapeXml(String text) {
        if (text == null) return null;
        return text.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }
}
