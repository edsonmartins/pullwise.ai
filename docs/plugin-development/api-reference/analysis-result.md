# Analysis Result

Result object returned by plugins after analysis.

## Overview

The `AnalysisResult` contains the findings from a plugin's analysis, including any issues found, success status, and optional metrics.

## Interface Definition

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Result returned by plugin analysis.
 *
 * <p>Contains:
 * <ul>
 *   <li>Plugin identifier</li>
 *   <li>Success/failure status</li>
 *   <li>List of issues found</li>
 *   <li>Optional error message</li>
 *   <li>Optional metrics</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface AnalysisResult {

    /**
     * Plugin that produced this result.
     *
     * @return Plugin identifier
     */
    @NonNull
    String getPluginId();

    /**
     * Plugin version.
     *
     * @return Version string
     */
    @Nullable
    String getPluginVersion();

    /**
     * Whether analysis succeeded.
     *
     * @return True if successful, false if error occurred
     */
    boolean isSuccess();

    /**
     * Error message if analysis failed.
     *
     * @return Error message or null if successful
     */
    @Nullable
    String getErrorMessage();

    /**
     * Issues found during analysis.
     *
     * @return List of issues (empty if none found)
     */
    @NonNull
    List<Issue> getIssues();

    /**
     * Metrics about the analysis.
     *
     * @return Optional metrics (execution time, files scanned, etc.)
     */
    @NonNull
    Optional<AnalysisMetrics> getMetrics();

    /**
     * Additional data.
     *
     * <p>Can contain plugin-specific information.
     *
     * @return Additional data map
     */
    @NonNull
    Map<String, Object> getData();

    /**
     * Check if any issues were found.
     *
     * @return True if at least one issue was found
     */
    default boolean hasIssues() {
        return !getIssues().isEmpty();
    }

    /**
     * Get issues by severity.
     *
     * @param severity Severity level
     * @return Issues of that severity
     */
    @NonNull
    default List<Issue> getIssuesBySeverity(@NonNull Severity severity) {
        return getIssues().stream()
            .filter(i -> i.getSeverity() == severity)
            .toList();
    }

    /**
     * Get issues by type.
     *
     * @param type Issue type
     * @return Issues of that type
     */
    @NonNull
    default List<Issue> getIssuesByType(@NonNull IssueType type) {
        return getIssues().stream()
            .filter(i -> i.getType() == type)
            .toList();
    }

    /**
     * Count issues by severity.
     *
     * @return Map of severity to count
     */
    @NonNull
    default Map<Severity, Long> getIssueCounts() {
        return getIssues().stream()
            .collect(Collectors.groupingBy(
                Issue::getSeverity,
                Collectors.counting()
            ));
    }
}
```

## Builder Pattern

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Builder for AnalysisResult.
 */
public class AnalysisResultBuilder {

    private String pluginId;
    private String pluginVersion;
    private boolean success = true;
    private String errorMessage;
    private List<Issue> issues = new ArrayList<>();
    private AnalysisMetrics metrics;
    private Map<String, Object> data = new HashMap<>();

    public static AnalysisResultBuilder builder() {
        return new AnalysisResultBuilder();
    }

    public AnalysisResultBuilder pluginId(String pluginId) {
        this.pluginId = pluginId;
        return this;
    }

    public AnalysisResultBuilder pluginVersion(String version) {
        this.pluginVersion = version;
        return this;
    }

    public AnalysisResultBuilder success(boolean success) {
        this.success = success;
        return this;
    }

    public AnalysisResultBuilder errorMessage(String message) {
        this.errorMessage = message;
        this.success = false;
        return this;
    }

    public AnalysisResultBuilder issues(List<Issue> issues) {
        this.issues = issues != null ? issues : new ArrayList<>();
        return this;
    }

    public AnalysisResultBuilder addIssue(Issue issue) {
        this.issues.add(issue);
        return this;
    }

    public AnalysisResultBuilder metrics(AnalysisMetrics metrics) {
        this.metrics = metrics;
        return this;
    }

    public AnalysisResultBuilder data(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public AnalysisResult build() {
        return new AnalysisResultImpl(this);
    }
}
```

## Analysis Metrics

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Metrics about plugin analysis.
 */
public interface AnalysisMetrics {

    /**
     * Analysis duration in milliseconds.
     *
     * @return Duration
     */
    long getDuration();

    /**
     * Number of files scanned.
     *
     * @return File count
     */
    int getFilesScanned();

    /**
     * Number of lines analyzed.
     *
     * @return Line count
     */
    int getLinesAnalyzed();

    /**
     * Memory used in bytes.
     *
     * @return Memory usage
     */
    long getMemoryUsed();

    /**
     * Additional metrics.
     *
     * @return Custom metrics map
     */
    Map<String, Object> getCustomMetrics();
}
```

### Metrics Builder

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Builder for AnalysisMetrics.
 */
public class AnalysisMetricsBuilder {

    private long duration;
    private int filesScanned;
    private int linesAnalyzed;
    private long memoryUsed;
    private Map<String, Object> customMetrics = new HashMap<>();

    public static AnalysisMetricsBuilder builder() {
        return new AnalysisMetricsBuilder();
    }

    public AnalysisMetricsBuilder duration(long duration) {
        this.duration = duration;
        return this;
    }

    public AnalysisMetricsBuilder filesScanned(int count) {
        this.filesScanned = count;
        return this;
    }

    public AnalysisMetricsBuilder linesAnalyzed(int count) {
        this.linesAnalyzed = count;
        return this;
    }

    public AnalysisMetricsBuilder memoryUsed(long bytes) {
        this.memoryUsed = bytes;
        return this;
    }

    public AnalysisMetricsBuilder customMetric(String key, Object value) {
        this.customMetrics.put(key, value);
        return this;
    }

    public AnalysisMetrics build() {
        return new AnalysisMetricsImpl(this);
    }
}
```

## Issue Details

```java
package com.pullwise.api.domain.model;

/**
 * Builder for Issue.
 */
public class IssueBuilder {

    private Severity severity = Severity.MEDIUM;
    private IssueType type = IssueType.CODE_SMELL;
    private String rule;
    private String filePath;
    private Integer startLine;
    private Integer endLine;
    private Integer startColumn;
    private Integer endColumn;
    private String message;
    private String suggestion;
    private String codeSnippet;
    private List<String> documentationUrls = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();

    public static IssueBuilder builder() {
        return new IssueBuilder();
    }

    public IssueBuilder severity(Severity severity) {
        this.severity = severity;
        return this;
    }

    public IssueBuilder type(IssueType type) {
        this.type = type;
        return this;
    }

    public IssueBuilder rule(String rule) {
        this.rule = rule;
        return this;
    }

    public IssueBuilder filePath(String path) {
        this.filePath = path;
        return this;
    }

    public IssueBuilder startLine(Integer line) {
        this.startLine = line;
        return this;
    }

    public IssueBuilder endLine(Integer line) {
        this.endLine = line;
        return this;
    }

    public IssueBuilder column(Integer start, Integer end) {
        this.startColumn = start;
        this.endColumn = end;
        return this;
    }

    public IssueBuilder message(String message) {
        this.message = message;
        return this;
    }

    public IssueBuilder suggestion(String suggestion) {
        this.suggestion = suggestion;
        return this;
    }

    public IssueBuilder codeSnippet(String snippet) {
        this.codeSnippet = snippet;
        return this;
    }

    public IssueBuilder documentationUrls(List<String> urls) {
        this.documentationUrls = urls != null ? urls : new ArrayList<>();
        return this;
    }

    public IssueBuilder addDocumentationUrl(String url) {
        this.documentationUrls.add(url);
        return this;
    }

    public IssueBuilder metadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    public Issue build() {
        return new IssueImpl(this);
    }
}
```

## Usage Examples

### Basic Result

```java
@Override
protected AnalysisResult doAnalyze(AnalysisRequest request) {
    List<Issue> issues = new ArrayList<>();

    // Find issues
    for (DiffFile file : request.getDiff().getFiles()) {
        issues.addAll(analyzeFile(file));
    }

    // Return result
    return AnalysisResult.builder()
        .pluginId(getId())
        .pluginVersion(getVersion())
        .issues(issues)
        .build();
}
```

### Result with Metrics

```java
@Override
protected AnalysisResult doAnalyze(AnalysisRequest request) {
    long start = System.currentTimeMillis();
    int filesScanned = 0;
    int linesAnalyzed = 0;

    List<Issue> issues = new ArrayList<>();

    for (DiffFile file : request.getDiff().getFiles()) {
        filesScanned++;
        linesAnalyzed += file.getAddedLinesCount();
        issues.addAll(analyzeFile(file));
    }

    long duration = System.currentTimeMillis() - start;
    long memoryUsed = Runtime.getRuntime().totalMemory()
        - Runtime.getRuntime().freeMemory();

    return AnalysisResult.builder()
        .pluginId(getId())
        .issues(issues)
        .metrics(AnalysisMetrics.builder()
            .duration(duration)
            .filesScanned(filesScanned)
            .linesAnalyzed(linesAnalyzed)
            .memoryUsed(memoryUsed)
            .build())
        .build();
}
```

### Error Result

```java
@Override
protected AnalysisResult doAnalyze(AnalysisRequest request) {
    try {
        return doAnalysis(request);
    } catch (IOException e) {
        log.error("Failed to read files", e);

        return AnalysisResult.builder()
            .pluginId(getId())
            .success(false)
            .errorMessage("Failed to read files: " + e.getMessage())
            .build();
    } catch (Exception e) {
        log.error("Unexpected error", e);

        return AnalysisResult.builder()
            .pluginId(getId())
            .success(false)
            .errorMessage("Unexpected error: " + e.getMessage())
            .build();
    }
}
```

### Result with Custom Data

```java
@Override
protected AnalysisResult doAnalyze(AnalysisRequest request) {
    List<Issue> issues = findIssues(request);

    // Calculate custom metrics
    Map<String, Integer> ruleCounts = issues.stream()
        .collect(Collectors.groupingBy(
            Issue::getRule,
            Collectors.summingInt(i -> 1)
        ));

    return AnalysisResult.builder()
        .pluginId(getId())
        .issues(issues)
        .data("ruleCounts", ruleCounts)
        .data("timestamp", Instant.now())
        .data("configuration", getEffectiveConfig())
        .build();
}
```

### Issue with All Fields

```java
private Issue createCredentialIssue(DiffFile file, DiffLine line, Matcher m) {
    return Issue.builder()
        .severity(Severity.CRITICAL)
        .type(IssueType.VULNERABILITY)
        .rule("HARDCODED_CREDENTIAL")
        .filePath(file.getPath())
        .startLine(line.getLineNumber())
        .endLine(line.getLineNumber())
        .startColumn(m.start(2))
        .endColumn(m.end(2))
        .message("Hardcoded credential detected")
        .suggestion("Use environment variables or secret management")
        .codeSnippet(line.getContent().trim())
        .documentationUrls(List.of(
            "https://owasp.org/www-community/Hard_coding_secrets",
            "https://cwe.mitre.org/data/definitions/798.html"
        ))
        .metadata("credentialType", "API_KEY")
        .metadata("pattern", m.pattern())
        .build();
}
```

### Grouped Issues

```java
@Override
protected AnalysisResult doAnalyze(AnalysisRequest request) {
    // Find all issues
    List<Issue> allIssues = new ArrayList<>();
    for (DiffFile file : request.getDiff().getFiles()) {
        allIssues.addAll(analyzeFile(file));
    }

    // Group related issues
    Map<String, List<Issue>> grouped = groupIssues(allIssues);

    // Create summary issues
    List<Issue> result = new ArrayList<>();
    for (Map.Entry<String, List<Issue>> entry : grouped.entrySet()) {
        if (entry.getValue().size() > 5) {
            // Too many issues, create summary
            result.add(createSummaryIssue(entry.getKey(), entry.getValue()));
        } else {
            // Add individual issues
            result.addAll(entry.getValue());
        }
    }

    return AnalysisResult.builder()
        .pluginId(getId())
        .issues(result)
        .data("totalIssuesFound", allIssues.size())
        .data("groupedCount", grouped.size())
        .build();
}
```

## Result Validation

### Validating Before Return

```java
@Override
protected AnalysisResult doAnalyze(AnalysisRequest request) {
    List<Issue> issues = findIssues(request);

    // Validate issues
    List<Issue> valid = issues.stream()
        .filter(this::isValidIssue)
        .toList();

    if (valid.size() != issues.size()) {
        log.warn("Filtered {} invalid issues", issues.size() - valid.size());
    }

    return AnalysisResult.builder()
        .pluginId(getId())
        .issues(valid)
        .build();
}

private boolean isValidIssue(Issue issue) {
    // Check required fields
    if (issue.getFilePath() == null) {
        return false;
    }
    if (issue.getMessage() == null || issue.getMessage().isBlank()) {
        return false;
    }
    if (issue.getSeverity() == null) {
        return false;
    }
    if (issue.getType() == null) {
        return false;
    }
    return true;
}
```

## Best Practices

### 1. Always Set Required Fields

```java
// Good: All required fields set
return Issue.builder()
    .severity(Severity.HIGH)
    .type(IssueType.VULNERABILITY)
    .rule("SQL_INJECTION")
    .filePath(file.getPath())
    .startLine(line.getLineNumber())
    .message("Potential SQL injection")
    .suggestion("Use parameterized queries")
    .build();

// Bad: Missing required fields
return Issue.builder()
    .message("Error here")
    .build();
```

### 2. Use Appropriate Severity

```java
// Good: Severity matches impact
if (exploitable) {
    return Issue.builder()
        .severity(Severity.CRITICAL)  // Can be exploited
        .build();
} else if (impactful) {
    return Issue.builder()
        .severity(Severity.HIGH)  // Significant impact
        .build();
} else {
    return Issue.builder()
        .severity(Severity.MEDIUM)  // Moderate issue
        .build();
}

// Bad: Always critical
return Issue.builder()
    .severity(Severity.CRITICAL)  // Overused
    .build();
```

### 3. Provide Actionable Suggestions

```java
// Good: Clear, actionable suggestion
.suggestion("Replace Statement.execute with JdbcTemplate.queryForList")

// Bad: Vague suggestion
.suggestion("Fix this")

// Good: Example code
.suggestion("""
    Use:
    jdbcTemplate.queryForList(
        "SELECT * FROM users WHERE id = ?",
        userId
    )
    """)
```

### 4. Include Documentation Links

```java
// Good: Relevant documentation links
.documentationUrls(List.of(
    "https://owasp.org/www-community/attacks/SQL_Injection",
    "https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html"
))

// Good: Framework-specific docs
.documentationUrls(List.of(
    "https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html"
))
```

### 5. Track Performance

```java
// Good: Include metrics
return AnalysisResult.builder()
    .pluginId(getId())
    .issues(issues)
    .metrics(AnalysisMetrics.builder()
        .duration(System.currentTimeMillis() - start)
        .filesScanned(files.size())
        .linesAnalyzed(totalLines)
        .memoryUsed(getMemoryUsed())
        .customMetric("cacheHitRate", cache.getHitRate())
        .customMetric("rulesTriggered", issues.size())
        .build())
    .build();
```

## Next Steps

- [Examples](/docs/plugin-development/examples/) - Plugin examples
- [Packaging](/docs/plugin-development/packaging/) - Packaging plugins
- [Testing](/docs/plugin-development/testing) - Testing plugins
