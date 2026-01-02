package com.pullwise.api.application.dto.response;

import java.time.LocalDateTime;

public record ConfigurationDTO(
        Long id,
        Long projectId,
        Long organizationId,
        String scope,
        String key,
        String value,
        String valueType,
        LocalDateTime updatedAt
) {
    public static ConfigurationDTO from(com.pullwise.api.domain.model.Configuration config) {
        return new ConfigurationDTO(
                config.getId(),
                config.getProject() != null ? config.getProject().getId() : null,
                config.getOrganization() != null ? config.getOrganization().getId() : null,
                config.getScope(),
                config.getKey(),
                config.getValue(),
                config.getValueType(),
                config.getUpdatedAt()
        );
    }
}
