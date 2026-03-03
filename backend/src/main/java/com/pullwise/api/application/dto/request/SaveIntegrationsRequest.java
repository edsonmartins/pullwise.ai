package com.pullwise.api.application.dto.request;

public record SaveIntegrationsRequest(
        String sonarqubeUrl,
        String sonarqubeToken,
        String openRouterKey,
        String bitbucketToken
) {}
