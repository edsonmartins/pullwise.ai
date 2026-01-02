# Rust Tool Integration

Integrating Rust-based tools like Biome and Ruff.

## Overview

Pullwise includes built-in support for running Rust-based analysis tools. This guide shows how to create plugins that wrap external Rust executables.

## Supported Tools

| Tool | Language | Purpose |
|------|----------|---------|
| **Biome** | JavaScript/TypeScript | Linting, formatting |
| **Ruff** | Python | Linting, formatting |

## Biome Plugin

### Plugin Implementation

```java
package com.pullwise.plugin.biome;

import com.pullwise.api.application.service.plugin.api.*;
import com.pullwise.api.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Plugin that wraps Biome for JavaScript/TypeScript analysis.
 */
public class BiomePlugin extends AbstractCodeReviewPlugin {

    private static final Logger log = LoggerFactory.getLogger(BiomePlugin.class);
    private static final Set<PluginLanguage> SUPPORTED = Set.of(
        PluginLanguage.JAVASCRIPT,
        PluginLanguage.TYPESCRIPT
    );

    private String biomePath = "biome";
    private boolean checkOnly = true;
    private int timeoutMs = 30000;

    @Override
    public String getId() {
        return "com.pullwise.plugin.biome";
    }

    @Override
    public String getName() {
        return "Biome Linter";
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
        // Load configuration
        biomePath = getConfigString("path", "biome");
        checkOnly = getConfigBoolean("checkOnly", true);
        timeoutMs = getConfigInt("timeout", 30000);

        // Verify Biome is available
        if (!isBiomeAvailable()) {
            throw new PluginException(
                getId(),
                "Biome not found at: " + biomePath,
                false
            );
        }

        log.info("Biome plugin initialized: path={}, checkOnly={}",
            biomePath, checkOnly);
    }

    @Override
    protected AnalysisResult doAnalyze(AnalysisRequest request) {
        List<Issue> issues = new ArrayList<>();

        for (DiffFile file : filterSupportedFiles(request.getDiff().getFiles())) {
            // Skip deleted files
            if (file.getStatus() == FileStatus.DELETED) {
                continue;
            }

            // Write file to temporary directory
            Path tempFile = null;
            try {
                tempFile = createTempFile(file);

                // Run Biome on the file
                List<Issue> fileIssues = runBiome(tempFile, file.getPath());
                issues.addAll(fileIssues);

            } catch (Exception e) {
                log.warn("Failed to analyze {}: {}",
                    file.getPath(), e.getMessage());
            } finally {
                // Clean up temp file
                if (tempFile != null) {
                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }

        return result(issues);
    }

    /**
     * Create temporary file with content.
     */
    private Path createTempFile(DiffFile file) throws IOException {
        String content = file.getLines().stream()
            .map(DiffLine::getContent)
            .collect(Collectors.joining("\n"));

        String extension = file.getExtension();
        Path tempDir = Files.createTempDirectory("biome-");
        Path tempFile = tempDir.resolve("temp" + extension);

        Files.writeString(tempFile, content, StandardCharsets.UTF_8);
        return tempFile;
    }

    /**
     * Run Biome and parse output.
     */
    private List<Issue> runBiome(Path file, String originalPath) {
        List<String> command = buildCommand(file);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read output with timeout
            String output = readOutput(process, timeoutMs);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return Collections.emptyList();
            }

            return parseBiomeOutput(output, originalPath);

        } catch (Exception e) {
            log.error("Failed to run Biome", e);
            return Collections.emptyList();
        }
    }

    /**
     * Build Biome command.
     */
    private List<String> buildCommand(Path file) {
        List<String> command = new ArrayList<>();
        command.add(biomePath);

        if (checkOnly) {
            command.add("check");
        } else {
            command.add("lint");
        }

        command.add(file.toString());
        command.add("--json");
        command.add("--diagnostic-level=warn");

        return command;
    }

    /**
     * Read process output with timeout.
     */
    private String readOutput(Process process, long timeoutMs)
            throws IOException, InterruptedException {
        StringBuilder output = new StringBuilder();

        try (InputStream is = process.getInputStream();
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(is, StandardCharsets.UTF_8))) {

            char[] buffer = new char[8192];
            long start = System.currentTimeMillis();

            while (true) {
                // Check timeout
                if (System.currentTimeMillis() - start > timeoutMs) {
                    process.destroyForcibly();
                    throw new IOException("Process timeout");
                }

                int ch = reader.read();
                if (ch == -1) break;

                output.append((char) ch);

                // Check if process is done
                try {
                    process.exitValue();
                    // Process has exited, drain remaining output
                    while (reader.ready()) {
                        ch = reader.read();
                        if (ch == -1) break;
                        output.append((char) ch);
                    }
                    break;
                } catch (IllegalThreadStateException e) {
                    // Still running
                }
            }
        }

        return output.toString();
    }

    /**
     * Parse Biome JSON output.
     */
    @SuppressWarnings("unchecked")
    private List<Issue> parseBiomeOutput(String output, String originalPath) {
        List<Issue> issues = new ArrayList<>();

        try {
            // Biome output format
            Map<String, Object> json = parseJson(output);
            List<Map<String, Object>> diagnostics =
                (List<Map<String, Object>>) json.get("diagnostics");

            if (diagnostics == null) {
                return issues;
            }

            for (Map<String, Object> diag : diagnostics) {
                Map<String, Object> location =
                    (Map<String, Object>) diag.get("location");

                Map<String, Object> position =
                    (Map<String, Object>) location.get("source");

                Issue issue = Issue.builder()
                    .severity(mapSeverity((String) diag.get("severity")))
                    .type(IssueType.CODE_SMELL)
                    .rule((String) diag.get("code"))
                    .filePath(originalPath)
                    .startLine(((Number) position.get("line")).intValue() + 1)
                    .message((String) diag.get("message"))
                    .suggestion(extractSuggestion(diag))
                    .build();

                issues.add(issue);
            }

        } catch (Exception e) {
            log.warn("Failed to parse Biome output: {}", e.getMessage());
        }

        return issues;
    }

    /**
     * Map Biome severity to Pullwise severity.
     */
    private Severity mapSeverity(String biomeSeverity) {
        return switch (biomeSeverity.toLowerCase()) {
            case "error" -> Severity.HIGH;
            case "warning" -> Severity.MEDIUM;
            case "info" -> Severity.LOW;
            default -> Severity.LOW;
        };
    }

    /**
     * Extract suggestion from diagnostic.
     */
    @SuppressWarnings("unchecked")
    private String extractSuggestion(Map<String, Object> diag) {
        Object suggest = diag.get("suggestion");
        if (suggest instanceof Map) {
            return (String) ((Map<String, Object>) suggest).get("get");
        }
        return null;
    }

    /**
     * Simple JSON parser.
     */
    private Map<String, Object> parseJson(String json) {
        // Use Jackson or Gson in production
        // This is simplified
        return new com.fasterxml.jackson.databind.ObjectMapper()
            .readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Check if Biome is available.
     */
    private boolean isBiomeAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(biomePath, "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void shutdown() {
        // Cleanup if needed
    }
}
```

## Ruff Plugin

### Plugin Implementation

```java
package com.pullwise.plugin.ruff;

import com.pullwise.api.application.service.plugin.api.*;
import com.pullwise.api.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Plugin that wraps Ruff for Python analysis.
 */
public class RuffPlugin extends AbstractCodeReviewPlugin {

    private static final Logger log = LoggerFactory.getLogger(RuffPlugin.class);
    private static final Set<PluginLanguage> SUPPORTED = Set.of(PluginLanguage.PYTHON);

    private String ruffPath = "ruff";
    private Set<String> selectRules = new HashSet<>();
    private Set<String> ignoreRules = new HashSet<>();
    private int timeoutMs = 30000;

    @Override
    public String getId() {
        return "com.pullwise.plugin.ruff";
    }

    @Override
    public String getName() {
        return "Ruff Linter";
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
        ruffPath = getConfigString("path", "ruff");
        selectRules = new HashSet<>(getConfigList("selectRules"));
        ignoreRules = new HashSet<>(getConfigList("ignoreRules"));
        timeoutMs = getConfigInt("timeout", 30000);

        if (!isRuffAvailable()) {
            throw new PluginException(
                getId(),
                "Ruff not found at: " + ruffPath,
                false
            );
        }

        log.info("Ruff plugin initialized: path={}", ruffPath);
    }

    @Override
    protected AnalysisResult doAnalyze(AnalysisRequest request) {
        List<Issue> issues = new ArrayList<>();

        for (DiffFile file : filterSupportedFiles(request.getDiff().getFiles())) {
            if (file.getStatus() == FileStatus.DELETED) {
                continue;
            }

            try {
                Path tempFile = createTempFile(file);
                issues.addAll(runRuff(tempFile, file.getPath()));
                Files.deleteIfExists(tempFile);
            } catch (Exception e) {
                log.warn("Failed to analyze {}: {}", file.getPath(), e.getMessage());
            }
        }

        return result(issues);
    }

    private Path createTempFile(DiffFile file) throws IOException {
        String content = file.getLines().stream()
            .map(DiffLine::getContent)
            .collect(Collectors.joining("\n"));

        Path tempDir = Files.createTempDirectory("ruff-");
        Path tempFile = tempDir.resolve("temp.py");
        Files.writeString(tempFile, content, StandardCharsets.UTF_8);
        return tempFile;
    }

    private List<Issue> runRuff(Path file, String originalPath) {
        List<String> command = buildCommand(file);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = readOutput(process, timeoutMs);

            return output.isEmpty()
                ? Collections.emptyList()
                : parseRuffOutput(output, originalPath);

        } catch (Exception e) {
            log.error("Failed to run Ruff", e);
            return Collections.emptyList();
        }
    }

    private List<String> buildCommand(Path file) {
        List<String> command = new ArrayList<>();
        command.add(ruffPath);
        command.add("check");
        command.add(file.toString());
        command.add("--output-format=json");
        command.add("--no-fix");

        if (!selectRules.isEmpty()) {
            command.add("--select=" + String.join(",", selectRules));
        }

        if (!ignoreRules.isEmpty()) {
            command.add("--ignore=" + String.join(",", ignoreRules));
        }

        return command;
    }

    private List<Issue> parseRuffOutput(String output, String originalPath) {
        List<Issue> issues = new ArrayList<>();

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();

            List<Map<String, Object>> diagnostics = mapper.readValue(
                output,
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {}
            );

            for (Map<String, Object> diag : diagnostics) {
                Issue issue = Issue.builder()
                    .severity(mapRuffSeverity((String) diag.get("fix_severity")))
                    .type(mapRuffType((String) diag.get("code")))
                    .rule((String) diag.get("code"))
                    .filePath(originalPath)
                    .startLine(((Number) diag.get("location")).get("start_line"))
                    .endLine(((Number) diag.get("location")).get("end_line"))
                    .column(
                        ((Number) diag.get("location")).get("start_column"),
                        ((Number) diag.get("location")).get("end_column")
                    )
                    .message((String) diag.get("message"))
                    .documentationUrls(List.of(
                        "https://docs.astral.sh/ruff/rules/" +
                        ((String) diag.get("code")).toLowerCase()
                    ))
                    .build();

                issues.add(issue);
            }

        } catch (Exception e) {
            log.warn("Failed to parse Ruff output: {}", e.getMessage());
        }

        return issues;
    }

    private Severity mapRuffSeverity(String severity) {
        return switch (severity) {
            case "error" -> Severity.HIGH;
            case "warning" -> Severity.MEDIUM;
            default -> Severity.LOW;
        };
    }

    private IssueType mapRuffType(String code) {
        String prefix = code.split("")[0];
        return switch (prefix) {
            case "E", "F" -> IssueType.BUG;
            case "W" -> IssueType.CODE_SMELL;
            case "S" -> IssueType.VULNERABILITY;
            default -> IssueType.CODE_SMELL;
        };
    }

    private String readOutput(Process process, long timeoutMs)
            throws IOException, InterruptedException {
        StringBuilder output = new StringBuilder();

        try (InputStream is = process.getInputStream();
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(is, StandardCharsets.UTF_8))) {

            char[] buffer = new char[8192];
            long start = System.currentTimeMillis();

            while (true) {
                if (System.currentTimeMillis() - start > timeoutMs) {
                    process.destroyForcibly();
                    throw new IOException("Process timeout");
                }

                int ch = reader.read();
                if (ch == -1) break;
                output.append((char) ch);

                try {
                    process.exitValue();
                    while (reader.ready()) {
                        ch = reader.read();
                        if (ch == -1) break;
                        output.append((char) ch);
                    }
                    break;
                } catch (IllegalThreadStateException e) {
                    // Still running
                }
            }
        }

        return output.toString();
    }

    private boolean isRuffAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(ruffPath, "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void shutdown() {
        // Cleanup
    }
}
```

## Configuration

```yaml
# application.yml
pullwise:
  plugins:
    biome:
      enabled: true
      path: biome  # or full path
      checkOnly: true
      timeout: 30000

    ruff:
      enabled: true
      path: ruff
      selectRules:
        - E  # pycodestyle errors
        - W  # pycodestyle warnings
        - F  # pyflakes
        - S  # bandit
      ignoreRules:
        - E501  # line too long
      timeout: 30000
```

## Installing Tools

### Biome

```bash
# Using npm
npm install -g @biomejs/biome

# Using Homebrew
brew install biome

# Verify installation
biome --version
```

### Ruff

```bash
# Using pip
pip install ruff

# Using Homebrew
brew install ruff

# Verify installation
ruff --version
```

## Docker Configuration

```dockerfile
FROM openjdk:17-slim

# Install Node.js for Biome
RUN apt-get update && apt-get install -y \
    curl \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && npm install -g @biomejs/biome

# Install Python for Ruff
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    && pip3 install ruff

# Copy application
COPY target/pullwise.jar /app/pullwise.jar

ENTRYPOINT ["java", "-jar", "/app/pullwise.jar"]
```

## Expected Output

```
Biome Analysis Results
======================

Issues Found: 3

1. lint/nursery/noConstAssign (Medium)
   File: src/utils/helper.ts:42
   Message: Assigning to a const variable is not allowed
   Suggestion: Use 'let' instead

2. lint/suspicious/noExplicitAny (Low)
   File: src/api/client.ts:18
   Message: Unexpected 'any' type
   Suggestion: Specify a more specific type

3. style/useConst (Low)
   File: src/components/Button.tsx:25
   Message: 'handleClick' is never reassigned, use 'const'
   Suggestion: Declare with 'const'
```

## Best Practices

### 1. Use Timeout

```java
// Always use timeout to prevent hanging
private String readOutput(Process process, long timeoutMs) {
    // ... implementation with timeout check
}
```

### 2. Clean Up Temp Files

```java
try {
    Path tempFile = createTempFile(file);
    // Use temp file
} finally {
    Files.deleteIfExists(tempFile);
}
```

### 3. Handle Process Failures

```java
try {
    // Run external tool
} catch (Exception e) {
    log.warn("Failed to run tool: {}", e.getMessage());
    // Return empty result instead of failing entire analysis
    return Collections.emptyList();
}
```

### 4. Cache Tool Availability

```java
private Boolean toolAvailable = null;

private boolean isToolAvailable() {
    if (toolAvailable != null) {
        return toolAvailable;
    }
    toolAvailable = checkToolAvailability();
    return toolAvailable;
}
```

## Next Steps

- [Config Plugin](/docs/plugin-development/examples/config-plugin) - Configuration examples
- [External Tool](/docs/plugin-development/examples/external-tool) - Generic wrapper
- [Packaging](/docs/plugin-development/packaging/) - Packaging plugins
