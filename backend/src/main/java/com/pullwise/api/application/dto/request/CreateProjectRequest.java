package com.pullwise.api.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProjectRequest(
        @NotBlank(message = "Name is required")
        String name,

        String description,

        @NotNull(message = "Platform is required")
        com.pullwise.api.domain.enums.Platform platform,

        @NotBlank(message = "Repository URL is required")
        String repositoryUrl,

        String repositoryId,

        Long githubInstallationId,

        boolean autoReviewEnabled
) {}
