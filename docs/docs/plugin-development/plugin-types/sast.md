# Plugin Types

Different types of plugins you can create for Pullwise.

## Overview

Pullwise supports several plugin types, each designed for specific analysis purposes:

| Type | Purpose | Examples |
|------|---------|---------|
| **SAST** | Static application security testing | Security scanners |
| **LINTER** | Code style and quality | ESLint, Checkstyle |
| **SECURITY** | Vulnerability detection | Bandit, Brakeman |
| **PERFORMANCE** | Performance analysis | Performance profilers |
| **CUSTOM_LLM** | Custom LLM integration | Fine-tuned models |

## SAST Plugins

### Purpose

Detect security vulnerabilities through static analysis:

- SQL injection
- XSS vulnerabilities
- Path traversal
- Command injection
- XXE attacks

### Example SAST Plugin

```java
public class SQLInjectionPlugin extends AbstractCodeReviewPlugin {

    private static final Pattern SQL_INJECTION_PATTERN =
        Pattern.compile("Statement\\.execute\\(.*\\+.*\\)");

    @Override
    protected AnalysisResult doAnalyze(AnalysisRequest request) {
        List<Issue> issues = new ArrayList<>();

        for (DiffFile file : request.getDiff().getFiles()) {
            if (file.getLanguage() != PluginLanguage.JAVA) {
                continue;
            }

            for (DiffLine line : file.getAddedLines()) {
                if (SQL_INJECTION_PATTERN.matcher(line.getContent()).find()) {
                    issues.add(Issue.builder()
                        .severity(Severity.CRITICAL)
                        .type(IssueType.VULNERABILITY)
                        .rule("SQL_INJECTION")
                        .filePath(file.getPath())
                        .startLine(line.getLineNumber())
                        .message("Potential SQL injection vulnerability")
                        .suggestion("Use parameterized queries")
                        .build());
                }
            }
        }

        return AnalysisResult.builder()
            .pluginId(getId())
            .issues(issues)
            .build();
    }

    @Override
    public PluginType getType() {
        return PluginType.SAST;
    }
}
```

## Linter Plugins

### Purpose

Enforce code style and quality standards:

- Naming conventions
- Code formatting
- Complexity limits
- Documentation requirements
- Best practices

### Example Linter Plugin

```java
public class NamingConventionPlugin extends AbstractCodeReviewPlugin {

    private static final Pattern VARIABLE_PATTERN =
        Pattern.compile("^([a-z][a-zA-Z0-9]*)$");

    @Override
    protected AnalysisResult doAnalyze(AnalysisRequest request) {
        List<Issue> issues = new ArrayList<>();

        for (DiffFile file : request.getDiff().getFiles()) {
            for (DiffLine line : file.getAddedLines()) {
                // Check for variable declarations
                if (line.getContent().contains("var ")) {
                    Matcher matcher = Pattern
                        .compile("var\\s+([a-zA-Z0-9_]+)\\s*=")
                        .matcher(line.getContent());

                    if (matcher.find()) {
                        String varName = matcher.group(1);

                        if (!VARIABLE_PATTERN.matcher(varName).matches()) {
                            issues.add(Issue.builder()
                                .severity(Severity.LOW)
                                .type(IssueType.CODE_SMELL)
                                .rule("NAMING_CONVENTION")
                                .filePath(file.getPath())
                                .startLine(line.getLineNumber())
                                .message("Variable name doesn't follow conventions")
                                .suggestion("Use camelCase: " + toCamelCase(varName))
                                .build());
                        }
                    }
                }
            }
        }

        return AnalysisResult.builder()
            .pluginId(getId())
            .issues(issues)
            .build();
    }

    @Override
    public PluginType getType() {
        return PluginType.LINTER;
    }
}
```

## Security Plugins

### Purpose

Detect security-specific issues beyond SAST:

- Weak cryptography
- Hardcoded secrets
- Insecure randomness
- Missing authentication

### Example Security Plugin

```java
public class WeakCryptoPlugin extends AbstractCodeReviewPlugin {

    private static final Pattern WEAK_CIPHERS = Pattern.compile(
        "Cipher\\.getInstance\\(\"(DES|MD4|MD5)\"\\)"
    );

    @Override
    protected AnalysisResult doAnalyze(AnalysisRequest request) {
        List<Issue> issues = new ArrayList<>();

        for (DiffFile file : request.getDiff().getFiles()) {
            for (DiffLine line : file.getAddedLines()) {
                Matcher matcher = WEAK_CIPHERS
                    .matcher(line.getContent());

                if (matcher.find()) {
                    String cipher = matcher.group(1);

                    issues.add(Issue.builder()
                        .severity(Severity.HIGH)
                        .type(IssueType.VULNERABILITY)
                        .rule("WEAK_CRYPTOGRAPHY")
                        .filePath(file.getPath())
                        .startLine(line.getLineNumber())
                        .message("Using weak cipher: " + cipher)
                        .suggestion("Use AES-256 instead")
                        .build());
                }
            }
        }

        return AnalysisResult.builder()
            .pluginId(getId())
            .issues(issues)
            .build();
    }

    @Override
    public PluginType getType() {
        return PluginType.SECURITY;
    }
}
```

## Performance Plugins

### Purpose

Identify performance bottlenecks:

- N+1 queries
- Missing indexes
- Large result sets
- Inefficient algorithms

### Example Performance Plugin

```java
public class NPlusOnePlugin extends AbstractCodeReviewPlugin {

    private static final Pattern QUERY_PATTERN =
        Pattern.compile("\\.(findAll|find)\\(");

    private static final Pattern LOOP_PATTERN =
        Pattern.compile("for\\s*\\(");

    @Override
    protected AnalysisResult doAnalyze(AnalysisRequest request) {
        List<Issue> issues = new ArrayList<>();

        for (DiffFile file : request.getDiff().getFiles()) {
            List<String> lines = file.getAddedLines().stream()
                .map(DiffLine::getContent)
                .toList();

            // Find queries inside loops
            boolean inLoop = false;
            for (String line : lines) {
                if (LOOP_PATTERN.matcher(line).find()) {
                    inLoop = true;
                } else if (line.contains("}")) {
                    inLoop = false;
                }

                if (inLoop && QUERY_PATTERN.matcher(line).find()) {
                    issues.add(Issue.builder()
                        .severity(Severity.HIGH)
                        .type(IssueType.PERFORMANCE)
                        .rule("N_PLUS_ONE_QUERY")
                        .filePath(file.getPath())
                        .startLine(getLineNumber(lines, line))
                        .message("Query inside loop may cause N+1 problem")
                        .suggestion("Fetch all data before loop")
                        .build());
                }
            }
        }

        return AnalysisResult.builder()
            .pluginId(getId())
            .issues(issues)
            .build();
    }

    @Override
    public PluginType getType() {
        return PluginType.PERFORMANCE;
    }
}
```

## Custom LLM Plugins

### Purpose

Integrate custom LLM models:

- Fine-tuned models
- Domain-specific models
- Local models
- Proprietary models

### Example LLM Plugin

```java
public class CustomLLMPlugin extends AbstractCodeReviewPlugin {

    private String modelEndpoint;
    private String apiKey;

    @Override
    protected void doInitialize() throws PluginException {
        this.modelEndpoint = getConfigString("endpoint", "http://localhost:11434/api/generate");
        this.apiKey = getConfigString("apiKey", "");
    }

    @Override
    protected AnalysisResult doAnalyze(AnalysisRequest request) {
        List<Issue> issues = new ArrayList<>();

        // Prepare prompt
        String prompt = buildPrompt(request);

        // Call custom model
        String response = callModel(prompt);

        // Parse response into issues
        issues = parseResponse(response);

        return AnalysisResult.builder()
            .pluginId(getId())
            .issues(issues)
            .build();
    }

    private String callModel(String prompt) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(modelEndpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(BodyPublishers.ofString(
                    "{\"model\":\"my-model\",\"prompt\":\"" + prompt + "\"}"
                ))
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

            return response.body();
        } catch (Exception e) {
            log.error("Failed to call model", e);
            return "";
        }
    }

    @Override
    public PluginType getType() {
        return PluginType.CUSTOM_LLM;
    }
}
```

## Hybrid Plugins

### Purpose

Combine multiple analysis types:

```java
public class HybridPlugin extends AbstractCodeReviewPlugin {

    @Override
    public PluginType getType() {
        return PluginType.SECURITY;  // Primary type
    }

    @Override
    public Set<PluginCapability> getCapabilities() {
        return Set.of(
            PluginCapability.SAST,
            PluginCapability.LINTER,
            PluginCapability.SECURITY
        );
    }

    @Override
    protected AnalysisResult doAnalyze(AnalysisRequest request) {
        List<Issue> allIssues = new ArrayList<>();

        // SAST analysis
        allIssues.addAll(sastAnalysis(request));

        // Linter checks
        allIssues.addAll(lintChecks(request));

        // Security scan
        allIssues.addAll(securityScan(request));

        // Consolidate
        return consolidate(allIssues);
    }
}
```

## Plugin Type Selection

Configure which plugin types to run:

```yaml
# application.yml
pullwise:
  plugins:
    enabled:
      - SAST           # Security scanning
      - LINTER         # Code quality
      - SECURITY       # Security plugins
      - PERFORMANCE    # Performance analysis
```

## Per-Type Configuration

```yaml
# Configure each type differently
pullwise:
  plugins:
    SAST:
      timeout: 300000       # 5 minutes
      parallel: true
    LINTER:
      timeout: 60000        # 1 minute
      parallel: true
    SECURITY:
      timeout: 120000       # 2 minutes
      block_on_critical: true
```

## Best Practices

### 1. Choose Appropriate Type

```java
// If detecting vulnerabilities
@Override
public PluginType getType() {
    return PluginType.SAST;
}

// If checking style
@Override
public PluginType getType() {
    return PluginType.LINTER;
}
```

### 2. Handle Errors Gracefully

```java
try {
    return analyze(request);
} catch (Exception e) {
    log.error("Analysis failed", e);
    return AnalysisResult.builder()
        .pluginId(getId())
        .success(false)
        .errorMessage(e.getMessage())
        .build();
}
```

### 3. Optimize Performance

```java
// Use parallel streams
return files.parallelStream()
    .flatMap(file -> analyzeFile(file).stream())
    .toList();
```

## Next Steps

- [Getting Started](/docs/plugin-development/getting-started) - Create your first plugin
- [API Reference](/docs/plugin-development/api-reference/) - Plugin API docs
- [Examples](/docs/plugin-development/examples/) - Example plugins
