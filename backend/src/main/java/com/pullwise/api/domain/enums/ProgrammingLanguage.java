package com.pullwise.api.domain.enums;

import lombok.Getter;

import java.util.List;

/**
 * Linguagens de programação suportadas pela análise.
 */
@Getter
public enum ProgrammingLanguage {
    JAVA("java", List.of(".java")),
    JAVASCRIPT("javascript", List.of(".js", ".jsx")),
    TYPESCRIPT("typescript", List.of(".ts", ".tsx")),
    PYTHON("python", List.of(".py")),
    GO("go", List.of(".go")),
    RUBY("ruby", List.of(".rb")),
    PHP("php", List.of(".php")),
    CSHARP("csharp", List.of(".cs")),
    KOTLIN("kotlin", List.of(".kt", ".kts")),
    RUST("rust", List.of(".rs")),
    SWIFT("swift", List.of(".swift")),
    HTML("html", List.of(".html", ".htm")),
    CSS("css", List.of(".css", ".scss", ".sass")),
    JSON("json", List.of(".json")),
    YAML("yaml", List.of(".yaml", ".yml")),
    XML("xml", List.of(".xml")),
    MARKDOWN("markdown", List.of(".md", ".markdown"));

    private final String displayName;
    private final List<String> extensions;

    ProgrammingLanguage(String displayName, List<String> extensions) {
        this.displayName = displayName;
        this.extensions = extensions;
    }
}
