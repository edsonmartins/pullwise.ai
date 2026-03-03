package com.pullwise.api.application.service.review;

import com.pullwise.api.application.service.billing.RateLimitingService;
import com.pullwise.api.application.service.billing.RateLimitingService.RateLimitExceededException;
import com.pullwise.api.application.service.integration.BitBucketService;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.integration.GitLabService;
import com.pullwise.api.application.service.notification.NotificationService;
import com.pullwise.api.domain.enums.PlanType;
import com.pullwise.api.domain.enums.Platform;
import com.pullwise.api.domain.enums.ReviewStatus;
import com.pullwise.api.domain.model.*;
import com.pullwise.api.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewOrchestratorTest {

    private ReviewOrchestrator orchestrator;

    @Mock private ReviewRepository reviewRepository;
    @Mock private IssueRepository issueRepository;
    @Mock private PullRequestRepository pullRequestRepository;
    @Mock private SastAnalysisService sastAnalysisService;
    @Mock private LLMReviewService llmReviewService;
    @Mock private ConsolidationService consolidationService;
    @Mock private PostingService postingService;
    @Mock private GitHubService gitHubService;
    @Mock private BitBucketService bitBucketService;
    @Mock private GitLabService gitLabService;
    @Mock private UsageRecordRepository usageRecordRepository;
    @Mock private RateLimitingService rateLimitingService;
    @Mock private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        orchestrator = new ReviewOrchestrator(
                reviewRepository, issueRepository, pullRequestRepository,
                sastAnalysisService, llmReviewService, consolidationService,
                postingService, gitHubService, bitBucketService,
                gitLabService, usageRecordRepository, rateLimitingService,
                notificationService
        );
    }

    @Test
    void createReview_shouldCheckRateLimit() {
        Organization org = new Organization();
        org.setId(1L);
        org.setPlanType(PlanType.FREE);

        Project project = new Project();
        project.setId(1L);
        project.setOrganization(org);

        PullRequest pr = new PullRequest();
        pr.setId(1L);
        pr.setProject(project);
        pr.setPlatform(Platform.GITHUB);

        when(pullRequestRepository.findById(1L)).thenReturn(Optional.of(pr));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        Review review = orchestrator.createReview(1L, true, true, false);

        assertThat(review).isNotNull();
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.PENDING);
        verify(rateLimitingService).checkReviewLimit(1L);
    }

    @Test
    void createReview_rateLimitExceeded_shouldThrow() {
        Organization org = new Organization();
        org.setId(1L);
        org.setPlanType(PlanType.FREE);

        Project project = new Project();
        project.setId(1L);
        project.setOrganization(org);

        PullRequest pr = new PullRequest();
        pr.setId(1L);
        pr.setProject(project);

        when(pullRequestRepository.findById(1L)).thenReturn(Optional.of(pr));
        doThrow(new RateLimitExceededException("Limit exceeded", PlanType.FREE, 50, 50))
                .when(rateLimitingService).checkReviewLimit(1L);

        assertThatThrownBy(() -> orchestrator.createReview(1L, true, true, false))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void createReview_activeReviewExists_shouldThrowIllegalState() {
        PullRequest pr = mock(PullRequest.class);
        when(pr.hasActiveReview()).thenReturn(true);
        when(pullRequestRepository.findById(1L)).thenReturn(Optional.of(pr));

        assertThatThrownBy(() -> orchestrator.createReview(1L, true, true, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Review already in progress");
    }

    @Test
    void createReview_pullRequestNotFound_shouldThrow() {
        when(pullRequestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orchestrator.createReview(999L, true, true, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelReview_shouldSetStatusToCancelled() {
        Review review = new Review();
        review.setId(1L);
        review.setStatus(ReviewStatus.PENDING);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        orchestrator.cancelReview(1L);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.CANCELLED);
        verify(reviewRepository).save(review);
    }

    @Test
    void getReviewStats_shouldReturnStats() {
        Review review = new Review();
        review.setId(1L);
        review.setStatus(ReviewStatus.COMPLETED);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(issueRepository.findByReviewId(1L)).thenReturn(List.of());

        Optional<ReviewOrchestrator.ReviewStats> stats = orchestrator.getReviewStats(1L);

        assertThat(stats).isPresent();
        assertThat(stats.get().totalIssues()).isEqualTo(0);
        assertThat(stats.get().status()).isEqualTo(ReviewStatus.COMPLETED);
    }
}
