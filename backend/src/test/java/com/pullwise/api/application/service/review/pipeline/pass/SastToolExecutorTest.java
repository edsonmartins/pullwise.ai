package com.pullwise.api.application.service.review.pipeline.pass;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.integration.SonarQubeService;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.model.Review;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SastToolExecutorTest {

    private SastToolExecutor executor;

    @Mock
    private SonarQubeService sonarQubeService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        executor = new SastToolExecutor(sonarQubeService, objectMapper);
        ReflectionTestUtils.setField(executor, "timeoutSeconds", 30);
    }

    @Test
    void execute_unimplementedTool_shouldReturnEmpty() {
        PullRequest pr = new PullRequest();
        pr.setId(1L);
        Review review = new Review();

        List<SastAggregatorPass.ToolIssue> result = executor.execute(
                SastAggregatorPass.SastTool.ERROR_PRONE, pr, review, List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void execute_sonarqube_notConfigured_shouldReturnEmpty() {
        when(sonarQubeService.isConfigured()).thenReturn(false);

        PullRequest pr = new PullRequest();
        pr.setId(1L);
        Review review = new Review();

        List<SastAggregatorPass.ToolIssue> result = executor.execute(
                SastAggregatorPass.SastTool.SONARQUBE, pr, review, List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void execute_checkstyle_noJavaFiles_shouldReturnEmpty() {
        PullRequest pr = new PullRequest();
        pr.setId(1L);
        Review review = new Review();

        // Only Python files — no Java
        List<GitHubService.FileDiff> diffs = List.of(
                new GitHubService.FileDiff("app.py", "modified", 10, 2, "+print('hello')")
        );

        List<SastAggregatorPass.ToolIssue> result = executor.execute(
                SastAggregatorPass.SastTool.CHECKSTYLE, pr, review, diffs);

        assertThat(result).isEmpty();
    }

    @Test
    void execute_eslint_noJsFiles_shouldReturnEmpty() {
        PullRequest pr = new PullRequest();
        pr.setId(1L);
        Review review = new Review();

        List<GitHubService.FileDiff> diffs = List.of(
                new GitHubService.FileDiff("Main.java", "modified", 5, 1, "+int x = 1;")
        );

        List<SastAggregatorPass.ToolIssue> result = executor.execute(
                SastAggregatorPass.SastTool.ESLINT, pr, review, diffs);

        assertThat(result).isEmpty();
    }

    @Test
    void execute_ruff_noPythonFiles_shouldReturnEmpty() {
        PullRequest pr = new PullRequest();
        pr.setId(1L);
        Review review = new Review();

        List<GitHubService.FileDiff> diffs = List.of(
                new GitHubService.FileDiff("index.ts", "modified", 5, 1, "+const x = 1;")
        );

        List<SastAggregatorPass.ToolIssue> result = executor.execute(
                SastAggregatorPass.SastTool.RUFF, pr, review, diffs);

        assertThat(result).isEmpty();
    }

    @Test
    void execute_removedFiles_shouldBeFiltered() {
        PullRequest pr = new PullRequest();
        pr.setId(1L);
        Review review = new Review();

        // Removed file — should be skipped
        List<GitHubService.FileDiff> diffs = List.of(
                new GitHubService.FileDiff("Deleted.java", "removed", 0, 50, "")
        );

        List<SastAggregatorPass.ToolIssue> result = executor.execute(
                SastAggregatorPass.SastTool.CHECKSTYLE, pr, review, diffs);

        assertThat(result).isEmpty();
    }
}
