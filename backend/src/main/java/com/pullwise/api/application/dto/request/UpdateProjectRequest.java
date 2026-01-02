package com.pullwise.api.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateProjectRequest(
        String name,

        String description,

        String repositoryUrl,

        Boolean isActive,

        Boolean autoReviewEnabled
) {}
