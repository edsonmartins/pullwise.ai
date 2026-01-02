# AbstractCodeReviewPlugin

Base class for implementing code review plugins.

## Overview

`AbstractCodeReviewPlugin` provides a convenient base implementation of the `CodeReviewPlugin` interface. It handles common functionality and reduces boilerplate.

## Class Definition

```java
package com.pullwise.api.application.service.plugin.api;

/**
 * Abstract base class for code review plugins.
 *
 * <p>This class provides common functionality for plugin
 * implementation including:
 * <ul>
 *   <li>Configuration management</li>
 *   <li>Logging infrastructure</li>
 *   <li>Error handling</li>
 *   <li>Language filtering</li>
 * </ul>
 *
 * @since 1.0.0
 */
public abstract class AbstractCodeReviewPlugin implements CodeReviewPlugin {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private Map<String, Object> config;
    private boolean initialized = false;

    // ... methods
}
```

## Abstract Methods

Implement these methods in your plugin:

```java
/**
 * Unique plugin identifier.
 */
@NonNull
public abstract String getId();

/**
 * Human-readable plugin name.
 */
@NonNull
public abstract String getName();

/**
 * Plugin version.
 */
@NonNull
public abstract String getVersion();

/**
 * Plugin type.
 */
@NonNull
public abstract PluginType getType();

/**
 * Supported languages.
 */
@NonNull
public abstract Set<PluginLanguage> getSupportedLanguages();

/**
 * Perform initialization.
 *
 * <p>Called once when plugin is loaded.
 * Use this to read configuration and setup resources.
 *
 * @throws PluginException if initialization fails
 */
protected abstract void doInitialize() throws PluginException;

/**
 * Perform analysis.
 *
 * @param request The analysis request
 * @return Analysis result with issues found
 * @throws PluginException if analysis fails
 */
@NonNull
protected abstract AnalysisResult doAnalyze(AnalysisRequest request)
    throws PluginException;
```

## Template Methods

These methods have default implementations but can be overridden:

```java
/**
 * Initialize the plugin.
 *
 * <p>Default implementation:
 * <ol>
 *   <li>Calls {@link #doInitialize()}</li>
 *   <li>Validates configuration</li>
 *   <li>Logs initialization</li>
 * </ol>
 */
@Override
public void initialize() throws PluginException {
    if (initialized) {
        log.warn("Plugin {} already initialized", getId());
        return;
    }

    log.info("Initializing plugin: {} v{}", getName(), getVersion());
    long start = System.currentTimeMillis();

    try {
        doInitialize();
        initialized = true;

        long duration = System.currentTimeMillis() - start;
        log.info("Plugin {} initialized in {}ms", getId(), duration);
    } catch (Exception e) {
        throw new PluginException(getId(), "Initialization failed", e, false);
    }
}

/**
 * Analyze code changes.
 *
 * <p>Default implementation:
 * <ol>
 *   <li>Validates request</li>
 *   <li>Filters by language</li>
 *   <li>Calls {@link #doAnalyze(AnalysisRequest)}</li>
 *   <li>Handles exceptions</li>
 * </ol>
 */
@Override
@NonNull
public AnalysisResult analyze(@NonNull AnalysisRequest request)
        throws PluginException {

    if (!initialized) {
        throw new PluginException(getId(), "Plugin not initialized", false);
    }

    if (!shouldAnalyze(request)) {
        return emptyResult();
    }

    long start = System.currentTimeMillis();

    try {
        AnalysisResult result = doAnalyze(request);

        long duration = System.currentTimeMillis() - start;
        log.debug("Analysis completed in {}ms, found {} issues",
            duration, result.getIssues().size());

        return result;
    } catch (PluginException e) {
        throw e;
    } catch (Exception e) {
        log.error("Analysis failed for plugin {}", getId(), e);
        throw new PluginException(getId(), "Analysis failed", e, true);
    }
}

/**
 * Shutdown the plugin.
 *
 * <p>Default implementation clears state.
 * Override to release resources.
 */
@Override
public void shutdown() {
    log.info("Shutting down plugin: {}", getId());
    initialized = false;
    config = null;
}
```

## Configuration Methods

Helper methods for accessing configuration:

```java
/**
 * Get configuration value.
 *
 * @param key Configuration key
 * @return Value or null if not found
 */
@Nullable
protected Object getConfig(@NonNull String key) {
    return config != null ? config.get(key) : null;
}

/**
 * Get string configuration value.
 *
 * @param key Configuration key
 * @param defaultValue Default value if not found
 * @return String value
 */
@NonNull
protected String getConfigString(
    @NonNull String key,
    @NonNull String defaultValue
) {
    Object value = getConfig(key);
    return value != null ? value.toString() : defaultValue;
}

/**
 * Get boolean configuration value.
 *
 * @param key Configuration key
 * @param defaultValue Default value if not found
 * @return Boolean value
 */
protected boolean getConfigBoolean(
    @NonNull String key,
    boolean defaultValue
) {
    Object value = getConfig(key);
    if (value instanceof Boolean) {
        return (Boolean) value;
    }
    if (value instanceof String) {
        return Boolean.parseBoolean((String) value);
    }
    return defaultValue;
}

/**
 * Get integer configuration value.
 *
 * @param key Configuration key
 * @param defaultValue Default value if not found
 * @return Integer value
 */
protected int getConfigInt(
    @NonNull String key,
    int defaultValue
) {
    Object value = getConfig(key);
    if (value instanceof Number) {
        return ((Number) value).intValue();
    }
    if (value instanceof String) {
        try {
            return Integer.parseInt((String) value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    return defaultValue;
}

/**
 * Get list configuration value.
 *
 * @param key Configuration key
 * @return List of values or empty list
 */
@NonNull
@SuppressWarnings("unchecked")
protected List<String> getConfigList(@NonNull String key) {
    Object value = getConfig(key);
    if (value instanceof List) {
        return (List<String>) value;
    }
    return Collections.emptyList();
}
```

## Language Filtering

```java
/**
 * Check if plugin should analyze this request.
 *
 * <p>Default implementation checks if any files
 * are in supported languages.
 *
 * @param request Analysis request
 * @return True if should analyze
 */
protected boolean shouldAnalyze(@NonNull AnalysisRequest request) {
    return request.getDiff().getFiles().stream()
        .anyMatch(file -> getSupportedLanguages().contains(file.getLanguage()));
}

/**
 * Filter files by supported language.
 *
 * @param files All files
 * @return Files in supported languages
 */
@NonNull
protected List<DiffFile> filterSupportedFiles(@NonNull List<DiffFile> files) {
    return files.stream()
        .filter(file -> getSupportedLanguages().contains(file.getLanguage()))
        .toList();
}
```

## Result Builders

```java
/**
 * Create empty result.
 *
 * @return Empty successful result
 */
@NonNull
protected AnalysisResult emptyResult() {
    return AnalysisResult.builder()
        .pluginId(getId())
        .success(true)
        .issues(Collections.emptyList())
        .build();
}

/**
 * Create error result.
 *
 * @param message Error message
 * @return Failed result
 */
@NonNull
protected AnalysisResult errorResult(@NonNull String message) {
    return AnalysisResult.builder()
        .pluginId(getId())
        .success(false)
        .errorMessage(message)
        .build();
}

/**
 * Create result with issues.
 *
 * @param issues Issues found
 * @return Successful result with issues
 */
@NonNull
protected AnalysisResult result(@NonNull List<Issue> issues) {
    return AnalysisResult.builder()
        .pluginId(getId())
        .success(true)
        .issues(issues)
        .build();
}
```

## Complete Example

```java
package com.example.pullwise.plugin;

import com.pullwise.api.application.service.plugin.api.*;
import com.pullwise.api.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.*;

/**
 * Plugin to detect hardcoded credentials.
 */
public class HardcodedSecretsPlugin extends AbstractCodeReviewPlugin {

    private static final Logger log = LoggerFactory.getLogger(
        HardcodedSecretsPlugin.class
    );

    // Patterns for detecting secrets
    private static final Map<String, Pattern> SECRET_PATTERNS = Map.of(
        "API_KEY", Pattern.compile("(?i)(api[_-]?key|apikey)[\"']?\\s*[:=]\\s*[\"']([a-z0-9]{32,})[\"']"),
        "PASSWORD", Pattern.compile("(?i)(password|passwd|pwd)[\"']?\\s*[:=]\\s*[\"']([^\"']{8,})[\"']"),
        "SECRET", Pattern.compile("(?i)(secret|token)[\"']?\\s*[:=]\\s*[\"']([a-z0-9]{20,})[\"']"),
        "AWS_KEY", Pattern.compile("AKIA[0-9A-Z]{16}")
    );

    private boolean enabled = true;
    private Set<String> excludedPatterns = new HashSet<>();

    @Override
    public String getId() {
        return "com.example.hardcoded-secrets-plugin";
    }

    @Override
    public String getName() {
        return "Hardcoded Secrets Detector";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginType getType() {
        return PluginType.SECURITY;
    }

    @Override
    public Set<PluginLanguage> getSupportedLanguages() {
        return EnumSet.allOf(PluginLanguage.class);
    }

    @Override
    protected void doInitialize() throws PluginException {
        // Read configuration
        enabled = getConfigBoolean("enabled", true);
        excludedPatterns = new HashSet<>(getConfigList("excludePatterns"));

        log.info("Hardcoded secrets plugin initialized: enabled={}, excluded={}",
            enabled, excludedPatterns);
    }

    @Override
    protected AnalysisResult doAnalyze(AnalysisRequest request) {
        if (!enabled) {
            return emptyResult();
        }

        List<Issue> issues = new ArrayList<>();

        for (DiffFile file : filterSupportedFiles(request.getDiff().getFiles())) {
            for (DiffLine line : file.getAddedLines()) {
                issues.addAll(findSecrets(file, line));
            }
        }

        return result(issues);
    }

    private List<Issue> findSecrets(DiffFile file, DiffLine line) {
        List<Issue> issues = new ArrayList<>();
        String content = line.getContent();

        for (Map.Entry<String, Pattern> entry : SECRET_PATTERNS.entrySet()) {
            String secretType = entry.getKey();
            Pattern pattern = entry.getValue();

            if (excludedPatterns.contains(secretType)) {
                continue;
            }

            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                issues.add(Issue.builder()
                    .severity(Severity.CRITICAL)
                    .type(IssueType.VULNERABILITY)
                    .rule("HARDCODED_SECRET_" + secretType)
                    .filePath(file.getPath())
                    .startLine(line.getLineNumber())
                    .endLine(line.getLineNumber())
                    .message(String.format(
                        "Potential hardcoded %s detected",
                        secretType.toLowerCase()
                    ))
                    .suggestion("Use environment variables or secret management")
                    .codeSnippet(content.trim())
                    .documentationUrls(List.of(
                        "https://owasp.org/www-community//Hard_coding_secrets"
                    ))
                    .build());
            }
        }

        return issues;
    }
}
```

## Configuration Example

```yaml
# application.yml
pullwise:
  plugins:
    hardcoded-secrets:
      enabled: true
      excludePatterns:
        - AWS_KEY  # Exclude if using AWS-specific detection
      # Exclude specific files
      excludeFiles:
        - "**/test/**"
        - "**/example/**"
```

## Best Practices

### 1. Use Log from Base Class

```java
// Good: Use inherited log
log.info("Processing file: {}", file.getPath());

// Bad: Create your own logger
private static final Logger MY_LOG = LoggerFactory.getLogger(MyPlugin.class);
```

### 2. Override Should Analyze for Custom Filtering

```java
@Override
protected boolean shouldAnalyze(AnalysisRequest request) {
    // Skip test files
    boolean hasTestFiles = request.getDiff().getFiles().stream()
        .anyMatch(f -> f.getPath().contains("/test/"));

    if (hasTestFiles && !getConfigBoolean("analyzeTests", false)) {
        return false;
    }

    return super.shouldAnalyze(request);
}
```

### 3. Use Configuration Methods

```java
// Good: Use helper methods
boolean enabled = getConfigBoolean("enabled", true);
int timeout = getConfigInt("timeout", 5000);

// Bad: Manual casting
boolean enabled = (Boolean) config.get("enabled");
int timeout = (Integer) config.get("timeout");
```

### 4. Build Results with Helpers

```java
// Good: Use helper methods
return emptyResult();
return errorResult("Configuration error");
return result(issues);

// Bad: Build manually
return AnalysisResult.builder()
    .pluginId(getId())
    .success(true)
    .issues(Collections.emptyList())
    .build();
```

## Next Steps

- [Plugin Context](/docs/plugin-development/api-reference/plugin-context) - Context API
- [Analysis Request](/docs/plugin-development/api-reference/analysis-request) - Request details
- [Examples](/docs/plugin-development/examples/) - Plugin examples
