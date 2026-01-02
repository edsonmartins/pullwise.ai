package com.pullwise.api.application.service.auth;

import com.pullwise.api.domain.model.Organization;
import com.pullwise.api.domain.model.User;
import com.pullwise.api.domain.repository.OrganizationRepository;
import com.pullwise.api.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de autenticação e gerenciamento de usuários.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final JwtService jwtService;

    /**
     * Processa o login OAuth2 (GitHub).
     * Cria ou atualiza o usuário e gera um token JWT.
     */
    @Transactional
    public String processOAuth2Login(OAuth2AuthenticatedPrincipal principal) {
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String picture = principal.getAttribute("picture");
        String githubLogin = principal.getAttribute("login");

        User user = userRepository.findByEmail(email)
                .map(existingUser -> {
                    existingUser.setDisplayName(name);
                    existingUser.setAvatarUrl(picture);
                    existingUser.setLastLoginAt(LocalDateTime.now());
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .username(githubLogin != null ? githubLogin : email)
                            .displayName(name)
                            .avatarUrl(picture)
                            .githubLogin(githubLogin)
                            .isActive(true)
                            .build();

                    User saved = userRepository.save(newUser);

                    // Criar organização pessoal para o novo usuário
                    createPersonalOrganization(saved, name, githubLogin);

                    return saved;
                });

        return jwtService.generateToken(user.getId().toString());
    }

    /**
     * Cria uma organização pessoal para o usuário.
     */
    private void createPersonalOrganization(User user, String displayName, String githubLogin) {
        String orgName = displayName != null ? displayName + "'s Org" : githubLogin + "'s Org";
        String slug = (githubLogin != null ? githubLogin : user.getUsername()).toLowerCase();

        Organization org = Organization.builder()
                .name(orgName)
                .slug(slug)
                .planType(com.pullwise.api.domain.enums.PlanType.FREE)
                .build();

        org = organizationRepository.save(org);

        // Adicionar usuário como owner
        com.pullwise.api.domain.model.OrganizationMember member =
                com.pullwise.api.domain.model.OrganizationMember.builder()
                        .organization(org)
                        .user(user)
                        .role("OWNER")
                        .isOwner(true)
                        .build();

        org.getMembers().add(member);
        organizationRepository.save(org);

        log.info("Created personal organization for user: {}", user.getEmail());
    }

    /**
     * Busca usuário por ID.
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Busca usuário por email.
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Obtém as organizações do usuário.
     */
    public List<Organization> getUserOrganizations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return user.getMemberships().stream()
                .map(com.pullwise.api.domain.model.OrganizationMember::getOrganization)
                .toList();
    }

    /**
     * Verifica se o usuário é owner de uma organização.
     */
    public boolean isUserOwnerOfOrganization(Long userId, Long orgId) {
        return userRepository.findById(userId)
                .map(user -> user.isOwnerOfOrganization(orgId))
                .orElse(false);
    }
}
