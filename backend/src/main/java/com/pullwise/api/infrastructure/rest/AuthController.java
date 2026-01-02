package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.dto.response.AuthResponse;
import com.pullwise.api.application.dto.response.OrganizationDTO;
import com.pullwise.api.application.dto.response.UserDTO;
import com.pullwise.api.application.service.auth.AuthenticationService;
import com.pullwise.api.application.service.auth.JwtService;
import com.pullwise.api.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para autenticação.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;

    /**
     * Retorna informações do usuário autenticado.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        String email = principal.getAttribute("email");
        return authenticationService.getUserByEmail(email)
                .map(UserDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Callback do OAuth2 (usado pelo frontend).
     */
    @PostMapping("/callback")
    public ResponseEntity<AuthResponse> oauthCallback(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String picture = principal.getAttribute("picture");

        // Processar login e criar/atualizar usuário
        String token = authenticationService.processOAuth2Login(principal);

        User user = authenticationService.getUserByEmail(email).orElse(null);
        List<OrganizationDTO> organizations = user != null
                ? authenticationService.getUserOrganizations(user.getId()).stream()
                        .map(org -> OrganizationDTO.from(org, 0, 0))
                        .toList()
                : List.of();

        AuthResponse response = AuthResponse.of(
                token,
                jwtService.getExpirationTime(),
                user != null ? UserDTO.from(user) : null,
                organizations
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Logout (client-side apenas remove o token).
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }

    /**
     * Renova o token JWT.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        String email = principal.getAttribute("email");
        User user = authenticationService.getUserByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String newToken = jwtService.generateToken(user.getId().toString());
        List<OrganizationDTO> organizations = authenticationService.getUserOrganizations(user.getId()).stream()
                .map(org -> OrganizationDTO.from(org, 0, 0))
                .toList();

        AuthResponse response = AuthResponse.of(
                newToken,
                jwtService.getExpirationTime(),
                UserDTO.from(user),
                organizations
        );

        return ResponseEntity.ok(response);
    }
}
