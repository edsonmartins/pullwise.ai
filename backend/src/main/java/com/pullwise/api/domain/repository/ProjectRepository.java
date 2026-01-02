package com.pullwise.api.domain.repository;

import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.enums.Platform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade Project.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOrganizationId(Long organizationId);

    @Query("SELECT p FROM Project p WHERE p.organization.id = :orgId AND p.isActive = true")
    List<Project> findActiveByOrganizationId(@Param("orgId") Long orgId);

    Optional<Project> findByOrganizationIdAndName(Long organizationId, String name);

    @Query("SELECT p FROM Project p WHERE p.organization.id = :orgId AND p.repositoryId = :repoId")
    Optional<Project> findByOrganizationIdAndRepositoryId(@Param("orgId") Long orgId, @Param("repoId") String repoId);

    @Query("SELECT p FROM Project p WHERE p.repositoryId = :repoId AND p.platform = :platform")
    Optional<Project> findByRepositoryIdAndPlatform(@Param("repoId") String repoId, @Param("platform") Platform platform);

    @Query("SELECT p FROM Project p WHERE p.organization.id = :orgId AND p.platform = :platform")
    List<Project> findByOrganizationIdAndPlatform(@Param("orgId") Long orgId, @Param("platform") Platform platform);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.organization.id = :orgId AND p.isActive = true")
    long countActiveByOrganizationId(@Param("orgId") Long orgId);

    boolean existsByOrganizationIdAndName(Long organizationId, String name);
}
