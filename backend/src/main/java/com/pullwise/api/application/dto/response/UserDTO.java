package com.pullwise.api.application.dto.response;

import java.time.LocalDateTime;

public record UserDTO(
        Long id,
        String username,
        String email,
        String displayName,
        String avatarUrl,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
    public static UserDTO from(com.pullwise.api.domain.model.User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getIsActive() != null ? user.getIsActive() : false,
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}
