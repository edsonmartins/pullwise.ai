package com.pullwise.api.infrastructure.websocket.dto;

import com.pullwise.api.domain.model.Issue;
import java.time.Instant;

/**
 * Mensagem emitida quando uma issue é detectada durante o review.
 */
public record IssueDetectedMessage(
    String type,
    IssueData data,
    Instant timestamp
) {
    public record IssueData(
        Long reviewId,
        Long issueId,
        String severity,
        String issueType,
        String title,
        String filePath,
        Integer lineStart
    ) {}

    public static IssueDetectedMessage fromIssue(Long reviewId, Issue issue) {
        IssueData data = new IssueData(
            reviewId,
            issue.getId(),
            issue.getSeverity().name(),
            issue.getType().name(),
            issue.getTitle(),
            issue.getFilePath(),
            issue.getLineStart()
        );
        return new IssueDetectedMessage("issue.detected", data, Instant.now());
    }
}
