package com.pullwise.api.application.dto.response;

import com.pullwise.api.domain.enums.PlanType;

import java.time.LocalDateTime;

public record OrganizationDTO(
        Long id,
        String name,
        String slug,
        String logoUrl,
        PlanType planType,
        Integer maxRepositories,
        Integer maxReviewsPerMonth,
        int currentRepositoryCount,
        int currentReviewCountThisMonth,
        LocalDateTime createdAt
) {
    public static OrganizationDTO from(com.pullwise.api.domain.model.Organization organization,
                                       int currentRepoCount,
                                       int currentReviewCount) {
        return new OrganizationDTO(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getLogoUrl(),
                organization.getPlanType(),
                organization.getMaxRepositories(),
                organization.getMaxReviewsPerMonth(),
                currentRepoCount,
                currentReviewCount,
                organization.getCreatedAt()
        );
    }
}
