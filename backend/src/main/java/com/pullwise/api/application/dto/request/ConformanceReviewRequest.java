package com.pullwise.api.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Stateless conformance review: judge whether a diff satisfies acceptance criteria (WHEN/THEN).
 * Used by external orchestrators (e.g. SquadX Pass 5). No project/PR registration required.
 */
public record ConformanceReviewRequest(
        @NotBlank(message = "gitDiff is required")
        String gitDiff,

        @NotEmpty(message = "at least one criterion is required")
        @Valid
        List<CriterionDTO> criteria
) {
    public record CriterionDTO(
            @NotBlank String name,
            @NotBlank String when,
            @NotBlank String then
    ) {}
}
