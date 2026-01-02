package com.pullwise.api.domain.repository;

import com.pullwise.api.domain.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade Organization.
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByName(String name);

    Optional<Organization> findBySlug(String slug);

    @Query("SELECT o FROM Organization o WHERE o.githubOrgId = :githubId")
    Optional<Organization> findByGitHubOrgId(@Param("githubId") String githubId);

    @Query("SELECT o FROM Organization o WHERE o.bitbucketWorkspaceId = :workspaceId")
    Optional<Organization> findByBitbucketWorkspaceId(@Param("workspaceId") String workspaceId);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.organization.id = :orgId AND p.isActive = true")
    long countActiveProjectsByOrganizationId(@Param("orgId") Long orgId);

    boolean existsBySlug(String slug);
}
