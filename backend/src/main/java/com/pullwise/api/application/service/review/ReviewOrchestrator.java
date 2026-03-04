package com.pullwise.api.application.service.review;

import com.pullwise.api.application.service.attestation.AttestationService;
import com.pullwise.api.application.service.billing.RateLimitingService;
import com.pullwise.api.application.service.config.ConfigurationResolver;
import com.pullwise.api.domain.constants.ConfigKeys;
import com.pullwise.api.application.service.integration.AzureDevOpsService;
import com.pullwise.api.application.service.integration.BitBucketService;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.integration.GitLabService;
import com.pullwise.api.application.service.notification.NotificationService;
import com.pullwise.api.domain.enums.Platform;
import com.pullwise.api.domain.enums.ReviewStatus;
import com.pullwise.api.domain.enums.Severity;
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
    private final BitBucketService bitBucketService;
    private final GitLabService gitLabService;
    private final AzureDevOpsService azureDevOpsService;
    private final UsageRecordRepository usageRecordRepository;
    private final RateLimitingService rateLimitingService;
    private final NotificationService notificationService;
    private final ConfigurationResolver configurationResolver;
    private final RiskAssessmentService riskAssessmentService;
    private final CoverageTrackingService coverageTrackingService;
    private final AttestationService attestationService;

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

            // 1. Buscar diffs do PR (dispatch por plataforma)
            PullRequest pr = review.getPullRequest();
            List<GitHubService.FileDiff> diffs;
            if (pr.getPlatform() == Platform.BITBUCKET) {
                diffs = bitBucketService.fetchPullRequestDiffs(pr.getProject(), pr.getPrNumber());
            } else if (pr.getPlatform() == Platform.GITLAB) {
                diffs = gitLabService.fetchMergeRequestDiffs(pr.getProject(), pr.getPrNumber());
            } else if (pr.getPlatform() == Platform.AZURE_DEVOPS) {
                diffs = azureDevOpsService.fetchPullRequestDiffs(pr.getProject(), pr.getPrNumber());
            } else {
                diffs = gitHubService.fetchPullRequestDiffs(pr.getProject(), pr.getPrNumber());
            }

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

            // 4b. Aplicar severity gating (filtrar issues abaixo da severidade mínima)
            Long projectId = pr.getProject().getId();
            String minSeverityConfig = configurationResolver.getConfig(projectId, ConfigKeys.REVIEW_MIN_SEVERITY);
            if (minSeverityConfig != null && !minSeverityConfig.isBlank()) {
                try {
                    Severity minSeverity = Severity.valueOf(minSeverityConfig.toUpperCase());
                    int beforeFilter = allIssues.size();
                    allIssues = consolidationService.filterByMinSeverity(allIssues, minSeverity);
                    log.info("Severity gating: filtered {} issues to {} (min={})",
                            beforeFilter, allIssues.size(), minSeverity);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid min_severity config '{}', skipping filter", minSeverityConfig);
                }
            }

            // 5. Salvar issues
            for (Issue issue : allIssues) {
                issue.setReview(review);
                issueRepository.save(issue);
            }

            // 5b. Atualizar cobertura de review
            coverageTrackingService.updateCoverage(review, diffs, allIssues);

            // 6. Atualizar métricas do review
            review.setFilesAnalyzed(diffs.size());
            review.setLinesAddedAnalyzed(diffs.stream().mapToInt(d -> d.additions()).sum());
            review.setLinesRemovedAnalyzed(diffs.stream().mapToInt(d -> d.deletions()).sum());

            // 7. Postar comentário resumo no PR
            String commentId = postingService.postReviewComment(pr, allIssues);
            review.setReviewCommentId(commentId);

            // 7b. Postar comentários inline (CRITICAL/HIGH em linhas específicas)
            postingService.postInlineComments(pr, allIssues);

            // 8. Marcar como completado
            review.complete();
            reviewRepository.save(review);

            // 8b. Gerar attestation criptográfica
            attestationService.createAttestation(review, allIssues);

            // 9. Registrar uso
            recordUsage(review);

            // 10. Enviar notificações (Slack, Teams — assíncrono)
            notificationService.notifyReviewCompleted(review, allIssues);

            // 11. Auto-approve se PR é de baixo risco (opt-in)
            var riskAssessment = riskAssessmentService.assess(pr, diffs, allIssues);
            log.info("PR #{} {}", pr.getPrNumber(), riskAssessment.getSummary());
            if (riskAssessmentService.shouldAutoApprove(pr, riskAssessment)) {
                String approveBody = String.format(
                        "Auto-approved by Pullwise. %s", riskAssessment.getSummary());
                if (pr.getPlatform() == Platform.GITHUB) {
                    gitHubService.approvePullRequest(pr.getProject(), pr.getPrNumber(), approveBody);
                } else if (pr.getPlatform() == Platform.AZURE_DEVOPS) {
                    azureDevOpsService.approvePullRequest(pr.getProject(), pr.getPrNumber());
                }
            }

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

        // Verificar rate limit baseado no plano da organização
        if (pr.getProject() != null && pr.getProject().getOrganization() != null) {
            rateLimitingService.checkReviewLimit(pr.getProject().getOrganization().getId());
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
