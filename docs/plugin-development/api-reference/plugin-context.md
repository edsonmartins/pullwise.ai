# Plugin Context

Context information available to plugins.

## Overview

The `PluginContext` provides plugins with runtime information including project details, repository access, and utility methods.

## Interface Definition

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Context available to plugins during analysis.
 *
 * <p>Provides access to:
 * <ul>
 *   <li>Project configuration</li>
 *   <li>Repository information</li>
 *   <li>File content access</li>
 *   <li>Utility methods</li>
 * </ul>
 */
public interface PluginContext {

    /**
     * Get project information.
     *
     * @return Project details
     */
    ProjectInfo getProject();

    /**
     * Get repository information.
     *
     * @return Repository details
     */
    RepositoryInfo getRepository();

    /**
     * Get analysis metadata.
     *
     * @return Analysis context
     */
    AnalysisInfo getAnalysis();

    /**
     * Access file content.
     *
     * @return File content accessor
     */
    FileContentAccessor getFileContent();
}
```

## Project Info

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Project information.
 */
public interface ProjectInfo {

    /**
     * Unique project identifier.
     *
     * @return Project ID
     */
    Long getId();

    /**
     * Project name.
     *
     * @return Project name
     */
    String getName();

    /**
     * Project description.
     *
     * @return Description or null
     */
    @Nullable
    String getDescription();

    /**
     * Primary programming language.
     *
     * @return Language or null
     */
    @Nullable
    String getLanguage();

    /**
     * Project-specific configuration.
     *
     * @return Configuration map
     */
    Map<String, Object> getConfig();

    /**
     * Get configuration value.
     *
     * @param key Configuration key
     * @return Value or null
     */
    @Nullable
    default Object getConfigValue(String key) {
        return getConfig().get(key);
    }

    /**
     * Get configuration value with default.
     *
     * @param key Configuration key
     * @param defaultValue Default value
     * @return Value or default
     */
    @SuppressWarnings("unchecked")
    default <T> T getConfigValue(String key, T defaultValue) {
        Object value = getConfig().get(key);
        return value != null ? (T) value : defaultValue;
    }
}
```

## Repository Info

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Repository information.
 */
public interface RepositoryInfo {

    /**
     * Repository URL.
     *
     * @return Repository URL
     */
    String getUrl();

    /**
     * Repository provider.
     *
     * @return Provider (github, gitlab, bitbucket)
     */
    String getProvider();

    /**
     * Default branch name.
     *
     * @return Branch name (e.g., "main", "master")
     */
    String getDefaultBranch();

    /**
     * Current branch being analyzed.
     *
     * @return Branch name
     */
    String getBranch();

    /**
     * Current commit SHA.
     *
     * @return Full commit SHA
     */
    String getCommitSha();

    /**
     * Base commit SHA for comparison.
     *
     * @return Base commit or null
     */
    @Nullable
    String getBaseCommitSha();

    /**
     * Pull request number if applicable.
     *
     * @return PR number or null
     */
    @Nullable
    Integer getPullRequestNumber();
}
```

## Analysis Info

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Analysis metadata.
 */
public interface AnalysisInfo {

    /**
     * Analysis type.
     *
     * @return Type (FULL, INCREMENTAL, PR)
     */
    AnalysisType getType();

    /**
     * Analysis start time.
     *
     * @return Start timestamp
     */
    Instant getStartTime();

    /**
     * Maximum allowed duration.
     *
     * @return Timeout in milliseconds
     */
    long getTimeout();

    /**
     * Whether this is a re-analysis.
     *
     * @return True if re-analysis
     */
    boolean isReAnalysis();

    /**
     * Get previous analysis results if available.
     *
     * @return Previous results or empty
     */
    Optional<AnalysisResult> getPreviousResult();
}
```

## Analysis Type

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Types of analysis.
 */
public enum AnalysisType {

    /**
     * Full repository scan.
     * Analyzes all files in repository.
     */
    FULL,

    /**
     * Incremental analysis.
     * Analyzes only changed files since last analysis.
     */
    INCREMENTAL,

    /**
     * Pull request analysis.
     * Analyzes files in a PR/MR.
     */
    PULL_REQUEST,

    /**
     * Single commit analysis.
     * Analyzes changes in a single commit.
     */
    SINGLE_COMMIT
}
```

## File Content Accessor

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Accessor for file content operations.
 */
public interface FileContentAccessor {

    /**
     * Get file content at current commit.
     *
     * @param path File path
     * @return File content or empty if not found
     */
    Optional<String> getContent(String path);

    /**
     * Get file content at specific commit.
     *
     * @param path File path
     * @param commitSha Commit SHA
     * @return File content or empty if not found
     */
    Optional<String> getContent(String path, String commitSha);

    /**
     * Get file lines.
     *
     * @param path File path
     * @return List of lines (1-indexed)
     */
    Optional<List<String>> getLines(String path);

    /**
     * Get specific line range.
     *
     * @param path File path
     * @param startLine Start line (1-indexed, inclusive)
     * @param endLine End line (1-indexed, inclusive)
     * @return List of lines in range
     */
    Optional<List<String>> getLines(String path, int startLine, int endLine);

    /**
     * Check if file exists.
     *
     * @param path File path
     * @return True if file exists
     */
    boolean exists(String path);

    /**
     * Get file metadata.
     *
     * @param path File path
     * @return File metadata or empty
     */
    Optional<FileMetadata> getMetadata(String path);

    /**
     * List files matching pattern.
     *
     * @param pattern Glob pattern (e.g., "**/*.java")
     * @return List of matching paths
     */
    List<String> listFiles(String pattern);
}
```

## File Metadata

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * File metadata.
 */
public interface FileMetadata {

    /**
     * File path.
     *
     * @return Path
     */
    String getPath();

    /**
     * File size in bytes.
     *
     * @return Size
     */
    long getSize();

    /**
     * Last modified timestamp.
     *
     * @return Timestamp
     */
    Instant getLastModified();

    /**
     * File language.
     *
     * @return Detected language
     */
    PluginLanguage getLanguage();

    /**
     * Whether file is executable.
     *
     * @return True if executable
     */
    boolean isExecutable();

    /**
     * File hash.
     *
     * @return SHA-256 hash or null
     */
    @Nullable
    String getHash();
}
```

## Usage Examples

### Access Project Config

```java
public class MyPlugin extends AbstractCodeReviewPlugin {

    @Override
    protected void doInitialize() throws PluginException {
        // Access project-specific configuration
        boolean strictMode = getConfigBoolean("strictMode", false);
        List<String> excludedPaths = getConfigList("excludedPaths");
    }

    @Override
    protected AnalysisResult doAnalyze(AnalysisRequest request) {
        PluginContext context = request.getContext();

        // Get project info
        ProjectInfo project = context.getProject();
        String language = project.getLanguage();

        // Get repository info
        RepositoryInfo repo = context.getRepository();
        String branch = repo.getBranch();
        String commit = repo.getCommitSha();

        // Continue analysis...
    }
}
```

### Access File Content

```java
@Override
protected AnalysisResult doAnalyze(AnalysisRequest request) {
    PluginContext context = request.getContext();
    FileContentAccessor files = context.getFileContent();

    List<Issue> issues = new ArrayList<>();

    for (DiffFile file : request.getDiff().getFiles()) {
        String path = file.getPath();

        // Get current file content
        Optional<String> content = files.getContent(path);
        if (content.isEmpty()) {
            continue;
        }

        // Get specific lines
        Optional<List<String>> lines = files.getLines(path, 1, 100);
        if (lines.isPresent()) {
            // Analyze lines
        }

        // Get file metadata
        Optional<FileMetadata> metadata = files.getMetadata(path);
        metadata.ifPresent(meta -> {
            log.debug("File size: {} bytes", meta.getSize());
            log.debug("Language: {}", meta.getLanguage());
        });
    }

    return result(issues);
}
```

### Context-Aware Analysis

```java
@Override
protected AnalysisResult doAnalyze(AnalysisRequest request) {
    PluginContext context = request.getContext();
    AnalysisInfo analysis = context.getAnalysis();

    // Adjust behavior based on analysis type
    switch (analysis.getType()) {
        case PULL_REQUEST:
            // More thorough checks for PRs
            return analyzeForPR(request);

        case INCREMENTAL:
            // Faster checks for incremental
            return analyzeIncremental(request);

        case FULL:
            // Comprehensive checks for full scan
            return analyzeFull(request);

        default:
            return analyzeDefault(request);
    }
}

private AnalysisResult analyzeForPR(AnalysisRequest request) {
    // Include suggestions and documentation
    List<Issue> issues = new ArrayList<>();

    for (Issue issue : findIssues(request)) {
        issues.add(issue.toBuilder()
            .suggestion(generateSuggestion(issue))
            .documentationUrls(List.of(getDocumentationUrl(issue)))
            .build());
    }

    return result(issues);
}
```

### Compare with Previous Analysis

```java
@Override
protected AnalysisResult doAnalyze(AnalysisRequest request) {
    PluginContext context = request.getContext();

    // Get previous results
    Optional<AnalysisResult> previous = context.getAnalysis()
        .getPreviousResult();

    if (previous.isEmpty()) {
        // No previous analysis, run full check
        return fullAnalysis(request);
    }

    // Compare with previous results
    AnalysisResult previousResult = previous.get();
    Map<String, Issue> previousIssues = previousResult.getIssues().stream()
        .collect(Collectors.toMap(
            i -> i.getFilePath() + ":" + i.getStartLine(),
            Function.identity()
        ));

    List<Issue> newIssues = new ArrayList<>();
    List<Issue> recurringIssues = new ArrayList<>();

    for (Issue currentIssue : findIssues(request)) {
        String key = currentIssue.getFilePath() + ":"
            + currentIssue.getStartLine();

        Issue previousIssue = previousIssues.get(key);
        if (previousIssue == null) {
            // New issue
            newIssues.add(currentIssue);
        } else {
            // Recurring issue
            recurringIssues.add(currentIssue.toBuilder()
                .message("(Recurring) " + currentIssue.getMessage())
                .build());
        }
    }

    // Prioritize recurring issues
    recurringIssues.forEach(i -> {
        i.setSeverity(increaseSeverity(i.getSeverity()));
    });

    return result(Stream.concat(
        newIssues.stream(),
        recurringIssues.stream()
    ).toList());
}
```

### List and Filter Files

```java
@Override
protected AnalysisResult doAnalyze(AnalysisRequest request) {
    PluginContext context = request.getContext();
    FileContentAccessor files = context.getFileContent();

    // List all Java files
    List<String> javaFiles = files.listFiles("**/*.java");

    // List all TypeScript files
    List<String> tsFiles = files.listFiles("**/*.ts");

    // Analyze specific files
    List<Issue> issues = new ArrayList<>();

    for (String path : javaFiles) {
        // Skip test files
        if (path.contains("/test/")) {
            continue;
        }

        // Get file content
        Optional<String> content = files.getContent(path);
        if (content.isPresent()) {
            issues.addAll(analyzeFile(path, content.get()));
        }
    }

    return result(issues);
}
```

## Best Practices

### 1. Cache File Content

```java
// Bad: Read file multiple times
String content1 = files.getContent(path).get();
String content2 = files.getContent(path).get(); // Another read

// Good: Cache content
Optional<String> contentOpt = files.getContent(path);
if (contentOpt.isPresent()) {
    String content = contentOpt.get();
    // Use cached content multiple times
}
```

### 2. Check Existence Before Access

```java
// Good: Check first
if (files.exists(path)) {
    Optional<String> content = files.getContent(path);
    // Process content
}

// Bad: Assume existence
Optional<String> content = files.getContent(path);
if (content.isPresent()) {
    // Process
}
```

### 3. Use Pattern Matching

```java
// Good: Use glob patterns
List<String> configFiles = files.listFiles("**/application.{yml,yaml,properties}");

// Bad: Manual filtering
List<String> allFiles = files.listFiles("**/*");
List<String> configFiles = allFiles.stream()
    .filter(f -> f.endsWith(".yml") || f.endsWith(".yaml"))
    .toList();
```

### 4. Handle Timeouts

```java
@Override
protected AnalysisResult doAnalyze(AnalysisRequest request) {
    PluginContext context = request.getContext();
    long timeout = context.getAnalysis().getTimeout();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<AnalysisResult> future = executor.submit(() -> {
        return doExpensiveAnalysis(request);
    });

    try {
        return future.get(timeout, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
        future.cancel(true);
        return errorResult("Analysis timed out");
    } finally {
        executor.shutdown();
    }
}
```

## Next Steps

- [Analysis Request](/docs/plugin-development/api-reference/analysis-request) - Request details
- [Analysis Result](/docs/plugin-development/api-reference/analysis-result) - Result details
- [Examples](/docs/plugin-development/examples/) - Plugin examples
