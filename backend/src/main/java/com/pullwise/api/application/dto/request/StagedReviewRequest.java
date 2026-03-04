package com.pullwise.api.application.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para request de review em staged changes (pre-commit).
 * Aceita diff raw do git sem necessidade de PullRequest existente.
 */
public record StagedReviewRequest(
        Long projectId,

        @NotBlank(message = "Diff content is required")
        String diff,

        java.util.List<String> filePaths,

        String commitMessage
) {}
