package com.pullwise.api.application.service;

import com.pullwise.api.application.service.integration.BitBucketService;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.integration.GitLabService;
import com.pullwise.api.application.service.llm.router.MultiModelLLMRouter;
import com.pullwise.api.application.service.review.pipeline.MultiPassReviewOrchestrator;
import com.pullwise.api.application.service.review.pipeline.MultiPassReviewOrchestrator.PassResult;
import com.pullwise.api.application.service.review.pipeline.MultiPassReviewOrchestrator.ReviewResult;
import com.pullwise.api.application.service.review.pipeline.pass.*;
import com.pullwise.api.application.service.review.pipeline.synthesis.IssueDuplicationDetector;
import com.pullwise.api.application.service.review.pipeline.synthesis.ResultSynthesizer;
import com.pullwise.api.domain.model.*;
import com.pullwise.api.domain.enums.*;
import com.pullwise.api.domain.repository.IssueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes para MultiPassReviewOrchestrator.
 */
@ExtendWith(MockitoExtension.class)
class MultiPassReviewOrchestratorTest {

    private MultiPassReviewOrchestrator orchestrator;

    @Mock private SastAggregatorPass sastAggregatorPass;
    @Mock private LlmPrimaryPass llmPrimaryPass;
    @Mock private SecurityFocusedPass securityFocusedPass;
    @Mock private CodeGraphImpactPass codeGraphImpactPass;
    @Mock private ResultSynthesizer resultSynthesizer;
    @Mock private IssueDuplicationDetector duplicationDetector;
    @Mock private MultiModelLLMRouter llmRouter;
    @Mock private IssueRepository issueRepository;
    @Mock private GitHubService gitHubService;
    @Mock private BitBucketService bitBucketService;
    @Mock private GitLabService gitLabService;

    @BeforeEach
    void setUp() {
        orchestrator = new MultiPassReviewOrchestrator(
                sastAggregatorPass, llmPrimaryPass, securityFocusedPass,
                codeGraphImpactPass, resultSynthesizer, duplicationDetector,
                llmRouter, issueRepository, gitHubService,
                bitBucketService, gitLabService
        );
    }

    @Test
    void executePipeline_shouldRunAll4Passes() {
        // Arrange
        Organization org = new Organization();
        org.setId(1L);

        Project project = new Project();
        project.setId(1L);
        project.setName("test-repo");
        project.setOrganization(org);

        PullRequest pr = new PullRequest();
        pr.setId(1L);
        pr.setPrNumber(42);
        pr.setProject(project);
        pr.setPlatform(Platform.GITHUB);

        Review review = new Review();
        review.setId(1L);
        review.setStatus(ReviewStatus.IN_PROGRESS);
        review.setPullRequest(pr);

        List<GitHubService.FileDiff> diffs = List.of(
                new GitHubService.FileDiff("src/Main.java", "modified", 10, 2, "+int x = 1;")
        );

        // Mock external calls
        when(gitHubService.fetchPullRequestDiffs(any(), eq(42))).thenReturn(diffs);

        PassResult emptyResult = new PassResult();
        emptyResult.setPassName("test");
        emptyResult.setSuccess(true);

        when(sastAggregatorPass.execute(any(), any(), any())).thenReturn(emptyResult);
        when(llmPrimaryPass.execute(any(), any(), any(), any())).thenReturn(emptyResult);
        when(securityFocusedPass.execute(any(), any(), any(), any(), any())).thenReturn(emptyResult);
        when(codeGraphImpactPass.execute(any(), any(), any(), any(), any())).thenReturn(emptyResult);
        when(duplicationDetector.deduplicate(any())).thenReturn(List.of());
        when(resultSynthesizer.generateSummary(any(), any(), any())).thenReturn("Summary");
        when(issueRepository.saveAll(any())).thenReturn(List.of());

        // Act
        ReviewResult result = orchestrator.executePipeline(pr, review);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSastResult()).isNotNull();
        assertThat(result.getLlmResult()).isNotNull();
        assertThat(result.getSecurityResult()).isNotNull();
        assertThat(result.getImpactResult()).isNotNull();

        verify(gitHubService).fetchPullRequestDiffs(project, 42);
        verify(sastAggregatorPass).execute(eq(pr), eq(review), eq(diffs));
    }

    @Test
    void executePipeline_whenSastFails_shouldContinueWithOtherPasses() {
        Project project = new Project();
        project.setName("test-repo");

        PullRequest pr = new PullRequest();
        pr.setId(1L);
        pr.setPrNumber(1);
        pr.setProject(project);

        Review review = new Review();
        review.setId(1L);
        review.setStatus(ReviewStatus.IN_PROGRESS);
        review.setPullRequest(pr);

        when(gitHubService.fetchPullRequestDiffs(any(), anyInt())).thenReturn(List.of());

        // SAST fails
        when(sastAggregatorPass.execute(any(), any(), any()))
                .thenThrow(new RuntimeException("SAST tool not found"));

        PassResult emptyResult = new PassResult();
        emptyResult.setSuccess(true);

        when(llmPrimaryPass.execute(any(), any(), any(), any())).thenReturn(emptyResult);
        when(securityFocusedPass.execute(any(), any(), any(), any(), any())).thenReturn(emptyResult);
        when(codeGraphImpactPass.execute(any(), any(), any(), any(), any())).thenReturn(emptyResult);
        when(duplicationDetector.deduplicate(any())).thenReturn(List.of());
        when(resultSynthesizer.generateSummary(any(), any(), any())).thenReturn("Summary");
        when(issueRepository.saveAll(any())).thenReturn(List.of());

        ReviewResult result = orchestrator.executePipeline(pr, review);

        // Pipeline should still succeed (degraded)
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSastResult().isSuccess()).isFalse(); // SAST failed
        assertThat(result.getLlmResult().isSuccess()).isTrue(); // LLM continued
    }

    @Test
    void reviewResult_collectAllIssues_shouldAggregateFromAllPasses() {
        ReviewResult result = new ReviewResult();

        PassResult sast = new PassResult();
        Issue i1 = Issue.builder().title("Issue 1").severity(Severity.HIGH).build();
        sast.setIssues(List.of(i1));

        PassResult llm = new PassResult();
        Issue i2 = Issue.builder().title("Issue 2").severity(Severity.LOW).build();
        llm.setIssues(List.of(i2));

        result.setSastResult(sast);
        result.setLlmResult(llm);
        result.setSecurityResult(new PassResult());
        result.setImpactResult(new PassResult());

        List<Issue> allIssues = result.collectAllIssues();
        assertThat(allIssues).hasSize(2);
    }
}
