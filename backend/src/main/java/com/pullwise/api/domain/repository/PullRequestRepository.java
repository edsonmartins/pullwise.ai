package com.pullwise.api.domain.repository;

import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.enums.Platform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade PullRequest.
 */
@Repository
public interface PullRequestRepository extends JpaRepository<PullRequest, Long> {

    List<PullRequest> findByProjectId(Long projectId);

    @Query("SELECT pr FROM PullRequest pr WHERE pr.project.id = :projectId AND pr.prId = :prId")
    Optional<PullRequest> findByProjectIdAndPrId(@Param("projectId") Long projectId, @Param("prId") Long prId);

    @Query("SELECT pr FROM PullRequest pr WHERE pr.project.id = :projectId AND pr.prNumber = :prNumber")
    Optional<PullRequest> findByProjectIdAndPrNumber(@Param("projectId") Long projectId, @Param("prNumber") Integer prNumber);

    @Query("SELECT pr FROM PullRequest pr WHERE pr.project.repositoryId = :repoId AND pr.platform = :platform AND pr.prNumber = :prNumber")
    Optional<PullRequest> findByRepositoryIdAndPlatformAndPrNumber(
            @Param("repoId") String repoId,
            @Param("platform") Platform platform,
            @Param("prNumber") Integer prNumber
    );

    @Query("SELECT pr FROM PullRequest pr WHERE pr.project.id = :projectId AND pr.isClosed = false AND pr.isMerged = false")
    List<PullRequest> findOpenByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT pr FROM PullRequest pr WHERE pr.project.id = :projectId AND pr.createdAt >= :since")
    List<PullRequest> findByProjectIdAndCreatedAtAfter(@Param("projectId") Long projectId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(pr) FROM PullRequest pr WHERE pr.project.id = :projectId AND pr.createdAt >= :start AND pr.createdAt < :end")
    long countByProjectIdAndPeriod(@Param("projectId") Long projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(pr) FROM PullRequest pr WHERE pr.project.organization.id = :orgId AND pr.createdAt >= :start AND pr.createdAt <= :end")
    long countByOrganizationIdAndCreatedAtBetween(@Param("orgId") Long orgId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
