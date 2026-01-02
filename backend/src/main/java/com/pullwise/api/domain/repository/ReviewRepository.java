package com.pullwise.api.domain.repository;

import com.pullwise.api.domain.model.Review;
import com.pullwise.api.domain.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade Review.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query(value = "SELECT r FROM Review r JOIN r.pullRequest p WHERE p.project.id = :projectId")
    List<Review> findByProjectId(@Param("projectId") Long projectId);

    List<Review> findByPullRequestId(Long pullRequestId);

    @Query(value = "SELECT r FROM Review r JOIN r.pullRequest pr WHERE pr.id = :prId")
    List<Review> findByPullRequestIdOrderByCreatedAtDesc(@Param("prId") Long prId);

    @Query(value = "SELECT * FROM reviews WHERE pull_request_id = :prId ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Optional<Review> findLatestByPullRequestId(@Param("prId") Long prId);

    @Query(value = "SELECT r FROM Review r WHERE r.pullRequest.id = :prId AND r.status IN :statuses")
    List<Review> findByPullRequestIdAndStatusIn(@Param("prId") Long prId, @Param("statuses") List<ReviewStatus> statuses);

    @Query(value = "SELECT r FROM Review r WHERE r.status IN :statuses AND r.startedAt < :timeout")
    List<Review> findStalledReviews(@Param("statuses") List<ReviewStatus> statuses, @Param("timeout") LocalDateTime timeout);

    @Query(value = "SELECT COUNT(r) FROM Review r WHERE r.pullRequest.project.id = :projectId AND r.createdAt >= :start AND r.createdAt < :end")
    long countByProjectIdAndPeriod(@Param("projectId") Long projectId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT COUNT(r) FROM Review r WHERE r.pullRequest.project.organization.id = :orgId AND r.createdAt >= :start AND r.createdAt < :end")
    long countByOrganizationIdAndPeriod(@Param("orgId") Long orgId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT r FROM Review r WHERE r.status = :status")
    List<Review> findByStatusOrderByCreatedAtAsc(@Param("status") ReviewStatus status);

    @Query(value = "SELECT r FROM Review r WHERE r.pullRequest.project.organization.id = :orgId AND r.status = :status")
    List<Review> findByOrganizationIdAndStatus(@Param("orgId") Long orgId, @Param("status") ReviewStatus status);

    @Query("SELECT r FROM Review r WHERE r.pullRequest.project.organization.id = :orgId AND r.createdAt >= :start AND r.createdAt <= :end")
    List<Review> findByOrganizationIdAndCreatedAtBetween(@Param("orgId") Long orgId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.pullRequest.project.organization.id = :orgId")
    long countByOrganizationId(@Param("orgId") Long orgId);
}
