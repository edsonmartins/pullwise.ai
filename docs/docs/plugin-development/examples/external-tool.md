# External Tool Plugin

Wrapper for executable external tools.

## Overview

This example shows how to create a plugin that wraps an external command-line tool. This pattern is useful for integrating existing linters, formatters, and analyzers.

## Base Class

```java
package com.pullwise.plugin.external;

import com.pullwise.api.application.service.plugin.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Base class for external tool plugins.
 */
public abstract class ExternalToolPlugin extends AbstractCodeReviewPlugin {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private String toolPath;
    private int timeoutMs;
    private int maxConcurrentProcesses;
    private ExecutorService executor;

    @Override
    protected void doInitialize() throws PluginException {
        this.toolPath = getConfigString("toolPath", getDefaultToolPath());
        this.timeoutMs = getConfigInt("timeout", 30000);
        this.maxConcurrentProcesses = getConfigInt("maxConcurrent", 4);

        // Verify tool is available
        if (!isToolAvailable()) {
            throw new PluginException(
                getId(),
                "Tool not found at: " + toolPath,
                false
            );
        }

        // Create thread pool for concurrent execution
        this.executor = Executors.newFixedThreadPool(maxConcurrentProcesses);

        log.info("External tool plugin initialized: tool={}, timeout={}ms",
            toolPath, timeoutMs);
    }

    /**
     * Get default tool path.
     */
    protected abstract String getDefaultToolPath();

    /**
     * Build command for execution.
     */
    protected abstract List<String> buildCommand(Path inputFile, String originalPath);

    /**
     * Parse tool output into issues.
     */
    protected abstract List<Issue> parseOutput(String output, String originalPath);

    @Override
    protected AnalysisResult doAnalyze(AnalysisRequest request) {
        List<DiffFile> filesToAnalyze = filterSupportedFiles(request.getDiff().getFiles());

        // Process files concurrently
        List<Future<List<Issue>>> futures = new ArrayList<>();

        for (DiffFile file : filesToAnalyze) {
            if (file.getStatus() == FileStatus.DELETED) {
                continue;
            }

            futures.add(executor.submit(() -> analyzeFile(file)));
        }

        // Collect results
        List<Issue> allIssues = new ArrayList<>();

        for (Future<List<Issue>> future : futures) {
            try {
                allIssues.addAll(future.get(timeoutMs, TimeUnit.MILLISECONDS));
            } catch (TimeoutException e) {
                log.warn("Analysis timed out");
            } catch (Exception e) {
                log.error("Analysis failed", e);
            }
        }

        return result(allIssues);
    }

    /**
     * Analyze a single file.
     */
    protected List<Issue> analyzeFile(DiffFile file) {
        Path tempFile = null;

        try {
            // Create temporary file
            tempFile = createTempFile(file);

            // Build and run command
            List<String> command = buildCommand(tempFile, file.getPath());
            String output = executeCommand(command);

            // Parse output
            return output.isEmpty()
                ? Collections.emptyList()
                : parseOutput(output, file.getPath());

        } catch (Exception e) {
            log.warn("Failed to analyze {}: {}", file.getPath(), e.getMessage());
            return Collections.emptyList();
        } finally {
            // Clean up temp file
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                    // Also delete parent directory if empty
                    Files.deleteIfExists(tempFile.getParent());
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    /**
     * Create temporary file with content.
     */
    protected Path createTempFile(DiffFile file) throws IOException {
        String content = file.getLines().stream()
            .map(DiffLine::getContent)
            .reduce((a, b) -> a + "\n" + b)
            .orElse("");

        String extension = file.getExtension();
        if (extension == null) {
            extension = ".txt";
        }

        Path tempDir = Files.createTempDirectory("pullwise-external-");
        Path tempFile = tempDir.resolve("temp" + extension);

        Files.writeString(tempFile, content, StandardCharsets.UTF_8);
        return tempFile;
    }

    /**
     * Execute command and return output.
     */
    protected String executeCommand(List<String> command) throws Exception {
        log.debug("Executing: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Read output with timeout
        StringBuilder output = new StringBuilder();
        long startTime = System.currentTimeMillis();

        try (InputStream is = process.getInputStream();
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(is, StandardCharsets.UTF_8))) {

            char[] buffer = new char[8192];

            while (true) {
                // Check timeout
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > timeoutMs) {
                    process.destroyForcibly();
                    throw new IOException("Process timeout after " + elapsed + "ms");
                }

                // Check if process is done
                try {
                    int exitCode = process.exitValue();
                    // Drain remaining output
                    while (reader.ready()) {
                        int n = reader.read(buffer);
                        if (n > 0) {
                            output.append(buffer, 0, n);
                        }
                    }
                    break;
                } catch (IllegalThreadStateException e) {
                    // Still running, read more data
                }

                // Read with small timeout
                int ch = reader.read();
                if (ch == -1) {
                    break;
                }
                output.append((char) ch);
            }
        }

        int exitCode = process.waitFor();
        log.debug("Process exit code: {}, output length: {}", exitCode, output.length());

        return output.toString();
    }

    /**
     * Check if tool is available.
     */
    protected boolean isToolAvailable() {
        try {
            List<String> versionCommand = Arrays.asList(toolPath, "--version");
            ProcessBuilder pb = new ProcessBuilder(versionCommand);

            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);

            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
```

## Example: ESLint Wrapper

```java
package com.pullwise.plugin.eslint;

import com.pullwise.plugin.external.ExternalToolPlugin;
import com.pullwise.api.application.service.plugin.api.*;
import com.pullwise.api.domain.model.*;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Plugin wrapping ESLint for JavaScript/TypeScript.
 */
public class ESLintPlugin extends ExternalToolPlugin {

    private static final Set<PluginLanguage> SUPPORTED = Set.of(
        PluginLanguage.JAVASCRIPT,
        PluginLanguage.TYPESCRIPT
    );

    private String configPath;
    private Set<String> rules;
    private String format = "stylish";

    @Override
    public String getId() {
        return "com.pullwise.plugin.eslint";
    }

    @Override
    public String getName() {
        return "ESLint";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginType getType() {
        return PluginType.LINTER;
    }

    @Override
    public Set<PluginLanguage> getSupportedLanguages() {
        return SUPPORTED;
    }

    @Override
    protected void doInitialize() throws PluginException {
        // Load ESLint-specific config
        this.configPath = getConfigString("configPath", "");
        this.rules = new HashSet<>(getConfigList("rules"));
        this.format = getConfigString("format", "stylish");

        // Initialize base class
        super.doInitialize();
    }

    @Override
    protected String getDefaultToolPath() {
        return "eslint";
    }

    @Override
    protected List<String> buildCommand(Path inputFile, String originalPath) {
        List<String> command = new ArrayList<>();
        command.add(getConfigString("toolPath", "eslint"));
        command.add(inputFile.toString());
        command.add("--format");
        command.add(format);

        // Add config if specified
        if (!configPath.isEmpty()) {
            command.add("--config");
            command.add(configPath);
        }

        // Add specific rules
        if (!rules.isEmpty()) {
            command.add("--rule");
            command.add(String.join(", ", rules));
        }

        return command;
    }

    @Override
    protected List<Issue> parseOutput(String output, String originalPath) {
        List<Issue> issues = new ArrayList<>();

        // ESLint output varies by format
        // Parse "stylish" format
        Pattern pattern = Pattern.compile(
            "(\\d+):(\\d+)\\s+error\\s+(\\w+)\\s+(.+)\\s+(.+)"
        );

        String[] lines = output.split("\n");
        String currentFile = null;

        for (String line : lines) {
            // File header
            if (line.endsWith(":")) {
                currentFile = line.substring(0, line.length() - 1);
                continue;
            }

            Matcher matcher = pattern.matcher(line.trim());
            if (matcher.find()) {
                int lineNumber = Integer.parseInt(matcher.group(1));
                String rule = matcher.group(3);
                String message = matcher.group(4);
                String context = matcher.groupCount() > 4 ? matcher.group(5) : "";

                issues.add(Issue.builder()
                    .severity(mapESLintSeverity("error"))
                    .type(IssueType.CODE_SMELL)
                    .rule(rule)
                    .filePath(originalPath)
                    .startLine(lineNumber)
                    .message(message)
                    .suggestion(extractSuggestion(message))
                    .codeSnippet(context)
                    .build());
            }
        }

        return issues;
    }

    private Severity mapESLintSeverity(String severity) {
        return "error".equals(severity) ? Severity.HIGH : Severity.MEDIUM;
    }

    private String extractSuggestion(String message) {
        // Extract suggestion from common ESLint messages
        if (message.contains("is assigned a value but never used")) {
            return "Remove the unused variable or use it in your code";
        }
        if (message.contains("Missing semicolon")) {
            return "Add a semicolon at the end of the statement";
        }
        return null;
    }
}
```

## Example: Pylint Wrapper

```java
package com.pullwise.plugin.pylint;

import com.pullwise.plugin.external.ExternalToolPlugin;
import com.pullwise.api.application.service.plugin.api.*;
import com.pullwise.api.domain.model.*;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Plugin wrapping Pylint for Python.
 */
public class PylintPlugin extends ExternalToolPlugin {

    private static final Set<PluginLanguage> SUPPORTED = Set.of(PluginLanguage.PYTHON);

    private double minScore = 8.0;
    private Set<String> disabledChecks = new HashSet<>();

    @Override
    public String getId() {
        return "com.pullwise.plugin.pylint";
    }

    @Override
    public String getName() {
        return "Pylint";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginType getType() {
        return PluginType.LINTER;
    }

    @Override
    public Set<PluginLanguage> getSupportedLanguages() {
        return SUPPORTED;
    }

    @Override
    protected void doInitialize() throws PluginException {
        this.minScore = getConfigDouble("minScore", 8.0);
        this.disabledChecks = new HashSet<>(getConfigList("disabledChecks"));
        super.doInitialize();
    }

    @Override
    protected String getDefaultToolPath() {
        return "pylint";
    }

    @Override
    protected List<String> buildCommand(Path inputFile, String originalPath) {
        List<String> command = new ArrayList<>();
        command.add("pylint");
        command.add(inputFile.toString());
        command.add("--output-format=json");

        // Disable specific checks
        if (!disabledChecks.isEmpty()) {
            command.add("--disable=" + String.join(",", disabledChecks));
        }

        // Set fail-under
        command.add("--fail-under=" + minScore);

        return command;
    }

    @Override
    protected List<Issue> parseOutput(String output, String originalPath) {
        List<Issue> issues = new ArrayList<>();

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();

            List<Map<String, Object>> messages = mapper.readValue(
                output,
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {}
            );

            for (Map<String, Object> msg : messages) {
                String type = (String) msg.get("type");
                String messageId = (String) msg.get("message-id");
                String message = (String) msg.get("message");

                issues.add(Issue.builder()
                    .severity(mapPylintType(type))
                    .type(mapPylintMessageType(messageId))
                    .rule(messageId)
                    .filePath(originalPath)
                    .startLine(((Number) msg.get("line")).intValue())
                    .endLine(getEndLine(msg))
                    .column(
                        getStartColumn(msg),
                        getEndColumn(msg)
                    )
                    .message(message)
                    .documentationUrls(List.of(
                        "https://pylint.pycqa.org/en/latest/messages/" +
                        messageId.toLowerCase().replace("_", "-") + ".html"
                    ))
                    .build());
            }

        } catch (Exception e) {
            log.warn("Failed to parse Pylint output: {}", e.getMessage());
        }

        return issues;
    }

    private Severity mapPylintType(String type) {
        return switch (type) {
            case "error" -> Severity.HIGH;
            case "warning" -> Severity.MEDIUM;
            case "convention", "refactor" -> Severity.LOW;
            default -> Severity.LOW;
        };
    }

    private IssueType mapPylintMessageType(String messageId) {
        if (messageId.startsWith("E")) return IssueType.BUG;
        if (messageId.startsWith("W")) return IssueType.CODE_SMELL;
        if (messageId.startsWith("R")) return IssueType.CODE_SMELL;
        if (messageId.startsWith("C")) return IssueType.CODE_SMELL;
        return IssueType.CODE_SMELL;
    }

    private int getEndLine(Map<String, Object> msg) {
        Object endLine = msg.get("endLine");
        return endLine != null ? ((Number) endLine).intValue() :
            ((Number) msg.get("line")).intValue();
    }

    private int getStartColumn(Map<String, Object> msg) {
        Object column = msg.get("column");
        return column != null ? ((Number) column).intValue() : 0;
    }

    private int getEndColumn(Map<String, Object> msg) {
        Object endColumn = msg.get("endColumn");
        return endColumn != null ? ((Number) endColumn).intValue() :
            getStartColumn(msg);
    }

    private double getConfigDouble(String key, double defaultValue) {
        Object value = getConfig(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
```

## Configuration

```yaml
pullwise:
  plugins:
    eslint:
      enabled: true
      toolPath: eslint
      configPath: /path/to/.eslintrc.json
      rules:
        - no-unused-vars
        - semi
      format: stylish
      timeout: 30000

    pylint:
      enabled: true
      toolPath: pylint
      minScore: 8.0
      disabledChecks:
        - C0301  # line-too-long
        - C0111  # missing-docstring
      maxConcurrent: 4
      timeout: 30000
```

## Best Practices

### 1. Use Thread Pool

```java
// Good: Limit concurrent processes
this.executor = Executors.newFixedThreadPool(maxConcurrentProcesses);

// Bad: Create new process for each file
for (DiffFile file : files) {
    Process p = Runtime.getRuntime().exec(command);
}
```

### 2. Always Clean Up Temp Files

```java
try {
    Path tempFile = createTempFile(file);
    // Use temp file
} finally {
    Files.deleteIfExists(tempFile);
}
```

### 3. Handle Timeouts

```java
long elapsed = System.currentTimeMillis() - startTime;
if (elapsed > timeoutMs) {
    process.destroyForcibly();
    throw new IOException("Process timeout");
}
```

### 4. Parse Output Robustly

```java
try {
    return parseOutput(output);
} catch (Exception e) {
    log.warn("Failed to parse output: {}", e.getMessage());
    return Collections.emptyList();  // Don't fail entire analysis
}
```

## Next Steps

- [Packaging](/docs/plugin-development/packaging/) - Packaging plugins
- [Testing](/docs/plugin-development/testing) - Testing plugins
- [Examples](/docs/plugin-development/examples/) - More examples
