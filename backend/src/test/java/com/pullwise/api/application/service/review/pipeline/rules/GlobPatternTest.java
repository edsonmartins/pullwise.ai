package com.pullwise.api.application.service.review.pipeline.rules;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobPatternTest {

    private boolean matches(String glob, String path) {
        return GlobPattern.compile(glob).matcher(path).matches();
    }

    @Test
    void doubleStarMatchesRootAndNestedFiles() {
        assertThat(matches("**/*.java", "Foo.java")).isTrue();
        assertThat(matches("**/*.java", "src/main/java/com/Foo.java")).isTrue();
        assertThat(matches("**/*.java", "Foo.kt")).isFalse();
    }

    @Test
    void braceExpansionMatchesAnyAlternative() {
        String glob = "**/*.{ts,tsx,js,jsx}";
        assertThat(matches(glob, "a.ts")).isTrue();
        assertThat(matches(glob, "components/Button.tsx")).isTrue();
        assertThat(matches(glob, "lib/util.js")).isTrue();
        assertThat(matches(glob, "styles.css")).isFalse();
    }

    @Test
    void singleStarDoesNotCrossDirectories() {
        assertThat(matches("*.java", "Foo.java")).isTrue();
        assertThat(matches("*.java", "src/Foo.java")).isFalse();
    }

    @Test
    void matchingIsCaseInsensitive() {
        assertThat(matches("**/*.java", "FOO.JAVA")).isTrue();
    }

    @Test
    void pathSegmentGlobs() {
        assertThat(matches("**/test/**", "src/test/com/FooTest.java")).isTrue();
        assertThat(matches("**/test/**", "src/main/Foo.java")).isFalse();
    }

    @Test
    void dotIsLiteralNotWildcard() {
        assertThat(matches("**/*.py", "scriptxpy")).isFalse();
        assertThat(matches("**/*.py", "script.py")).isTrue();
    }
}
