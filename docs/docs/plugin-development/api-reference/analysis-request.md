# Analysis Request

Request object passed to plugins for analysis.

## Overview

The `AnalysisRequest` contains all the information a plugin needs to analyze code changes: the diff, project configuration, and analysis context.

## Interface Definition

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Request passed to plugins for code analysis.
 *
 * <p>Contains:
 * <ul>
 *   <li>{@link Diff} - Code changes to analyze</li>
 *   <li>{@link ProjectConfig} - Project configuration</li>
 *   <li>{@link PluginContext} - Runtime context</li>
 *   <li>{@link Map} - Plugin-specific configuration</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface AnalysisRequest {

    /**
     * The code diff to analyze.
     *
     * @return Code diff containing all changes
     */
    @NonNull
    Diff getDiff();

    /**
     * Project configuration.
     *
     * @return Project settings
     */
    @NonNull
    ProjectConfig getProjectConfig();

    /**
     * Plugin-specific configuration.
     *
     * @return Configuration map for this plugin
     */
    @NonNull
    Map<String, Object> getPluginConfig();

    /**
     * Analysis context.
     *
     * @return Context information
     */
    @NonNull
    PluginContext getContext();
}
```

## Diff Interface

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Code diff representation.
 */
public interface Diff {

    /**
     * All files in the diff.
     *
     * @return List of files (added, modified, deleted)
     */
    @NonNull
    List<DiffFile> getFiles();

    /**
     * Only added files.
     *
     * @return List of newly added files
     */
    @NonNull
    default List<DiffFile> getAddedFiles() {
        return getFiles().stream()
            .filter(f -> f.getStatus() == FileStatus.ADDED)
            .toList();
    }

    /**
     * Only modified files.
     *
     * @return List of modified files
     */
    @NonNull
    default List<DiffFile> getModifiedFiles() {
        return getFiles().stream()
            .filter(f -> f.getStatus() == FileStatus.MODIFIED)
            .toList();
    }

    /**
     * Only deleted files.
     *
     * @return List of deleted files
     */
    @NonNull
    default List<DiffFile> getDeletedFiles() {
        return getFiles().stream()
            .filter(f -> f.getStatus() == FileStatus.DELETED)
            .toList();
    }

    /**
     * Get file by path.
     *
     * @param path File path
     * @return File or empty if not found
     */
    @NonNull
    default Optional<DiffFile> getFile(@NonNull String path) {
        return getFiles().stream()
            .filter(f -> f.getPath().equals(path))
            .findFirst();
    }

    /**
     * Files filtered by language.
     *
     * @param language Programming language
     * @return Files of that language
     */
    @NonNull
    default List<DiffFile> getFilesByLanguage(@NonNull PluginLanguage language) {
        return getFiles().stream()
            .filter(f -> f.getLanguage() == language)
            .toList();
    }

    /**
     * Total lines added across all files.
     *
     * @return Count of added lines
     */
    default int getTotalAddedLines() {
        return getFiles().stream()
            .mapToInt(DiffFile::getAddedLinesCount)
            .sum();
    }

    /**
     * Total lines removed across all files.
     *
     * @return Count of removed lines
     */
    default int getTotalRemovedLines() {
        return getFiles().stream()
            .mapToInt(DiffFile::getRemovedLinesCount)
            .sum();
    }
}
```

## Diff File

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * A file in the diff.
 */
public interface DiffFile {

    /**
     * File path.
     *
     * @return Path relative to repository root
     */
    @NonNull
    String getPath();

    /**
     * File status.
     *
     * @return Status (added, modified, deleted, renamed)
     */
    @NonNull
    FileStatus getStatus();

    /**
     * Detected programming language.
     *
     * @return Language or UNKNOWN if not detected
     */
    @NonNull
    PluginLanguage getLanguage();

    /**
     * Old path if renamed.
     *
     * @return Old path or null if not renamed
     */
    @Nullable
    String getOldPath();

    /**
     * All diff lines.
     *
     * @return List of all diff lines
     */
    @NonNull
    List<DiffLine> getLines();

    /**
     * Only added lines.
     *
     * @return List of added lines
     */
    @NonNull
    List<DiffLine> getAddedLines();

    /**
     * Only removed lines.
     *
     * @return List of removed lines
     */
    @NonNull
    List<DiffLine> getRemovedLines();

    /**
     * Only context lines.
     *
     * @return List of context lines
     */
    @NonNull
    default List<DiffLine> getContextLines() {
        return getLines().stream()
            .filter(l -> l.getType() == LineType.CONTEXT)
            .toList();
    }

    /**
     * Count of added lines.
     *
     * @return Count
     */
    default int getAddedLinesCount() {
        return getAddedLines().size();
    }

    /**
     * Count of removed lines.
     *
     * @return Count
     */
    default int getRemovedLinesCount() {
        return getRemovedLines().size();
    }

    /**
     * File extension.
     *
     * @return Extension including dot (e.g., ".java")
     */
    @Nullable
    default String getExtension() {
        String path = getPath();
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot) : null;
    }

    /**
     * File name without extension.
     *
     * @return File name
     */
    @NonNull
    default String getFileName() {
        String path = getPath();
        int lastSlash = path.lastIndexOf('/');
        int lastDot = path.lastIndexOf('.');
        String name = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        if (lastDot > 0) {
            name = name.substring(0, name.lastIndexOf('.'));
        }
        return name;
    }
}
```

## File Status

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * File status in diff.
 */
public enum FileStatus {

    /**
     * New file added.
     */
    ADDED,

    /**
     * Existing file modified.
     */
    MODIFIED,

    /**
     * File deleted.
     */
    DELETED,

    /**
     * File renamed.
     */
    RENAMED
}
```

## Diff Line

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * A line in the diff.
 */
public interface DiffLine {

    /**
     * Line number in old file.
     *
     * @return Line number (1-based) or null
     */
    @Nullable
    Integer getOldLineNumber();

    /**
     * Line number in new file.
     *
     * @return Line number (1-based) or null
     */
    @Nullable
    Integer getNewLineNumber();

    /**
     * Line type.
     *
     * @return Type (added, removed, context)
     */
    @NonNull
    LineType getType();

    /**
     * Line content without diff marker.
     *
     * @return Line content
     */
    @NonNull
    String getContent();

    /**
     * Raw line with diff marker.
     *
     * @return Raw line (e.g., "+ return true;")
     */
    @NonNull
    String getRawContent();

    /**
     * Get line number for reporting.
     *
     * <p>Returns the appropriate line number based on type:
     * <ul>
     *   <li>ADDED: Returns new line number</li>
     *   <li>REMOVED: Returns old line number</li>
     *   <li>CONTEXT: Returns new line number</li>
     * </ul>
     *
     * @return Line number or null
     */
    @Nullable
    default Integer getLineNumber() {
        return switch (getType()) {
            case ADDED, CONTEXT -> getNewLineNumber();
            case REMOVED -> getOldLineNumber();
        };
    }

    /**
     * Check if line is empty.
     *
     * @return True if content is blank
     */
    default boolean isEmpty() {
        return getContent().isBlank();
    }

    /**
     * Check if line is a comment.
     *
     * @return True if appears to be a comment
     */
    default boolean isComment() {
        String content = getContent().trim();
        return content.startsWith("//")
            || content.startsWith("#")
            || content.startsWith("/*")
            || content.startsWith("*");
    }
}
```

## Line Type

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Line type in diff.
 */
public enum LineType {

    /**
     * Line added in new version.
     * Prefixed with '+' in diff.
     */
    ADDED,

    /**
     * Line removed from old version.
     * Prefixed with '-' in diff.
     */
    REMOVED,

    /**
     * Context line (unchanged).
     * Prefixed with ' ' in diff.
     */
    CONTEXT
}
```

## Project Configuration

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Project configuration.
 */
public interface ProjectConfig {

    /**
     * Project identifier.
     *
     * @return Project ID
     */
    Long getProjectId();

    /**
     * Project name.
     *
     * @return Name
     */
    String getProjectName();

    /**
     * Primary language.
     *
     * @return Language (e.g., "java", "typescript")
     */
    String getLanguage();

    /**
     * Build tool.
     *
     * @return Build tool (maven, gradle, npm, etc.)
     */
    String getBuildTool();

    /**
     * Framework.
     *
     * @return Framework (spring, react, etc.)
     */
    String getFramework();

    /**
     * Project-specific settings.
     *
     * @return Settings map
     */
    Map<String, Object> getSettings();

    /**
     * Get setting value.
     *
     * @param key Setting key
     * @return Value or null
     */
    @Nullable
    default Object getSetting(String key) {
        return getSettings().get(key);
    }

    /**
     * Get setting with default.
     *
     * @param key Setting key
     * @param defaultValue Default value
     * @return Value or default
     */
    @SuppressWarnings("unchecked")
    default <T> T getSetting(String key, T defaultValue) {
        Object value = getSettings().get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Check if feature is enabled.
     *
     * @param feature Feature name
     * @return True if enabled
     */
    default boolean isFeatureEnabled(String feature) {
        Object value = getSetting("features." + feature);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }
}
```

## Usage Examples

### Iterate Over Files

```java
@Override
protected AnalysisResult doAnalyze(AnalysisRequest request) {
    List<Issue> issues = new ArrayList<>();
    Diff diff = request.getDiff();

    for (DiffFile file : diff.getFiles()) {
        // Skip deleted files
        if (file.getStatus() == FileStatus.DELETED) {
            continue;
        }

        // Skip unsupported languages
        if (!getSupportedLanguages().contains(file.getLanguage())) {
            continue;
        }

        // Analyze added lines
        for (DiffLine line : file.getAddedLines()) {
            if (shouldFlag(line)) {
                issues.add(createIssue(file, line));
            }
        }
    }

    return result(issues);
}
```

### Filter by Language

```java
@Override
protected AnalysisResult doAnalyze(AnalysisRequest request) {
    List<Issue> issues = new ArrayList<>();
    Diff diff = request.getDiff();

    // Get only Java files
    for (DiffFile file : diff.getFilesByLanguage(PluginLanguage.JAVA)) {
        issues.addAll(analyzeJavaFile(file));
    }

    // Get only TypeScript files
    for (DiffFile file : diff.getFilesByLanguage(PluginLanguage.TYPESCRIPT)) {
        issues.addAll(analyzeTsFile(file));
    }

    return result(issues);
}
```

### Access Context Lines

```java
@Override
protected AnalysisResult doAnalyze(AnalysisRequest request) {
    List<Issue> issues = new ArrayList<>();

    for (DiffFile file : request.getDiff().getFiles()) {
        for (DiffLine addedLine : file.getAddedLines()) {
            // Get surrounding context
            List<DiffLine> context = getSurroundingContext(
                file,
                addedLine.getNewLineNumber(),
                3  // 3 lines before and after
            );

            // Analyze with context
            if (isIssueWithContext(addedLine, context)) {
                issues.add(createIssueWithContext(file, addedLine, context));
            }
        }
    }

    return result(issues);
}

private List<DiffLine> getSurroundingContext(
    DiffFile file,
    Integer lineNumber,
    int contextLines
) {
    return file.getLines().stream()
        .filter(l -> {
            Integer num = l.getNewLineNumber();
            return num != null
                && num >= lineNumber - contextLines
                && num <= lineNumber + contextLines;
        })
        .toList();
}
```

### Check File Size

```java
@Override
protected AnalysisResult doAnalyze(AnalysisRequest request) {
    List<Issue> issues = new ArrayList<>();
    Diff diff = request.getDiff();

    // Warn about large files
    for (DiffFile file : diff.getFiles()) {
        int addedLines = file.getAddedLinesCount();

        if (addedLines > 500) {
            issues.add(Issue.builder()
                .severity(Severity.MEDIUM)
                .type(IssueType.CODE_SMELL)
                .rule("LARGE_FILE")
                .filePath(file.getPath())
                .message(String.format(
                    "Large file change: %d lines added",
                    addedLines
                ))
                .suggestion("Consider splitting into smaller files")
                .build());
        }
    }

    return result(issues);
}
```

### Track Modified Lines

```java
@Override
protected AnalysisResult doAnalyze(AnalysisRequest request) {
    List<Issue> issues = new ArrayList<>();

    for (DiffFile file : request.getDiff().getModifiedFiles()) {
        // Track which functions were modified
        Set<String> modifiedFunctions = findModifiedFunctions(file);

        for (String function : modifiedFunctions) {
            // Check if function has tests
            if (!hasTests(file, function)) {
                issues.add(Issue.builder()
                    .severity(Severity.MEDIUM)
                    .type(IssueType.CODE_SMELL)
                    .rule("MISSING_TEST")
                    .filePath(file.getPath())
                    .message(String.format(
                        "Function '%s' modified but no tests found",
                        function
                    ))
                    .suggestion("Add tests for modified functions")
                    .build());
            }
        }
    }

    return result(issues);
}
```

### Cross-File Analysis

```java
@Override
protected AnalysisResult doAnalyze(AnalysisRequest request) {
    List<Issue> issues = new ArrayList<>();
    Diff diff = request.getDiff();

    // Build index of modified types
    Map<String, List<DiffFile>> typesToFiles = new HashMap<>();

    for (DiffFile file : diff.getFiles()) {
        for (String type : extractTypes(file)) {
            typesToFiles.computeIfAbsent(type, k -> new ArrayList<>())
                .add(file);
        }
    }

    // Check for inconsistencies
    for (Map.Entry<String, List<DiffFile>> entry : typesToFiles.entrySet()) {
        String type = entry.getKey();
        List<DiffFile> files = entry.getValue();

        if (files.size() > 1) {
            // Same type modified in multiple files
            issues.add(Issue.builder()
                .severity(Severity.LOW)
                .type(IssueType.CODE_SMELL)
                .rule("DUPLICATE_TYPE")
                .filePath(files.get(0).getPath())
                .message(String.format(
                    "Type '%s' defined in multiple files",
                    type
                ))
                .build());
        }
    }

    return result(issues);
}
```

## Best Practices

### 1. Always Check Language

```java
// Good: Check language first
for (DiffFile file : diff.getFiles()) {
    if (!getSupportedLanguages().contains(file.getLanguage())) {
        continue;  // Skip unsupported files
    }
    // Analyze...
}

// Bad: Assume language support
for (DiffFile file : diff.getFiles()) {
    // Might fail on unsupported languages
    analyze(file);
}
```

### 2. Filter by Line Type

```java
// Good: Only analyze added lines
for (DiffLine line : file.getAddedLines()) {
    // These are new lines
}

// Bad: Analyze all lines
for (DiffLine line : file.getLines()) {
    // Includes removed and context lines
}
```

### 3. Check File Status

```java
// Good: Handle different statuses
switch (file.getStatus()) {
    case ADDED:
        // New file analysis
        break;
    case MODIFIED:
        // Modified file analysis
        break;
    case DELETED:
        // Skip deleted files
        continue;
    case RENAMED:
        // Handle renamed files
        break;
}

// Bad: Assume all files exist
String content = readFile(file.getPath());  // Might fail for deleted files
```

## Next Steps

- [Analysis Result](/docs/plugin-development/api-reference/analysis-result) - Result details
- [Examples](/docs/plugin-development/examples/) - Plugin examples
