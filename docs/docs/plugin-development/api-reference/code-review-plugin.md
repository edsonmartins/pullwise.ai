# CodeReviewPlugin Interface

Core interface for creating code review plugins.

## Overview

The `CodeReviewPlugin` interface is the foundation for all plugins in Pullwise. Implementing this interface allows your code to analyze changes and report issues.

## Interface Definition

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Main interface for code review plugins.
 *
 * <p>All plugins must implement this interface to be
 * discovered and loaded by the plugin system.
 *
 * @since 1.0.0
 */
public interface CodeReviewPlugin {

    /**
     * Unique identifier for this plugin.
     *
     * <p>Must follow reverse domain notation:
     * {@code com.company.plugin-name}
     *
     * @return Unique plugin identifier
     */
    String getId();

    /**
     * Human-readable name.
     *
     * @return Plugin name
     */
    String getName();

    /**
     * Plugin version.
     *
     * @return Version string (e.g., "1.0.0")
     */
    String getVersion();

    /**
     * Plugin type.
     *
     * @return The type of analysis this plugin performs
     * @see PluginType
     */
    PluginType getType();

    /**
     * Supported programming languages.
     *
     * @return Set of languages this plugin can analyze
     */
    Set<PluginLanguage> getSupportedLanguages();

    /**
     * Initialize the plugin with configuration.
     *
     * @throws PluginException if initialization fails
     */
    void initialize() throws PluginException;

    /**
     * Analyze code changes.
     *
     * @param request The analysis request containing diff and context
     * @return Analysis result with any issues found
     * @throws PluginException if analysis fails
     */
    AnalysisResult analyze(AnalysisRequest request) throws PluginException;

    /**
     * Cleanup resources.
     *
     * <p>Called when plugin is unloaded.
     */
    void shutdown();
}
```

## Plugin Type

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Categories of code review plugins.
 */
public enum PluginType {

    /**
     * Static Application Security Testing.
     * Detects security vulnerabilities.
     */
    SAST,

    /**
     * Code quality and style checking.
     * Enforces coding standards.
     */
    LINTER,

    /**
     * Security-specific analysis beyond SAST.
     * Cryptography, secrets, authentication.
     */
    SECURITY,

    /**
     * Performance analysis.
     * Identifies bottlenecks and inefficiencies.
     */
    PERFORMANCE,

    /**
     * Custom LLM integration.
     * Uses external AI models.
     */
    CUSTOM_LLM
}
```

## Plugin Language

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Programming languages supported by plugins.
 */
public enum PluginLanguage {

    // JVM Languages
    JAVA,
    KOTLIN,
    SCALA,

    // JavaScript/TypeScript
    JAVASCRIPT,
    TYPESCRIPT,

    // Compiled
    GO,
    RUST,
    C,
    CPP,

    // Scripting
    PYTHON,
    RUBY,
    PHP,

    // Config/Data
    YAML,
    JSON,
    XML,

    // Markup
    MARKDOWN,
    HTML,
    CSS
}
```

## Analysis Request

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Request object passed to plugins for analysis.
 */
public interface AnalysisRequest {

    /**
     * The diff to analyze.
     *
     * @return Code diff containing added, modified, and removed lines
     */
    Diff getDiff();

    /**
     * Project configuration.
     *
     * @return Project settings and metadata
     */
    ProjectConfig getProjectConfig();

    /**
     * Plugin-specific configuration.
     *
     * @return Configuration map for this plugin
     */
    Map<String, Object> getPluginConfig();

    /**
     * Analysis context.
     *
     * @return Context information like branch, commit, etc.
     */
    AnalysisContext getContext();
}
```

## Analysis Result

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Result returned by plugin analysis.
 */
public interface AnalysisResult {

    /**
     * Plugin that produced this result.
     *
     * @return Plugin identifier
     */
    String getPluginId();

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
    String getErrorMessage();

    /**
     * Issues found during analysis.
     *
     * @return List of issues (empty if none found)
     */
    List<Issue> getIssues();

    /**
     * Metrics about the analysis.
     *
     * @return Optional metrics (execution time, files scanned, etc.)
     */
    Optional<AnalysisMetrics> getMetrics();
}
```

## Issue

```java
package com.pullwise.api.domain.model;

/**
 * An issue found during code review.
 */
public class Issue {

    /** Issue identifier */
    private Long id;

    /** Severity level */
    private Severity severity;

    /** Issue type */
    private IssueType type;

    /** Rule that triggered this issue */
    private String rule;

    /** File path where issue was found */
    private String filePath;

    /** Start line number (1-based) */
    private Integer startLine;

    /** End line number (1-based) */
    private Integer endLine;

    /** Issue description */
    private String message;

    /** Suggested fix */
    private String suggestion;

    /** Code snippet */
    private String codeSnippet;

    /** Links to documentation */
    private List<String> documentationUrls;

    // Getters, setters, builder
}
```

## Severity

```java
package com.pullwise.api.domain.model;

/**
 * Severity levels for issues.
 */
public enum Severity {

    /**
     * Critical: Security vulnerability, data loss risk.
     * Blocks deployment.
     */
    CRITICAL(4),

    /**
     * High: Bug, serious performance issue.
     * Should fix before merge.
     */
    HIGH(3),

    /**
     * Medium: Code smell, minor performance issue.
     * Consider fixing.
     */
    MEDIUM(2),

    /**
     * Low: Style, documentation.
     * Nice to have.
     */
    LOW(1);

    private final int weight;

    Severity(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
```

## Issue Type

```java
package com.pullwise.api.domain.model;

/**
 * Categories of issues.
 */
public enum IssueType {

    /** Security vulnerability */
    VULNERABILITY,

    /** Bug */
    BUG,

    /** Code smell */
    CODE_SMELL,

    /** Performance issue */
    PERFORMANCE,

    /** Style violation */
    STYLE,

    /** Documentation issue */
    DOCUMENTATION,

    /** Duplication */
    DUPLICATION,

    /** Security best practice */
    SECURITY_PRACTICE
}
```

## Plugin Exception

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Exception thrown by plugins.
 */
public class PluginException extends Exception {

    private final String pluginId;
    private final boolean recoverable;

    public PluginException(
        String pluginId,
        String message,
        boolean recoverable
    ) {
        super(message);
        this.pluginId = pluginId;
        this.recoverable = recoverable;
    }

    public PluginException(
        String pluginId,
        String message,
        Throwable cause,
        boolean recoverable
    ) {
        super(message, cause);
        this.pluginId = pluginId;
        this.recoverable = recoverable;
    }

    public String getPluginId() {
        return pluginId;
    }

    /**
     * Whether the error is recoverable.
     *
     * <p>Recoverable errors allow the review to continue.
     * Non-recoverable errors fail the entire review.
     *
     * @return true if recoverable
     */
    public boolean isRecoverable() {
        return recoverable;
    }
}
```

## Example Implementation

```java
package com.example.pullwise.plugin;

import com.pullwise.api.application.service.plugin.api.*;
import com.pullwise.api.domain.model.*;
import java.util.*;

/**
 * Simple example plugin that detects TODO comments.
 */
public class TodoCommentPlugin implements CodeReviewPlugin {

    private static final Set<PluginLanguage> SUPPORTED_LANGUAGES =
        Set.of(PluginLanguage.JAVA, PluginLanguage.TYPESCRIPT);

    private Map<String, Object> config;

    @Override
    public String getId() {
        return "com.example.todo-comment-plugin";
    }

    @Override
    public String getName() {
        return "TODO Comment Detector";
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
        return SUPPORTED_LANGUAGES;
    }

    @Override
    public void initialize() throws PluginException {
        // Load configuration
        this.config = getConfiguration();
    }

    @Override
    public AnalysisResult analyze(AnalysisRequest request) {
        List<Issue> issues = new ArrayList<>();

        for (DiffFile file : request.getDiff().getFiles()) {
            if (!isSupported(file.getLanguage())) {
                continue;
            }

            for (DiffLine line : file.getAddedLines()) {
                if (line.getContent().contains("TODO")) {
                    issues.add(createTodoIssue(file, line));
                }
            }
        }

        return AnalysisResult.builder()
            .pluginId(getId())
            .success(true)
            .issues(issues)
            .build();
    }

    @Override
    public void shutdown() {
        // Cleanup if needed
    }

    private Issue createTodoIssue(DiffFile file, DiffLine line) {
        return Issue.builder()
            .severity(Severity.LOW)
            .type(IssueType.DOCUMENTATION)
            .rule("TODO_COMMENT")
            .filePath(file.getPath())
            .startLine(line.getLineNumber())
            .message("TODO comment found in new code")
            .suggestion("Resolve TODO or move to issue tracker")
            .codeSnippet(line.getContent())
            .build();
    }

    private boolean isSupported(PluginLanguage language) {
        return SUPPORTED_LANGUAGES.contains(language);
    }
}
```

## Best Practices

### 1. Plugin ID

Use reverse domain notation:

```java
// Good
"com.company.pullwise.plugin-name"

// Bad
"my-plugin"
"plugin"
```

### 2. Error Handling

Always use `PluginException`:

```java
@Override
public AnalysisResult analyze(AnalysisRequest request) {
    try {
        return doAnalysis(request);
    } catch (IOException e) {
        throw new PluginException(
            getId(),
            "Failed to analyze: " + e.getMessage(),
            e,
            true  // Recoverable
        );
    }
}
```

### 3. Language Support

Be explicit about supported languages:

```java
@Override
public Set<PluginLanguage> getSupportedLanguages() {
    // Good: Explicit set
    return Set.of(PluginLanguage.JAVA, PluginLanguage.KOTLIN);

    // Bad: All languages
    return EnumSet.allOf(PluginLanguage.class);
}
```

### 4. Thread Safety

Plugins must be thread-safe:

```java
public class MyPlugin implements CodeReviewPlugin {

    // Good: Immutable state
    private final Config config;

    // Bad: Shared mutable state
    private List<Issue> sharedIssues;

    @Override
    public AnalysisResult analyze(AnalysisRequest request) {
        // Use local variables
        List<Issue> issues = new ArrayList<>();

        // Don't use shared state
        // sharedIssues.clear(); // WRONG!
    }
}
```

## Next Steps

- [Abstract Plugin](/docs/plugin-development/api-reference/abstract-plugin) - Base implementation
- [Plugin Context](/docs/plugin-development/api-reference/plugin-context) - Context API
- [Examples](/docs/plugin-development/examples/) - Plugin examples
