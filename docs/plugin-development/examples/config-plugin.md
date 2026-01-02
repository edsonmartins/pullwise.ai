# Configuration Plugin Example

Plugin with custom configuration options.

## Overview

This example demonstrates how to create a plugin with rich configuration options, including validation, default values, and runtime updates.

## Plugin Class

```java
package com.example.pullwise.plugin;

import com.pullwise.api.application.service.plugin.api.*;
import com.pullwise.api.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.*;

/**
 * Configurable naming convention plugin.
 */
public class NamingConventionPlugin extends AbstractCodeReviewPlugin {

    private static final Logger log = LoggerFactory.getLogger(NamingConventionPlugin.class);
    private static final Set<PluginLanguage> SUPPORTED = Set.of(
        PluginLanguage.JAVA,
        PluginLanguage.TYPESCRIPT
    );

    // Configuration with defaults
    private NamingConfig config;

    @Override
    public String getId() {
        return "com.example.naming-convention";
    }

    @Override
    public String getName() {
        return "Naming Convention Checker";
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
        // Load and validate configuration
        this.config = loadConfig();
        log.info("NamingConventionPlugin initialized with config: {}", config);
    }

    @Override
    protected AnalysisResult doAnalyze(AnalysisRequest request) {
        List<Issue> issues = new ArrayList<>();

        for (DiffFile file : filterSupportedFiles(request.getDiff().getFiles())) {
            for (DiffLine line : file.getAddedLines()) {
                issues.addAll(checkLine(file, line));
            }
        }

        return result(issues);
    }

    /**
     * Load configuration with validation.
     */
    private NamingConfig loadConfig() throws PluginException {
        NamingConfig cfg = new NamingConfig();

        // Load class naming pattern
        cfg.classPattern = getConfigString(
            "classPattern",
            NamingConfig.DEFAULT_CLASS_PATTERN
        );
        validatePattern(cfg.classPattern, "classPattern");

        // Load method naming pattern
        cfg.methodPattern = getConfigString(
            "methodPattern",
            NamingConfig.DEFAULT_METHOD_PATTERN
        );
        validatePattern(cfg.methodPattern, "methodPattern");

        // Load variable naming pattern
        cfg.variablePattern = getConfigString(
            "variablePattern",
            NamingConfig.DEFAULT_VARIABLE_PATTERN
        );
        validatePattern(cfg.variablePattern, "variablePattern");

        // Load constant pattern
        cfg.constantPattern = getConfigString(
            "constantPattern",
            NamingConfig.DEFAULT_CONSTANT_PATTERN
        );
        validatePattern(cfg.constantPattern, "constantPattern");

        // Load boolean flags
        cfg.checkClasses = getConfigBoolean("checkClasses", true);
        cfg.checkMethods = getConfigBoolean("checkMethods", true);
        cfg.checkVariables = getConfigBoolean("checkVariables", true);
        cfg.checkConstants = getConfigBoolean("checkConstants", true);

        // Load prefixes to skip
        cfg.skipPrefixes = new HashSet<>(getConfigList("skipPrefixes"));

        // Load max length
        cfg.maxLength = getConfigInt("maxLength", 50);
        if (cfg.maxLength < 10 || cfg.maxLength > 100) {
            throw new PluginException(
                getId(),
                "maxLength must be between 10 and 100",
                false
            );
        }

        // Load severity levels
        cfg.severity = parseSeverity(
            getConfigString("severity", "MEDIUM")
        );

        return cfg;
    }

    /**
     * Validate regex pattern.
     */
    private void validatePattern(String pattern, String name)
            throws PluginException {
        try {
            Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            throw new PluginException(
                getId(),
                "Invalid pattern for " + name + ": " + e.getMessage(),
                false
            );
        }
    }

    /**
     * Parse severity from string.
     */
    private Severity parseSeverity(String value) {
        try {
            return Severity.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid severity '{}', using MEDIUM", value);
            return Severity.MEDIUM;
        }
    }

    /**
     * Check a line for naming violations.
     */
    private List<Issue> checkLine(DiffFile file, DiffLine line) {
        List<Issue> issues = new ArrayList<>();
        String content = line.getContent();

        // Skip comments and empty lines
        if (line.isComment() || line.isEmpty()) {
            return issues;
        }

        // Check class declarations
        if (config.checkClasses) {
            issues.addAll(checkClassDeclaration(file, line, content));
        }

        // Check method declarations
        if (config.checkMethods) {
            issues.addAll(checkMethodDeclaration(file, line, content));
        }

        // Check variable declarations
        if (config.checkVariables) {
            issues.addAll(checkVariableDeclaration(file, line, content));
        }

        // Check constant declarations
        if (config.checkConstants) {
            issues.addAll(checkConstantDeclaration(file, line, content));
        }

        return issues;
    }

    private List<Issue> checkClassDeclaration(DiffFile file, DiffLine line, String content) {
        List<Issue> issues = new ArrayList<>();

        // Match: class ClassName, interface InterfaceName, enum EnumName
        Pattern pattern = Pattern.compile(
            "(class|interface|enum)\\s+(\\w+)"
        );
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String name = matcher.group(2);
            String kind = matcher.group(1);

            if (!matchesPattern(name, config.classPattern)) {
                issues.add(createIssue(
                    file, line,
                    String.format("%s name '%s' doesn't match pattern '%s'",
                        capitalize(kind), name, config.classPattern),
                    String.format("Use pattern: %s", config.classPattern)
                ));
            }

            if (name.length() > config.maxLength) {
                issues.add(createIssue(
                    file, line,
                    String.format("%s name '%s' is too long (%d > %d)",
                        capitalize(kind), name, name.length(), config.maxLength),
                    "Use a shorter name"
                ));
            }
        }

        return issues;
    }

    private List<Issue> checkMethodDeclaration(DiffFile file, DiffLine line, String content) {
        List<Issue> issues = new ArrayList<>();

        // Match: methodName(, methodName (
        Pattern pattern = Pattern.compile(
            "(?:public|private|protected)?\\s*(?:static|abstract)?\\s*\\w+\\s+(\\w+)\\s*\\("
        );
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String name = matcher.group(1);

            // Skip overridden methods
            if (content.contains("@Override")) {
                return issues;
            }

            // Skip main method
            if ("main".equals(name)) {
                return issues;
            }

            // Skip configured prefixes
            if (shouldSkip(name)) {
                return issues;
            }

            if (!matchesPattern(name, config.methodPattern)) {
                issues.add(createIssue(
                    file, line,
                    String.format("Method name '%s' doesn't match pattern '%s'",
                        name, config.methodPattern),
                    String.format("Use pattern: %s", config.methodPattern)
                ));
            }
        }

        return issues;
    }

    private List<Issue> checkVariableDeclaration(DiffFile file, DiffLine line, String content) {
        List<Issue> issues = new ArrayList<>();

        // Match: Type varName =, var varName =, let varName =, const varName =
        Pattern pattern = Pattern.compile(
            "(?:\\w+\\s+|var|let|const)\\s+([a-z][a-zA-Z0-9]*)\\s*="
        );
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String name = matcher.group(1);

            if (shouldSkip(name)) {
                continue;
            }

            if (!matchesPattern(name, config.variablePattern)) {
                issues.add(createIssue(
                    file, line,
                    String.format("Variable name '%s' doesn't match pattern '%s'",
                        name, config.variablePattern),
                    String.format("Use pattern: %s", config.variablePattern)
                ));
            }
        }

        return issues;
    }

    private List<Issue> checkConstantDeclaration(DiffFile file, DiffLine line, String content) {
        List<Issue> issues = new ArrayList<>();

        // Match: static final TYPE CONST_NAME =, private static final TYPE CONST_NAME =
        Pattern pattern = Pattern.compile(
            "(?:static\\s+final|final\\s+static)\\s+\\w+\\s+([A-Z][A-Z0-9_]*)\\s*="
        );
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String name = matcher.group(1);

            if (!matchesPattern(name, config.constantPattern)) {
                issues.add(createIssue(
                    file, line,
                    String.format("Constant name '%s' doesn't match pattern '%s'",
                        name, config.constantPattern),
                    String.format("Use pattern: %s", config.constantPattern)
                ));
            }
        }

        return issues;
    }

    private boolean matchesPattern(String name, String patternStr) {
        try {
            Pattern pattern = Pattern.compile(patternStr);
            return pattern.matcher(name).matches();
        } catch (Exception e) {
            log.error("Failed to compile pattern: {}", patternStr, e);
            return true; // Don't fail on pattern error
        }
    }

    private boolean shouldSkip(String name) {
        return config.skipPrefixes.stream()
            .anyMatch(prefix -> name.startsWith(prefix));
    }

    private Issue createIssue(DiffFile file, DiffLine line, String message, String suggestion) {
        return Issue.builder()
            .severity(config.severity)
            .type(IssueType.CODE_SMELL)
            .rule("NAMING_CONVENTION")
            .filePath(file.getPath())
            .startLine(line.getLineNumber())
            .message(message)
            .suggestion(suggestion)
            .codeSnippet(line.getContent().trim())
            .build();
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Configuration class.
     */
    public static class NamingConfig {
        public static final String DEFAULT_CLASS_PATTERN = "[A-Z][a-zA-Z0-9]*";
        public static final String DEFAULT_METHOD_PATTERN = "[a-z][a-zA-Z0-9]*";
        public static final String DEFAULT_VARIABLE_PATTERN = "[a-z][a-zA-Z0-9]*";
        public static final String DEFAULT_CONSTANT_PATTERN = "[A-Z][A-Z0-9_]*";

        String classPattern = DEFAULT_CLASS_PATTERN;
        String methodPattern = DEFAULT_METHOD_PATTERN;
        String variablePattern = DEFAULT_VARIABLE_PATTERN;
        String constantPattern = DEFAULT_CONSTANT_PATTERN;

        boolean checkClasses = true;
        boolean checkMethods = true;
        boolean checkVariables = true;
        boolean checkConstants = true;

        Set<String> skipPrefixes = Set.of("_", "test", "mock", "fake");

        int maxLength = 50;
        Severity severity = Severity.MEDIUM;

        @Override
        public String toString() {
            return "NamingConfig{" +
                "classPattern='" + classPattern + '\'' +
                ", methodPattern='" + methodPattern + '\'' +
                ", variablePattern='" + variablePattern + '\'' +
                ", constantPattern='" + constantPattern + '\'' +
                ", checkClasses=" + checkClasses +
                ", checkMethods=" + checkMethods +
                ", checkVariables=" + checkVariables +
                ", checkConstants=" + checkConstants +
                ", skipPrefixes=" + skipPrefixes +
                ", maxLength=" + maxLength +
                ", severity=" + severity +
                '}';
        }
    }
}
```

## Configuration Options

### application.yml

```yaml
pullwise:
  plugins:
    naming-convention:
      enabled: true

      # Patterns (regex)
      classPattern: "[A-Z][a-zA-Z0-9]*"
      methodPattern: "[a-z][a-zA-Z0-9]*"
      variablePattern: "[a-z][a-zA-Z0-9]*"
      constantPattern: "[A-Z][A-Z0-9_]*"

      # Enable/disable checks
      checkClasses: true
      checkMethods: true
      checkVariables: true
      checkConstants: true

      # Skip names with these prefixes
      skipPrefixes:
        - "_"
        - "test"
        - "mock"

      # Maximum name length
      maxLength: 50

      # Severity level
      severity: MEDIUM  # CRITICAL, HIGH, MEDIUM, LOW
```

### Project-Specific Config

```yaml
# project-config.yml
plugins:
  naming-convention:
    # Stricter rules for backend
    classPattern: "[A-Z][a-zA-Z]*"  # No numbers in class names
    maxLength: 40

    # Allow test prefixes for test code
    skipPrefixes:
      - "_"
      - "test"
```

## Dynamic Configuration

### Reloading Configuration

```java
@Override
protected void doInitialize() throws PluginException {
    this.config = loadConfig();
}

/**
 * Reload configuration at runtime.
 */
public void reloadConfig() throws PluginException {
    log.info("Reloading configuration for {}", getId());
    this.config = loadConfig();
}

/**
 * Update specific config value.
 */
public void updateConfig(String key, Object value) throws PluginException {
    // Validate new value
    if ("maxLength".equals(key)) {
        int max = ((Number) value).intValue();
        if (max < 10 || max > 100) {
            throw new PluginException(
                getId(),
                "Invalid maxLength: " + max,
                false
            );
        }
    }

    // Update configuration
    config = loadConfig();  // Reload with new value
    log.info("Configuration updated: {}", key);
}
```

### Configuration Schema

```java
/**
 * Get configuration schema for UI.
 */
public Map<String, ConfigField> getConfigSchema() {
    Map<String, ConfigField> schema = new LinkedHashMap<>();

    schema.put("classPattern", ConfigField.builder()
        .type("regex")
        .label("Class Name Pattern")
        .description("Regex pattern for class names")
        .defaultValue(NamingConfig.DEFAULT_CLASS_PATTERN)
        .required(true)
        .build());

    schema.put("methodPattern", ConfigField.builder()
        .type("regex")
        .label("Method Name Pattern")
        .description("Regex pattern for method names")
        .defaultValue(NamingConfig.DEFAULT_METHOD_PATTERN)
        .required(true)
        .build());

    schema.put("maxLength", ConfigField.builder()
        .type("integer")
        .label("Maximum Name Length")
        .description("Maximum length for identifiers")
        .defaultValue(50)
        .min(10)
        .max(100)
        .required(true)
        .build());

    schema.put("severity", ConfigField.builder()
        .type("enum")
        .label("Severity Level")
        .description("Default severity for violations")
        .defaultValue("MEDIUM")
        .options(List.of("CRITICAL", "HIGH", "MEDIUM", "LOW"))
        .required(true)
        .build());

    return schema;
}
```

## Configuration Best Practices

### 1. Provide Sensible Defaults

```java
// Good: Default pattern
public static final String DEFAULT_PATTERN = "[A-Z][a-zA-Z0-9]*";

// Use default
String pattern = getConfigString("pattern", DEFAULT_PATTERN);

// Bad: No default
String pattern = getConfigString("pattern");  // May return null
```

### 2. Validate Configuration

```java
private void validateConfig() throws PluginException {
    if (config.minLength > config.maxLength) {
        throw new PluginException(
            getId(),
            "minLength cannot be greater than maxLength",
            false
        );
    }
}
```

### 3. Document Configuration

```java
/**
 * Configuration for naming convention plugin.
 *
 * <p>Supported properties:
 * <ul>
 *   <li>classPattern - Regex for class names (default: [A-Z][a-zA-Z0-9]*)</li>
 *   <li>methodPattern - Regex for method names (default: [a-z][a-zA-Z0-9]*)</li>
 *   <li>maxLength - Maximum name length (default: 50, range: 10-100)</li>
 *   <li>severity - Issue severity (default: MEDIUM)</li>
 * </ul>
 */
```

### 4. Type Conversion

```java
// Good: Handle type conversion
public int getConfigInt(String key, int defaultValue) {
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

// Good: Handle enum parsing
public Severity parseSeverity(String value) {
    if (value == null) {
        return Severity.MEDIUM;
    }
    try {
        return Severity.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
        log.warn("Unknown severity: {}", value);
        return Severity.MEDIUM;
    }
}
```

## Testing Configuration

### Unit Tests

```java
@Test
void shouldLoadDefaultConfig() throws PluginException {
    NamingConventionPlugin plugin = new NamingConventionPlugin();
    plugin.initialize();

    assertNotNull(plugin.getConfig());
    assertEquals("[A-Z][a-zA-Z0-9]*", plugin.getConfig().classPattern);
}

@Test
void shouldValidatePattern() {
    assertThrows(PluginException.class, () -> {
        NamingConventionPlugin plugin = new NamingConventionPlugin() {
            @Override
            protected void doInitialize() throws PluginException {
                validatePattern("[invalid(", "test");
            }
        };
    });
}

@Test
void shouldRejectInvalidMaxLength() {
    assertThrows(PluginException.class, () -> {
        plugin.setConfigValue("maxLength", 200);
        plugin.initialize();
    });
}
```

## Next Steps

- [External Tool](/docs/plugin-development/examples/external-tool) - Executable wrapper
- [Packaging](/docs/plugin-development/packaging/) - Packaging plugins
- [Testing](/docs/plugin-development/testing) - Testing plugins
