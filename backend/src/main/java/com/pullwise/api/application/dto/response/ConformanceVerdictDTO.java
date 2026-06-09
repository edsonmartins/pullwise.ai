package com.pullwise.api.application.dto.response;

import java.util.List;

/**
 * Verdict of a stateless conformance review. {@code diverges=true} when the code does not satisfy
 * one or more criteria; {@code summary} is a short critique; {@code criteria} carries per-criterion
 * results.
 */
public record ConformanceVerdictDTO(
        boolean diverges,
        String summary,
        List<CriterionResultDTO> criteria
) {
    public record CriterionResultDTO(
            String name,
            boolean ok,
            String note
    ) {}
}
