# Simple Linter Example

A basic example of creating a linter plugin.

## Overview

This example shows how to create a simple linter that checks for:
- Long methods (>50 lines)
- Long parameter lists (>5 parameters)
- Magic numbers
- Missing Javadoc on public methods

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
 * Simple Java code quality linter.
 */
public class SimpleLinterPlugin extends AbstractCodeReviewPlugin {

    private static final Logger log = LoggerFactory.getLogger(SimpleLinterPlugin.class);
    private static final Set<PluginLanguage> SUPPORTED = Set.of(PluginLanguage.JAVA);

    // Configuration
    private int maxMethodLines = 50;
    private int maxParameters = 5;
    private boolean requireJavadoc = true;

    @Override
    public String getId() {
        return "com.example.simple-linter";
    }

    @Override
    public String getName() {
        return "Simple Linter";
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
        maxMethodLines = getConfigInt("maxMethodLines", 50);
        maxParameters = getConfigInt("maxParameters", 5);
        requireJavadoc = getConfigBoolean("requireJavadoc", true);

        log.info("SimpleLinter initialized: maxMethodLines={}, maxParameters={}",
            maxMethodLines, maxParameters);
    }

    @Override
    protected AnalysisResult doAnalyze(AnalysisRequest request) {
        List<Issue> issues = new ArrayList<>();

        for (DiffFile file : filterSupportedFiles(request.getDiff().getFiles())) {
            // Skip test files
            if (file.getPath().contains("/test/")) {
                continue;
            }

            issues.addAll(analyzeJavaFile(file));
        }

        return result(issues);
    }

    private List<Issue> analyzeJavaFile(DiffFile file) {
        List<Issue> issues = new ArrayList<>();
        List<DiffLine> lines = file.getLines();

        // Track method context
        MethodContext currentMethod = null;

        for (int i = 0; i < lines.size(); i++) {
            DiffLine line = lines.get(i);
            String content = line.getContent();

            // Check for method declaration
            if (isMethodDeclaration(content)) {
                if (currentMethod != null) {
                    // Check previous method length
                    issues.addAll(checkMethodLength(currentMethod));
                }

                // Start new method context
                currentMethod = new MethodContext(
                    file,
                    extractMethodName(content),
                    line.getNewLineNumber(),
                    extractParameterCount(content),
                    hasJavadoc(lines, i)
                );
            }

            // Track method lines
            if (currentMethod != null) {
                if (content.contains("}")) {
                    // Method might be ending
                    if (isMethodEnd(lines, i)) {
                        issues.addAll(checkMethodLength(currentMethod));
                        currentMethod = null;
                    }
                } else if (!line.isEmpty() && !line.isComment()) {
                    currentMethod.incrementLineCount();
                }
            }

            // Check for magic numbers
            if (line.getType() == LineType.ADDED) {
                issues.addAll(checkMagicNumbers(file, line));
            }
        }

        return issues;
    }

    private boolean isMethodDeclaration(String line) {
        // Simple heuristic for method declaration
        return line.matches(".*\\s+(public|protected|private)\\s+.*\\(.*\\).*\\{?.*");
    }

    private String extractMethodName(String line) {
        Pattern pattern = Pattern.compile("\\s(\\w+)\\s*\\(");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown";
    }

    private int extractParameterCount(String line) {
        int start = line.indexOf('(');
        int end = line.indexOf(')', start);
        if (start < 0 || end < 0) return 0;

        String params = line.substring(start + 1, end).trim();
        if (params.isEmpty()) return 0;

        return (int) Arrays.stream(params.split(","))
            .filter(s -> !s.trim().isEmpty())
            .count();
    }

    private boolean hasJavadoc(List<DiffLine> lines, int methodIndex) {
        // Check preceding lines for Javadoc
        for (int i = methodIndex - 1; i >= 0 && i >= methodIndex - 5; i--) {
            String line = lines.get(i).getContent();
            if (line.contains("/**")) {
                return true;
            }
            if (!line.trim().isEmpty() && !line.trim().startsWith("*")) {
                break;
            }
        }
        return false;
    }

    private List<Issue> checkMethodLength(MethodContext method) {
        List<Issue> issues = new ArrayList<>();

        if (method.getLineCount() > maxMethodLines) {
            issues.add(Issue.builder()
                .severity(Severity.MEDIUM)
                .type(IssueType.CODE_SMELL)
                .rule("LONG_METHOD")
                .filePath(method.getFile().getPath())
                .startLine(method.getStartLine())
                .message(String.format(
                    "Method '%s' is too long (%d lines, max %d)",
                    method.getName(),
                    method.getLineCount(),
                    maxMethodLines
                ))
                .suggestion("Extract smaller helper methods")
                .documentationUrls(List.of(
                    "https://refactoring.guru/extract-method"
                ))
                .build());
        }

        if (method.getParameterCount() > maxParameters) {
            issues.add(Issue.builder()
                .severity(Severity.MEDIUM)
                .type(IssueType.CODE_SMELL)
                .rule("LONG_PARAMETER_LIST")
                .filePath(method.getFile().getPath())
                .startLine(method.getStartLine())
                .message(String.format(
                    "Method '%s' has too many parameters (%d, max %d)",
                    method.getName(),
                    method.getParameterCount(),
                    maxParameters
                ))
                .suggestion("Consider using a parameter object")
                .documentationUrls(List.of(
                    "https://refactoring.guru/introduce-parameter-object"
                ))
                .build());
        }

        if (requireJavadoc && !method.hasJavadoc() && method.isPublic()) {
            issues.add(Issue.builder()
                .severity(Severity.LOW)
                .type(IssueType.DOCUMENTATION)
                .rule("MISSING_JAVADOC")
                .filePath(method.getFile().getPath())
                .startLine(method.getStartLine())
                .message(String.format(
                    "Public method '%s' is missing Javadoc",
                    method.getName()
                ))
                .suggestion("Add Javadoc comment describing the method")
                .build());
        }

        return issues;
    }

    private List<Issue> checkMagicNumbers(DiffFile file, DiffLine line) {
        List<Issue> issues = new ArrayList<>();
        String content = line.getContent();

        // Skip comments and strings
        if (line.isComment() || content.contains("\"")) {
            return issues;
        }

        // Find numeric literals (excluding 0, 1, -1, 2)
        Pattern pattern = Pattern.compile("\\b(?!-?([01]|2)\\b)(-?\\d+\\.?\\d*)\\b");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String number = matcher.group(1);
            int start = matcher.start(1);

            // Check if it's in a comment
            int commentIdx = content.indexOf("//");
            if (commentIdx >= 0 && start > commentIdx) {
                continue;
            }

            issues.add(Issue.builder()
                .severity(Severity.LOW)
                .type(IssueType.CODE_SMELL)
                .rule("MAGIC_NUMBER")
                .filePath(file.getPath())
                .startLine(line.getLineNumber())
                .message(String.format(
                    "Magic number '%s' found in code",
                    number
                ))
                .suggestion("Extract to named constant")
                .codeSnippet(content.trim())
                .build());
        }

        return issues;
    }

    private boolean isMethodEnd(List<DiffLine> lines, int index) {
        int braceCount = 0;
        for (int i = index; i >= 0; i--) {
            String line = lines.get(i).getContent();
            if (line.contains("{")) braceCount++;
            if (line.contains("}")) braceCount--;
        }
        return braceCount == 0;
    }

    /**
     * Context for tracking method analysis.
     */
    private static class MethodContext {
        private final DiffFile file;
        private final String name;
        private final Integer startLine;
        private final int parameterCount;
        private final boolean hasJavadoc;
        private int lineCount = 0;
        private boolean isPublic = false;

        MethodContext(DiffFile file, String name, Integer startLine,
                     int parameterCount, boolean hasJavadoc) {
            this.file = file;
            this.name = name;
            this.startLine = startLine;
            this.parameterCount = parameterCount;
            this.hasJavadoc = hasJavadoc;
        }

        DiffFile getFile() { return file; }
        String getName() { return name; }
        Integer getStartLine() { return startLine; }
        int getParameterCount() { return parameterCount; }
        boolean hasJavadoc() { return hasJavadoc; }
        int getLineCount() { return lineCount; }
        boolean isPublic() { return isPublic; }

        void incrementLineCount() { lineCount++; }
        void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    }
}
```

## Configuration

```yaml
# application.yml
pullwise:
  plugins:
    simple-linter:
      enabled: true
      maxMethodLines: 50
      maxParameters: 5
      requireJavadoc: true
```

## Plugin Metadata

```java
// META-INF/services/com.pullwise.api.application.service.plugin.api.CodeReviewPlugin
com.example.pullwise.plugin.SimpleLinterPlugin
```

## Building

```bash
# Compile plugin
javac -cp "pullwise-api.jar:/path/to/slf4j/api.jar" \
  -d target/ \
  src/main/java/com/example/pullwise/plugin/SimpleLinterPlugin.java

# Create JAR
jar cf simple-linter-plugin.jar \
  -C target/ . \
  -C src/main/resources/ .

# Place in plugins directory
cp simple-linter-plugin.jar /opt/pullwise/plugins/
```

## Expected Output

```
Analysis Result from Simple Linter
==================================

Issues Found: 4

1. LONG_METHOD (Medium)
   File: src/main/java/Service.java:42
   Message: Method 'processData' is too long (75 lines, max 50)
   Suggestion: Extract smaller helper methods
   Docs: https://refactoring.guru/extract-method

2. LONG_PARAMETER_LIST (Medium)
   File: src/main/java/Repository.java:18
   Message: Method 'query' has too many parameters (7, max 5)
   Suggestion: Consider using a parameter object
   Docs: https://refactoring.guru/introduce-parameter-object

3. MISSING_JAVADOC (Low)
   File: src/main/java/Controller.java:25
   Message: Public method 'handleRequest' is missing Javadoc
   Suggestion: Add Javadoc comment describing the method

4. MAGIC_NUMBER (Low)
   File: src/main/java/Util.java:33
   Message: Magic number '3600' found in code
   Suggestion: Extract to named constant
```

## Testing

```java
@Test
void shouldDetectLongMethod() {
    // Arrange
    SimpleLinterPlugin plugin = new SimpleLinterPlugin();
    plugin.initialize();

    String code = """
        public class Test {
            public void longMethod() {
        """ + "  // 50+ lines\n".repeat(51) + """
            }
        }
        """;

    DiffFile file = createMockFile("Test.java", code);

    // Act
    AnalysisResult result = plugin.analyze(createRequest(file));

    // Assert
    assertThat(result.getIssues()).hasSize(1);
    assertThat(result.getIssues().get(0).getRule()).isEqualTo("LONG_METHOD");
}
```

## Extension Ideas

### Add More Rules

```java
private List<Issue> checkNamingConventions(DiffFile file) {
    List<Issue> issues = new ArrayList<>();

    // Check class names (PascalCase)
    // Check method names (camelCase)
    // Check constant names (UPPER_SNAKE_CASE)

    return issues;
}

private List<Issue> checkDuplicateCode(DiffFile file) {
    List<Issue> issues = new ArrayList<>();

    // Detect similar code blocks
    // Suggest extraction

    return issues;
}
```

### Add Configuration

```java
@Override
protected void doInitialize() throws PluginException {
    // Load rules configuration
    enabledRules = new HashSet<>(getConfigList("enabledRules"));

    // Load thresholds
    maxCyclomaticComplexity = getConfigInt("maxCyclomaticComplexity", 10);
    maxNestingDepth = getConfigInt("maxNestingDepth", 4);
}
```

## Next Steps

- [Rust Tool](/docs/plugin-development/examples/rust-tool) - External tool wrapper
- [Config Plugin](/docs/plugin-development/examples/config-plugin) - Configuration examples
- [External Tool](/docs/plugin-development/examples/external-tool) - Executable wrapper
