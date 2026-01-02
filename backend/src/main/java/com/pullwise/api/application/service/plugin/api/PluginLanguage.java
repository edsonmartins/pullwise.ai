package com.pullwise.api.application.service.plugin.api;

import lombok.Getter;

import java.util.Set;

/**
 * Linguagens suportadas pelos plugins.
 */
@Getter
public enum PluginLanguage {
    JAVA("java", ".java"),
    JAVASCRIPT("javascript", ".js", ".jsx", ".mjs"),
    TYPESCRIPT("typescript", ".ts", ".tsx"),
    PYTHON("python", ".py"),
    GO("go", ".go"),
    RUBY("ruby", ".rb"),
    PHP("php", ".php"),
    CSHARP("csharp", ".cs"),
    KOTLIN("kotlin", ".kt", ".kts"),
    RUST("rust", ".rs"),
    ALL("all", "*");

    private final String code;
    private final Set<String> extensions;

    PluginLanguage(String code, String... extensions) {
        this.code = code;
        this.extensions = Set.of(extensions);
    }

    public static PluginLanguage fromExtension(String extension) {
        for (PluginLanguage lang : values()) {
            if (lang.getExtensions().contains(extension)) {
                return lang;
            }
        }
        return null;
    }

    public boolean supportsExtension(String extension) {
        return this == ALL || extensions.contains(extension);
    }
}
