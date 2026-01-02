# Creating Your First Plugin

Create a simple Pullwise plugin from scratch.

## What We'll Build

We'll create a plugin that detects:
- TODO comments in code
- Console.log statements
- Hardcoded passwords

## Prerequisites

- Java 17+
- Maven 3.9+
- IDE (IntelliJ IDEA recommended)

## Step 1: Create Plugin Project

```bash
# Create project directory
mkdir pullwise-todo-plugin
cd pullwise-todo-plugin

# Create Maven structure
mkdir -p src/main/java/com/example/pullwise
mkdir -p src/main/resources/META-INF/services
```

## Step 2: Configure Maven

Create `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>pullwise-todo-plugin</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>Pullwise TODO Plugin</name>
    <description>Detects TODO comments and console.log statements</description>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Pullwise Plugin API -->
        <dependency>
            <groupId>com.pullwise</groupId>
            <artifactId>pullwise-plugin-api</artifactId>
            <version>1.0.0</version>
            <scope>provided</scope>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.9</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

## Step 3: Implement Plugin

Create `TodoPlugin.java`:

```java
package com.example.pullwise;

import com.pullwise.api.application.service.plugin.api.*;
import com.pullwise.api.application.service.plugin.api.AbstractCodeReviewPlugin;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Simple plugin that detects code quality issues.
 */
public class TodoPlugin extends AbstractCodeReviewPlugin {

    // Patterns to detect
    private static final Pattern TODO_PATTERN = Pattern.compile("//\\s*TODO:?.*");
    private static final Pattern CONSOLE_LOG_PATTERN = Pattern.compile("console\\.log\\(");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("password\\s*=\\s*[\"'][^\"']+[\"']");

    private boolean enableTodoCheck;
    private boolean enableConsoleCheck;
    private boolean enablePasswordCheck;

    @Override
    protected void doInitialize() throws PluginException {
        // Read configuration
        this.enableTodoCheck = getConfigBoolean("enableTodoCheck", true);
        this.enableConsoleCheck = getConfigBoolean("enableConsoleCheck", true);
        this.enablePasswordCheck = getConfigBoolean("enablePasswordCheck", true);

        log.info("TodoPlugin initialized with TODO={}, console={}, password={}",
            enableTodoCheck, enableConsoleCheck, enablePasswordCheck);
    }

    @Override
    protected AnalysisResult doAnalyze(AnalysisRequest request) throws PluginException {
        List<Issue> issues = new ArrayList<>();

        // Analyze each file in the diff
        for (DiffFile file : request.getDiff().getFiles()) {
            if (file.getRemovedLines() != null) {
                // Only check added/modified lines
                issues.addAll(analyzeFile(file));
            }
        }

        return AnalysisResult.builder()
            .pluginId(getId())
            .issues(issues)
            .success(true)
            .build();
    }

    private List<Issue> analyzeFile(DiffFile file) {
        List<Issue> issues = new ArrayList<>();
        String filePath = file.getPath();
        String language = file.getLanguage();

        // Check each line
        for (DiffLine line : file.getAddedLines()) {
            String content = line.getContent();
            int lineNumber = line.getLineNumber();

            // Check for TODO comments
            if (enableTodoCheck && TODO_PATTERN.matcher(content).find()) {
                issues.add(Issue.builder()
                    .severity(Severity.LOW)
                    .type(IssueType.CODE_SMELL)
                    .rule("TODO_COMMENT")
                    .filePath(filePath)
                    .startLine(lineNumber)
                    .endLine(lineNumber)
                    .message("TODO comment found. Please create an issue instead.")
                    .suggestion("Replace with: // FIXME: Link to issue #123")
                    .build());
            }

            // Check for console.log (JavaScript/TypeScript only)
            if (enableConsoleCheck &&
                ("JAVASCRIPT".equals(language) || "TYPESCRIPT".equals(language))) {
                Matcher matcher = CONSOLE_LOG_PATTERN.matcher(content);
                if (matcher.find()) {
                    issues.add(Issue.builder()
                        .severity(Severity.LOW)
                        .type(IssueType.CODE_SMELL)
                        .rule("NO_CONSOLE_LOG")
                        .filePath(filePath)
                        .startLine(lineNumber)
                        .endLine(lineNumber)
                        .message("console.log statement found. Remove before deployment.")
                        .suggestion("Use a proper logging library instead.")
                        .build());
                }
            }

            // Check for hardcoded passwords
            if (enablePasswordCheck) {
                Matcher matcher = PASSWORD_PATTERN.matcher(content);
                if (matcher.find()) {
                    issues.add(Issue.builder()
                        .severity(Severity.CRITICAL)
                        .type(IssueType.VULNERABILITY)
                        .rule("HARDCODED_PASSWORD")
                        .filePath(filePath)
                        .startLine(lineNumber)
                        .endLine(lineNumber)
                        .message("Hardcoded password detected. This is a security vulnerability.")
                        .suggestion("Use environment variables or a secure vault.")
                        .build());
                }
            }
        }

        return issues;
    }

    // Required metadata methods

    @Override
    public String getId() {
        return "com.example.pullwise.todo-plugin";
    }

    @Override
    public String getName() {
        return "TODO Detector";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getAuthor() {
        return "Your Name";
    }

    @Override
    public String getDescription() {
        return "Detects TODO comments, console.log statements, and hardcoded passwords";
    }

    @Override
    public PluginType getType() {
        return PluginType.LINTER;
    }

    @Override
    public Set<PluginLanguage> getSupportedLanguages() {
        return Set.of(
            PluginLanguage.JAVA,
            PluginLanguage.JAVASCRIPT,
            PluginLanguage.TYPESCRIPT,
            PluginLanguage.PYTHON
        );
    }
}
```

## Step 4: Register Plugin

Create `META-INF/services/com.pullwise.api.application.service.plugin.api.CodeReviewPlugin`:

```
com.example.pullwise.TodoPlugin
```

## Step 5: Build Plugin

```bash
# Build the plugin
mvn clean package

# Output: target/pullwise-todo-plugin-1.0.0.jar
```

## Step 6: Install Plugin

### Option 1: Copy to Plugins Directory

```bash
# Create plugins directory
sudo mkdir -p /opt/pullwise/plugins

# Copy plugin
cp target/pullwise-todo-plugin-1.0.0.jar /opt/pullwise/plugins/

# Restart Pullwise
sudo systemctl restart pullwise-backend
```

### Option 2: Add to Classpath

```bash
# Add to backend lib directory
cp target/pullwise-todo-plugin-1.0.0.jar backend/lib/

# Restart backend
docker-compose restart backend
```

## Step 7: Configure Plugin

Add to `application.yml`:

```yaml
pullwise:
  plugins:
    com.example.pullwise.todo-plugin:
      enabled: true
      config:
        enableTodoCheck: true
        enableConsoleCheck: true
        enablePasswordCheck: true
```

## Step 8: Test Plugin

Create a test file with issues:

```javascript
// TODO: fix this later
function authenticate() {
    var password = "secret123";  // Critical!
    console.log("Authenticating...");  // Low
    return true;
}
```

Trigger a review and verify:
- ✅ TODO comment detected
- ✅ Console.log detected
- ✅ Hardcoded password detected

## Step 9: Verify Results

Check the Pullwise dashboard:

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/reviews/{review-id}/issues
```

Expected response:
```json
{
  "content": [
    {
      "rule": "TODO_COMMENT",
      "severity": "LOW",
      "message": "TODO comment found. Please create an issue instead."
    },
    {
      "rule": "NO_CONSOLE_LOG",
      "severity": "LOW",
      "message": "console.log statement found. Remove before deployment."
    },
    {
      "rule": "HARDCODED_PASSWORD",
      "severity": "CRITICAL",
      "message": "Hardcoded password detected. This is a security vulnerability."
    }
  ]
}
```

## Next Steps

- [Plugin Types](/docs/plugin-development/plugin-types/) - Explore different plugin types
- [API Reference](/docs/plugin-development/api-reference/) - Full API documentation
- [Examples](/docs/plugin-development/examples/) - More example plugins
- [Packaging](/docs/plugin-development/packaging/) - Package and distribute your plugin
