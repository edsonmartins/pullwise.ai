package com.pullwise.api.application.service.review.pipeline.synthesis;

import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.domain.enums.IssueSource;
import com.pullwise.api.domain.enums.IssueType;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.model.Issue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommentPositionerTest {

    private final CommentPositioner positioner = new CommentPositioner();

    /**
     * Patch de referência (new-side, começando em 1):
     * <pre>
     * 1: public class Foo {
     * 2:     int x = 2;          (added)
     * 3:     int y = compute();  (added)
     * 4:     return x;
     * 5: }
     * </pre>
     * Old-side: linha 2 = "int x = 1;" (removida).
     */
    private static final String PATCH = """
            @@ -1,4 +1,5 @@
             public class Foo {
            -    int x = 1;
            +    int x = 2;
            +    int y = compute();
                 return x;
             }""";

    @Test
    void resolvesAddedLineToNewFileLineNumber() {
        Issue issue = issue("Foo.java", "int y = compute();", 99);

        int n = positioner.reposition(List.of(issue), diffs("Foo.java", PATCH));

        assertThat(n).isEqualTo(1);
        assertThat(issue.getLineStart()).isEqualTo(3);
        assertThat(issue.getLineEnd()).isEqualTo(3);
    }

    @Test
    void resolvesMultiLineSnippetToStartAndEnd() {
        Issue issue = issue("Foo.java", "int x = 2;\nint y = compute();", 1);

        positioner.reposition(List.of(issue), diffs("Foo.java", PATCH));

        assertThat(issue.getLineStart()).isEqualTo(2);
        assertThat(issue.getLineEnd()).isEqualTo(3);
    }

    @Test
    void ignoresIndentationAndWhitespaceDrift() {
        // Snippet sem a indentação original
        Issue issue = issue("Foo.java", "int   x = 2;", 1);

        positioner.reposition(List.of(issue), diffs("Foo.java", PATCH));

        assertThat(issue.getLineStart()).isEqualTo(2);
    }

    @Test
    void fallsBackToOldSideForRemovedLine() {
        Issue issue = issue("Foo.java", "int x = 1;", 1);

        int n = positioner.reposition(List.of(issue), diffs("Foo.java", PATCH));

        assertThat(n).isEqualTo(1);
        assertThat(issue.getLineStart()).isEqualTo(2); // linha no arquivo antigo
    }

    @Test
    void leavesIssueUntouchedWhenNoSnippet() {
        Issue issue = issue("Foo.java", null, 99);

        int n = positioner.reposition(List.of(issue), diffs("Foo.java", PATCH));

        assertThat(n).isZero();
        assertThat(issue.getLineStart()).isEqualTo(99);
    }

    @Test
    void leavesIssueUntouchedWhenSnippetNotFound() {
        Issue issue = issue("Foo.java", "completely unrelated code", 42);

        int n = positioner.reposition(List.of(issue), diffs("Foo.java", PATCH));

        assertThat(n).isZero();
        assertThat(issue.getLineStart()).isEqualTo(42);
    }

    @Test
    void leavesIssueUntouchedWhenFileNotInDiff() {
        Issue issue = issue("Other.java", "int y = compute();", 7);

        int n = positioner.reposition(List.of(issue), diffs("Foo.java", PATCH));

        assertThat(n).isZero();
        assertThat(issue.getLineStart()).isEqualTo(7);
    }

    @Test
    void handlesNullAndEmptyInputs() {
        assertThat(positioner.reposition(null, diffs("Foo.java", PATCH))).isZero();
        assertThat(positioner.reposition(List.of(), diffs("Foo.java", PATCH))).isZero();
        assertThat(positioner.reposition(List.of(issue("Foo.java", "x", 1)), null)).isZero();
        assertThat(positioner.reposition(List.of(issue("Foo.java", "x", 1)), List.of())).isZero();
    }

    private Issue issue(String filePath, String snippet, int line) {
        return Issue.builder()
                .filePath(filePath)
                .codeSnippet(snippet)
                .lineStart(line)
                .lineEnd(line)
                .severity(Severity.MEDIUM)
                .type(IssueType.BUG)
                .title("t")
                .source(IssueSource.LLM)
                .build();
    }

    private List<GitHubService.FileDiff> diffs(String filename, String patch) {
        return List.of(new GitHubService.FileDiff(filename, "modified", 2, 1, patch));
    }
}
