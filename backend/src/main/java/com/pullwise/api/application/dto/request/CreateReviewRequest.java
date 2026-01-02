package com.pullwise.api.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateReviewRequest(
        @NotNull(message = "Pull Request ID is required")
        Long pullRequestId,

        boolean sastEnabled,

        boolean llmEnabled,

        boolean ragEnabled
) {}
