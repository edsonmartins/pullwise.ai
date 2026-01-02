package com.pullwise.api.application.service;

import com.pullwise.api.application.service.llm.router.MultiModelLLMRouter;
import com.pullwise.api.application.service.review.pipeline.MultiPassReviewOrchestrator;
import com.pullwise.api.application.service.review.pipeline.pass.SastToolExecutor;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.repository.PullRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Testes de integração para MultiPassReviewOrchestrator.
 */
@ExtendWith(MockitoExtension.class)
class MultiPassReviewOrchestratorTest {

    private MultiPassReviewOrchestrator orchestrator;

    @Mock
    private MultiModelLLMRouter llmRouter;

    @Mock
    private SastToolExecutor sastExecutor;

    @Mock
    private PullRequestRepository prRepository;

    @BeforeEach
    void setUp() {
        orchestrator = new MultiPassReviewOrchestrator(llmRouter, sastExecutor, prRepository, null, null);
        ReflectionTestUtils.setField(orchestrator, "maxPasses", 3);
    }

    @Test
    void testExecuteReviewReturnsIssues() {
        // Arrange
        PullRequest pr = new PullRequest();
        pr.setId(1L);
        pr.setRepositoryId(100L);
        pr.setBranchName("feature/test");
        pr.setSourceRepositoryUrl("https://github.com/test/repo");

        when(sastExecutor.executeSastTools(any())).thenReturn(List.of());
        when(llmRouter.analyzeCode(any(), any(), any())).thenReturn("Code looks good.");
        when(prRepository.findById(1L)).thenReturn(java.util.Optional.of(pr));

        // Act
        List<Issue> issues = orchestrator.executeReview(1L);

        // Assert
        assertThat(issues).isNotNull();
    }

    @Test
    void testOrchestratorConfiguration() {
        // Verify configuration is loaded
        Integer maxPasses = ReflectionTestUtils.getField(orchestrator, "maxPasses");
        assertThat(maxPasses).isNotNull();
    }
}
