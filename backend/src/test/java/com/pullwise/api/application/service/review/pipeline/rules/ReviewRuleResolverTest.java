package com.pullwise.api.application.service.review.pipeline.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewRuleResolverTest {

    private ReviewRuleResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ReviewRuleResolver(new ObjectMapper());
        resolver.load(); // @PostConstruct manualmente (sem contexto Spring)
    }

    @Test
    void resolvesJavaChecklistForJavaFile() {
        assertThat(resolver.resolve("src/main/java/com/Foo.java"))
                .contains("Java")
                .contains("NullPointerException");
    }

    @Test
    void resolvesTypeScriptChecklistForTsxFile() {
        assertThat(resolver.resolve("web/src/Button.tsx"))
                .contains("TypeScript");
    }

    @Test
    void resolvesPythonChecklistForPyFile() {
        assertThat(resolver.resolve("scripts/run.py"))
                .contains("Mutable default arguments");
    }

    @Test
    void fallsBackToDefaultForUnknownExtension() {
        assertThat(resolver.resolve("notes.xyz"))
                .contains("General review checklist");
    }

    @Test
    void nullPathReturnsDefault() {
        assertThat(resolver.resolve(null)).contains("General review checklist");
    }

    @Test
    void firstMatchWins_kotlinMapsToJavaChecklist() {
        assertThat(resolver.resolve("src/Main.kt")).contains("Java");
    }
}
