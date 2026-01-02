package com.pullwise.api.application.dto.response;

import com.pullwise.api.domain.enums.IssueSource;
import com.pullwise.api.domain.enums.IssueType;
import com.pullwise.api.domain.enums.Severity;

import java.time.LocalDateTime;

public record IssueDTO(
        Long id,
        Long reviewId,
        Severity severity,
        IssueType type,
        IssueSource source,
        String title,
        String description,
        String filePath,
        Integer lineStart,
        Integer lineEnd,
        String ruleId,
        String suggestion,
        String codeSnippet,
        String fixedCode,
        boolean isFalsePositive,
        LocalDateTime createdAt
) {
    public static IssueDTO from(com.pullwise.api.domain.model.Issue issue) {
        return new IssueDTO(
                issue.getId(),
                issue.getReview() != null ? issue.getReview().getId() : null,
                issue.getSeverity(),
                issue.getType(),
                issue.getSource(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getFilePath(),
                issue.getLineStart(),
                issue.getLineEnd(),
                issue.getRuleId(),
                issue.getSuggestion(),
                issue.getCodeSnippet(),
                issue.getFixedCode(),
                issue.getIsFalsePositive() != null ? issue.getIsFalsePositive() : false,
                issue.getCreatedAt()
        );
    }
}
