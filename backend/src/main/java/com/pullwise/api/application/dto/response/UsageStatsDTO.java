package com.pullwise.api.application.dto.response;

import java.util.Map;

public record UsageStatsDTO(
        Long organizationId,
        String period,
        long totalReviews,
        long llmTokensUsed,
        long filesScanned,
        long linesAnalyzed,
        Map<String, Long> breakdownByProject,
        boolean withinLimits
) {}
