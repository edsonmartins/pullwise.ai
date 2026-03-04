package com.pullwise.api.application.service.auth;

import com.pullwise.api.domain.model.Organization;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.repository.OrganizationMemberRepository;
import com.pullwise.api.domain.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.security.Principal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    private AuthorizationService authorizationService;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private ProjectRepository projectRepository;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService(organizationMemberRepository, projectRepository);
    }

    // --- getUserId tests ---

    @Test
    void getUserId_withValidNumericPrincipal_shouldReturnUserId() {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("42");

        Long userId = authorizationService.getUserId(principal);

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void getUserId_withNullPrincipal_shouldThrowAuthenticationException() {
        assertThatThrownBy(() -> authorizationService.getUserId(null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessageContaining("No authentication principal found");
    }

    @Test
    void getUserId_withNonNumericName_andNoOAuth2Attribute_shouldThrowException() {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("john.doe@example.com");

        assertThatThrownBy(() -> authorizationService.getUserId(principal))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessageContaining("Unable to extract user ID");
    }

    // --- requireOrganizationMember tests ---

    @Test
    void requireOrganizationMember_whenUserIsMember_shouldNotThrow() {
        Long userId = 1L;
        Long orgId = 10L;

        when(organizationMemberRepository.existsByUserIdAndOrganizationId(userId, orgId))
                .thenReturn(true);

        assertThatCode(() -> authorizationService.requireOrganizationMember(userId, orgId))
                .doesNotThrowAnyException();

        verify(organizationMemberRepository).existsByUserIdAndOrganizationId(userId, orgId);
    }

    @Test
    void requireOrganizationMember_whenUserIsNotMember_shouldThrowAccessDenied() {
        Long userId = 1L;
        Long orgId = 10L;

        when(organizationMemberRepository.existsByUserIdAndOrganizationId(userId, orgId))
                .thenReturn(false);

        assertThatThrownBy(() -> authorizationService.requireOrganizationMember(userId, orgId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User does not have access to organization " + orgId);
    }

    // --- requireProjectAccess tests ---

    @Test
    void requireProjectAccess_whenUserHasAccess_shouldNotThrow() {
        Long userId = 1L;
        Long projectId = 100L;
        Long orgId = 10L;

        Organization org = new Organization();
        org.setId(orgId);

        Project project = new Project();
        project.setId(projectId);
        project.setOrganization(org);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(organizationMemberRepository.existsByUserIdAndOrganizationId(userId, orgId))
                .thenReturn(true);

        assertThatCode(() -> authorizationService.requireProjectAccess(userId, projectId))
                .doesNotThrowAnyException();

        verify(projectRepository).findById(projectId);
        verify(organizationMemberRepository).existsByUserIdAndOrganizationId(userId, orgId);
    }

    @Test
    void requireProjectAccess_whenUserHasNoAccess_shouldThrowAccessDenied() {
        Long userId = 1L;
        Long projectId = 100L;
        Long orgId = 10L;

        Organization org = new Organization();
        org.setId(orgId);

        Project project = new Project();
        project.setId(projectId);
        project.setOrganization(org);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(organizationMemberRepository.existsByUserIdAndOrganizationId(userId, orgId))
                .thenReturn(false);

        assertThatThrownBy(() -> authorizationService.requireProjectAccess(userId, projectId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User does not have access to organization " + orgId);
    }

    @Test
    void requireProjectAccess_whenProjectNotFound_shouldThrowIllegalArgument() {
        Long userId = 1L;
        Long projectId = 999L;

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.requireProjectAccess(userId, projectId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void requireProjectAccess_whenProjectHasNoOrganization_shouldNotThrow() {
        Long userId = 1L;
        Long projectId = 100L;

        Project project = new Project();
        project.setId(projectId);
        project.setOrganization(null); // legacy project without org

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatCode(() -> authorizationService.requireProjectAccess(userId, projectId))
                .doesNotThrowAnyException();

        // Should not check organization membership for legacy projects
        verifyNoInteractions(organizationMemberRepository);
    }
}
