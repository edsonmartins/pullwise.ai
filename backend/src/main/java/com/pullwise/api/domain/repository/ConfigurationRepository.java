package com.pullwise.api.domain.repository;

import com.pullwise.api.domain.model.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade Configuration.
 */
@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {

    List<Configuration> findByProjectId(Long projectId);

    List<Configuration> findByOrganizationId(Long organizationId);

    @Query("SELECT c FROM Configuration c WHERE c.project.id = :projectId AND c.scope = :scope AND c.key = :key")
    Optional<Configuration> findByProjectIdAndScopeAndKey(
            @Param("projectId") Long projectId,
            @Param("scope") String scope,
            @Param("key") String key
    );

    @Query("SELECT c FROM Configuration c WHERE c.organization.id = :orgId AND c.scope = :scope AND c.key = :key")
    Optional<Configuration> findByOrganizationIdAndScopeAndKey(
            @Param("orgId") Long orgId,
            @Param("scope") String scope,
            @Param("key") String key
    );

    @Query("SELECT c FROM Configuration c WHERE c.project.id = :projectId AND c.scope = :scope")
    List<Configuration> findByProjectIdAndScope(@Param("projectId") Long projectId, @Param("scope") String scope);

    @Query("SELECT c FROM Configuration c WHERE c.organization.id = :orgId AND c.scope = :scope")
    List<Configuration> findByOrganizationIdAndScope(@Param("orgId") Long orgId, @Param("scope") String scope);

    @Query("SELECT c FROM Configuration c WHERE c.project.id = :projectId AND c.key IN :keys")
    List<Configuration> findByProjectIdAndKeyIn(@Param("projectId") Long projectId, @Param("keys") List<String> keys);

    boolean existsByProjectIdAndScopeAndKey(Long projectId, String scope, String key);
}
