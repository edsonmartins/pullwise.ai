package com.pullwise.api.application.service.review;

import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.domain.enums.ReviewStatus;
import com.pullwise.api.domain.model.*;
import com.pullwise.api.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Serviço orquestrador de reviews.
 * Coordena as análises SAST e LLM, consolida os resultados e posta no PR.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewOrchestrator {

    private final ReviewRepository reviewRepository;
    private final IssueRepository issueRepository;
    private final PullRequestRepository pullRequestRepository;
    private final SastAnalysisService sastAnalysisService;
    private final LLMReviewService llmReviewService;
    private final ConsolidationService consolidationService;
    private final PostingService postingService;
    private final GitHubService gitHubService;
    private final UsageRecordRepository usageRecordRepository;

    /**
     * Inicia um review de forma assíncrona.
     */
    @Async("reviewExecutor")
    @Transactional
    public void startReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));

        log.info("Starting review {} for PR {}", reviewId, review.getPullRequest().getPrNumber());

        try {
            review.start();
            reviewRepository.save(review);

            // 1. Buscar diffs do PR
            PullRequest pr = review.getPullRequest();
            List<GitHubService.FileDiff> diffs = gitHubService.fetchPullRequestDiffs(
                    pr.getProject(),
                    pr.getPrNumber()
            );

            // 2. Executar análise SAST (se habilitado)
            List<Issue> sastIssues = List.of();
            if (review.getSastEnabled()) {
                sastIssues = sastAnalysisService.analyze(review, pr, diffs);
            }

            // 3. Executar análise LLM (se habilitado)
            List<Issue> llmIssues = List.of();
            if (review.getLlmEnabled()) {
                llmIssues = llmReviewService.analyze(pr, diffs);
            }

            // 4. Consolidar resultados
            List<Issue> allIssues = consolidationService.consolidateIssues(sastIssues, llmIssues);

            // 5. Salvar issues
            for (Issue issue : allIssues) {
                issue.setReview(review);
                issueRepository.save(issue);
            }

            // 6. Atualizar métricas do review
            review.setFilesAnalyzed(diffs.size());
            review.setLinesAddedAnalyzed(diffs.stream().mapToInt(d -> d.additions()).sum());
            review.setLinesRemovedAnalyzed(diffs.stream().mapToInt(d -> d.deletions()).sum());

            // 7. Postar comentário no PR
            String commentId = postingService.postReviewComment(pr, allIssues);
            review.setReviewCommentId(commentId);

            // 8. Marcar como completado
            review.complete();
            reviewRepository.save(review);

            // 9. Registrar uso
            recordUsage(review);

            log.info("Review {} completed with {} issues", reviewId, allIssues.size());

        } catch (Exception e) {
            log.error("Error processing review {}", reviewId, e);
            review.fail(e.getMessage());
            reviewRepository.save(review);
        }
    }

    /**
     * Cria um novo review para um PR.
     */
    @Transactional
    public Review createReview(Long pullRequestId, boolean sastEnabled, boolean llmEnabled, boolean ragEnabled) {
        PullRequest pr = pullRequestRepository.findById(pullRequestId)
                .orElseThrow(() -> new IllegalArgumentException("PullRequest not found: " + pullRequestId));

        // Verificar se já existe review em andamento
        if (pr.hasActiveReview()) {
            throw new IllegalStateException("Review already in progress for this PR");
        }

        Review review = Review.builder()
                .pullRequest(pr)
                .status(ReviewStatus.PENDING)
                .sastEnabled(sastEnabled)
                .llmEnabled(llmEnabled)
                .ragEnabled(ragEnabled)
                .build();

        return reviewRepository.save(review);
    }

    /**
     * Registra o uso para billing.
     */
    private void recordUsage(Review review) {
        Organization org = review.getPullRequest().getProject().getOrganization();
        String period = java.time.LocalDate.now().toString().substring(0, 7); // YYYY-MM

        UsageRecord record = UsageRecord.of(
                org,
                review.getPullRequest().getProject(),
                java.time.LocalDate.now(),
                UsageRecord.METRIC_REVIEWS,
                1L
        );
        usageRecordRepository.save(record);

        // Registrar tokens LLM se aplicável
        if (review.getLlmEnabled()) {
            UsageRecord tokensRecord = UsageRecord.of(
                    org,
                    review.getPullRequest().getProject(),
                    java.time.LocalDate.now(),
                    UsageRecord.METRIC_LLM_TOKENS,
                    llmReviewService.getEstimatedTokens(review)
            );
            usageRecordRepository.save(tokensRecord);
        }
    }

    /**
     * Busca reviews por status.
     */
    public List<Review> findReviewsByStatus(ReviewStatus status) {
        return reviewRepository.findByStatusOrderByCreatedAtAsc(status);
    }

    /**
     * Busca reviews stalled (em progresso há muito tempo).
     */
    public List<Review> findStalledReviews() {
        java.time.LocalDateTime timeout = java.time.LocalDateTime.now().minusMinutes(30);
        return reviewRepository.findStalledReviews(
                List.of(ReviewStatus.IN_PROGRESS, ReviewStatus.PENDING),
                timeout
        );
    }

    /**
     * Cancela um review.
     */
    @Transactional
    public void cancelReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));

        if (review.getStatus().isTerminal()) {
            throw new IllegalStateException("Cannot cancel a review in status: " + review.getStatus());
        }

        review.setStatus(ReviewStatus.CANCELLED);
        reviewRepository.save(review);

        log.info("Review {} cancelled", reviewId);
    }

    /**
     * Obtém estatísticas de um review.
     */
    public Optional<ReviewStats> getReviewStats(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .map(review -> {
                    List<Issue> issues = issueRepository.findByReviewId(reviewId);
                    return new ReviewStats(
                            review.getId(),
                            review.getStatus(),
                            issues.size(),
                            (int) issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.CRITICAL).count(),
                            (int) issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.HIGH).count(),
                            (int) issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.MEDIUM).count(),
                            (int) issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.LOW).count(),
                            review.getDurationMs()
                    );
                });
    }

    public record ReviewStats(
            Long reviewId,
            ReviewStatus status,
            int totalIssues,
            int criticalCount,
            int highCount,
            int mediumCount,
            int lowCount,
            Long durationMs
    ) {}
}
