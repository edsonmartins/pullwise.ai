package com.pullwise.api.application.service.review.pipeline.synthesis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.llm.router.MultiModelLLMRouter;
import com.pullwise.api.application.service.llm.router.MultiModelLLMRouter.LLMResponse;
import com.pullwise.api.domain.enums.IssueSource;
import com.pullwise.api.domain.enums.IssueType;
import com.pullwise.api.domain.enums.ReviewTaskType;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.model.Issue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewReflectionFilterTest {

    @Mock private MultiModelLLMRouter llmRouter;

    private ReviewReflectionFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ReviewReflectionFilter(llmRouter, new ObjectMapper());
    }

    @Test
    void removesCommentThatDiffProvesWrong() {
        Issue good = llmIssue("Foo.java", "Real bug");
        Issue bad = llmIssue("Foo.java", "Bogus claim");

        // O LLM identifica o id=1 (bad) como provadamente errado.
        when(llmRouter.execute(eq(ReviewTaskType.QA), anyString(), anyString()))
                .thenReturn(response("[1]"));

        List<Issue> result = filter.filter(List.of(good, bad), diffs("Foo.java"));

        assertThat(result).containsExactly(good);
        assertThat(bad.getIsFalsePositive()).isTrue();
        assertThat(good.getIsFalsePositive()).isFalse();
    }

    @Test
    void keepsAllWhenLlmReturnsEmptyArray() {
        Issue a = llmIssue("Foo.java", "Issue A");
        Issue b = llmIssue("Foo.java", "Issue B");

        when(llmRouter.execute(any(), anyString(), anyString()))
                .thenReturn(response("[]"));

        List<Issue> result = filter.filter(List.of(a, b), diffs("Foo.java"));

        assertThat(result).containsExactly(a, b);
        assertThat(a.getIsFalsePositive()).isFalse();
    }

    @Test
    void neverFiltersSastIssues() {
        Issue sast = Issue.builder()
                .filePath("Foo.java").title("Checkstyle").description("d")
                .severity(Severity.LOW).type(IssueType.STYLE).source(IssueSource.CHECKSTYLE)
                .build();

        // Nenhum issue de LLM → não deve nem chamar o LLM, retorna tudo intacto.
        List<Issue> result = filter.filter(List.of(sast), diffs("Foo.java"));

        assertThat(result).containsExactly(sast);
    }

    @Test
    void isResilientWhenLlmThrows() {
        Issue a = llmIssue("Foo.java", "Issue A");

        when(llmRouter.execute(any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("LLM down"));

        List<Issue> result = filter.filter(List.of(a), diffs("Foo.java"));

        // Falha graciosa: mantém o achado.
        assertThat(result).containsExactly(a);
    }

    @Test
    void returnsInputWhenNoDiffs() {
        Issue a = llmIssue("Foo.java", "Issue A");
        assertThat(filter.filter(List.of(a), List.of())).containsExactly(a);
    }

    private Issue llmIssue(String filePath, String title) {
        return Issue.builder()
                .filePath(filePath).title(title).description("desc of " + title)
                .severity(Severity.MEDIUM).type(IssueType.BUG).source(IssueSource.LLM)
                .build();
    }

    private List<GitHubService.FileDiff> diffs(String filename) {
        return List.of(new GitHubService.FileDiff(filename, "modified", 1, 0, "@@ -1 +1 @@\n+code"));
    }

    private LLMResponse response(String content) {
        return new LLMResponse(content, "test-model", null, 1L, BigDecimal.ZERO);
    }
}
