package com.pullwise.api.application.dto.response;

import com.pullwise.api.domain.enums.Platform;
import com.pullwise.api.domain.enums.ReviewStatus;

import java.time.LocalDateTime;
import java.util.List;

public record PullRequestDTO(
        Long id,
        Long projectId,
        String projectName,
        Platform platform,
        Long prId,
        Integer prNumber,
        String title,
        String description,
        String sourceBranch,
        String targetBranch,
        String authorName,
        String reviewUrl,
        boolean isOpen,
        boolean isMerged,
        boolean isClosed,
        LocalDateTime createdAt,
        LocalDateTime mergedAt,
        LocalDateTime closedAt,
        List<ReviewSummary> reviews
) {
    public record ReviewSummary(
            Long id,
            ReviewStatus status,
            int issueCount,
            LocalDateTime createdAt
    ) {}

    public static PullRequestDTO from(com.pullwise.api.domain.model.PullRequest pr) {
        List<ReviewSummary> reviewSummaries = pr.getReviews().stream()
                .map(r -> new ReviewSummary(
                        r.getId(),
                        r.getStatus(),
                        r.getIssues() != null ? r.getIssues().size() : 0,
                        r.getCreatedAt()
                ))
                .toList();

        return new PullRequestDTO(
                pr.getId(),
                pr.getProject() != null ? pr.getProject().getId() : null,
                pr.getProject() != null ? pr.getProject().getName() : null,
                pr.getPlatform(),
                pr.getPrId(),
                pr.getPrNumber(),
                pr.getTitle(),
                pr.getDescription(),
                pr.getSourceBranch(),
                pr.getTargetBranch(),
                pr.getAuthorName(),
                pr.getReviewUrl(),
                pr.isOpen(),
                pr.getIsMerged() != null ? pr.getIsMerged() : false,
                pr.getIsClosed() != null ? pr.getIsClosed() : false,
                pr.getCreatedAt(),
                pr.getMergedAt(),
                pr.getClosedAt(),
                reviewSummaries
        );
    }
}
