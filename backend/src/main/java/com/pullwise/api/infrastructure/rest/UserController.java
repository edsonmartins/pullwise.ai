package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.dto.request.UpdateProfileRequest;
import com.pullwise.api.application.dto.response.UserDTO;
import com.pullwise.api.domain.model.User;
import com.pullwise.api.domain.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Controller REST para gerenciamento de perfil do usuário.
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * Atualiza o perfil do usuário autenticado.
     */
    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Principal principal) {

        Long userId = getUserIdFromPrincipal(principal);

        return userRepository.findById(userId)
                .map(user -> {
                    user.setDisplayName(request.displayName());
                    User saved = userRepository.save(user);
                    return ResponseEntity.ok(UserDTO.from(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Long getUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "No authentication principal found");
        }
        String name = principal.getName();
        if (name != null) {
            try {
                return Long.parseLong(name);
            } catch (NumberFormatException e) {
                log.debug("Principal name is not a numeric user ID: {}", name);
            }
        }
        throw new AuthenticationCredentialsNotFoundException(
                "Unable to extract user ID from authentication principal");
    }
}
