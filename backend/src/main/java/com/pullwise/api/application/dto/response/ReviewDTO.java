package com.pullwise.api.application.dto.response;

import com.pullwise.api.domain.enums.ReviewStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewDTO(
        Long id,
        Long pullRequestId,
        Integer prNumber,
        String prTitle,
        ReviewStatus status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Long durationMs,
        Integer filesAnalyzed,
        Integer linesAddedAnalyzed,
        Integer linesRemovedAnalyzed,
        boolean sastEnabled,
        boolean llmEnabled,
        boolean ragEnabled,
        String errorMessage,
        LocalDateTime createdAt,
        ReviewStats stats
) {
    public record ReviewStats(
            int totalIssues,
            int criticalCount,
            int highCount,
            int mediumCount,
            int lowCount,
            int infoCount
    ) {}

    public static ReviewDTO from(com.pullwise.api.domain.model.Review review, ReviewStats stats) {
        return new ReviewDTO(
                review.getId(),
                review.getPullRequest() != null ? review.getPullRequest().getId() : null,
                review.getPullRequest() != null ? review.getPullRequest().getPrNumber() : null,
                review.getPullRequest() != null ? review.getPullRequest().getTitle() : null,
                review.getStatus(),
                review.getStartedAt(),
                review.getCompletedAt(),
                review.getDurationMs(),
                review.getFilesAnalyzed(),
                review.getLinesAddedAnalyzed(),
                review.getLinesRemovedAnalyzed(),
                review.getSastEnabled() != null ? review.getSastEnabled() : false,
                review.getLlmEnabled() != null ? review.getLlmEnabled() : false,
                review.getRagEnabled() != null ? review.getRagEnabled() : false,
                review.getErrorMessage(),
                review.getCreatedAt(),
                stats
        );
    }
}
