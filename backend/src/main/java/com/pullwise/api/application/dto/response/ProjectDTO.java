package com.pullwise.api.application.dto.response;

import com.pullwise.api.domain.enums.Platform;

import java.time.LocalDateTime;

public record ProjectDTO(
        Long id,
        String name,
        String description,
        Long organizationId,
        String organizationName,
        Platform platform,
        String repositoryUrl,
        String repositoryId,
        boolean isActive,
        boolean autoReviewEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProjectDTO from(com.pullwise.api.domain.model.Project project) {
        return new ProjectDTO(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getOrganization() != null ? project.getOrganization().getId() : null,
                project.getOrganization() != null ? project.getOrganization().getName() : null,
                project.getPlatform(),
                project.getRepositoryUrl(),
                project.getRepositoryId(),
                project.getIsActive() != null ? project.getIsActive() : false,
                project.getAutoReviewEnabled() != null ? project.getAutoReviewEnabled() : false,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
