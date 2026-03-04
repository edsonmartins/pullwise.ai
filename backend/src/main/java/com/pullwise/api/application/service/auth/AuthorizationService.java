package com.pullwise.api.application.service.auth;

import com.pullwise.api.domain.model.OrganizationMember;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.repository.OrganizationMemberRepository;
import com.pullwise.api.domain.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;

/**
 * Centralized authorization service for verifying user access to resources.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final OrganizationMemberRepository organizationMemberRepository;
    private final ProjectRepository projectRepository;

    /**
     * Extracts the user ID from a Principal (JWT or OAuth2).
     */
    public Long getUserId(Principal principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException("No authentication principal found");
        }

        String name = principal.getName();
        if (name != null) {
            try {
                return Long.parseLong(name);
            } catch (NumberFormatException e) {
                log.debug("Principal name is not a numeric user ID: {}", name);
            }
        }

        if (principal instanceof OAuth2AuthenticatedPrincipal oauth) {
            Object userId = oauth.getAttribute("user_id");
            if (userId instanceof Long l) return l;
            if (userId instanceof String s) return Long.parseLong(s);
        }

        throw new AuthenticationCredentialsNotFoundException("Unable to extract user ID from principal");
    }

    /**
     * Gets the current authenticated user's ID from SecurityContext.
     */
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new AuthenticationCredentialsNotFoundException("No authentication found");
        }
        String subject = auth.getName();
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            throw new AuthenticationCredentialsNotFoundException("Unable to extract user ID from authentication");
        }
    }

    /**
     * Returns the organization IDs the user belongs to.
     */
    public List<Long> getUserOrganizationIds(Long userId) {
        return organizationMemberRepository.findOrganizationIdsByUserId(userId);
    }

    /**
     * Checks if the user is a member of the organization. Throws AccessDeniedException if not.
     */
    public void requireOrganizationMember(Long userId, Long organizationId) {
        if (!organizationMemberRepository.existsByUserIdAndOrganizationId(userId, organizationId)) {
            throw new AccessDeniedException("User does not have access to organization " + organizationId);
        }
    }

    /**
     * Checks if the user has admin or owner role in the organization.
     */
    public void requireOrganizationAdmin(Long userId, Long organizationId) {
        OrganizationMember member = organizationMemberRepository
                .findByUserIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new AccessDeniedException("User does not have access to organization " + organizationId));

        if (!member.isAdmin()) {
            throw new AccessDeniedException("User does not have admin access to organization " + organizationId);
        }
    }

    /**
     * Checks if the user has access to a project (via organization membership).
     */
    public void requireProjectAccess(Long userId, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (project.getOrganization() == null) {
            return; // Projects without org are accessible (legacy)
        }

        requireOrganizationMember(userId, project.getOrganization().getId());
    }

    /**
     * Checks if a project belongs to any of the user's organizations.
     */
    public boolean canAccessProject(Long userId, Long projectId) {
        try {
            requireProjectAccess(userId, projectId);
            return true;
        } catch (AccessDeniedException | IllegalArgumentException e) {
            return false;
        }
    }
}
