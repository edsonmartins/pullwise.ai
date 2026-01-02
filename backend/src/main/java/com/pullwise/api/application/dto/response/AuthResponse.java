package com.pullwise.api.application.dto.response;

import java.util.List;

public record AuthResponse(
        String token,
        String type,
        Long expiresIn,
        UserDTO user,
        List<OrganizationDTO> organizations
) {
    public static AuthResponse of(String token, Long expiresIn, UserDTO user, List<OrganizationDTO> organizations) {
        return new AuthResponse(
                token,
                "Bearer",
                expiresIn,
                user,
                organizations
        );
    }
}
