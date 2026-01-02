package com.pullwise.api.domain.repository;

import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.enums.IssueSource;
import com.pullwise.api.domain.enums.IssueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório para a entidade Issue.
 */
@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findByReviewId(Long reviewId);

    @Query("SELECT i FROM Issue i WHERE i.review.id = :reviewId AND i.isFalsePositive = false")
    List<Issue> findByReviewIdExcludingFalsePositives(@Param("reviewId") Long reviewId);

    List<Issue> findByReviewIdAndSeverity(Long reviewId, Severity severity);

    @Query("SELECT i FROM Issue i WHERE i.review.id = :reviewId AND i.severity IN :severities")
    List<Issue> findByReviewIdAndSeverityIn(@Param("reviewId") Long reviewId, @Param("severities") List<Severity> severities);

    List<Issue> findByReviewIdAndSource(Long reviewId, IssueSource source);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.review.id = :reviewId AND i.severity = :severity")
    long countByReviewIdAndSeverity(@Param("reviewId") Long reviewId, @Param("severity") Severity severity);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.review.id = :reviewId AND i.source = :source")
    long countByReviewIdAndSource(@Param("reviewId") Long reviewId, @Param("source") IssueSource source);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.review.pullRequest.project.id = :projectId AND i.review.createdAt >= :start AND i.review.createdAt < :end")
    long countByProjectIdAndPeriod(@Param("projectId") Long projectId, @Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query(value = "SELECT i FROM Issue i WHERE i.review.pullRequest.project.id = :projectId AND i.isFalsePositive = false")
    List<Issue> findActiveByProjectIdOrderBySeverity(@Param("projectId") Long projectId);

    @Query("SELECT i FROM Issue i WHERE i.review.pullRequest.project.id = :projectId AND i.type = :type")
    List<Issue> findByProjectIdAndType(@Param("projectId") Long projectId, @Param("type") IssueType type);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.review.pullRequest.project.organization.id = :orgId AND i.createdAt >= :start AND i.createdAt <= :end")
    long countByReviewOrganizationIdAndCreatedAtBetween(@Param("orgId") Long orgId, @Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.review.pullRequest.project.organization.id = :orgId AND i.severity = :severity AND i.createdAt >= :start AND i.createdAt <= :end")
    long countByReviewOrganizationIdAndSeverityAndCreatedAtBetween(@Param("orgId") Long orgId, @Param("severity") Severity severity, @Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.review.pullRequest.project.organization.id = :orgId AND i.type = :type AND i.createdAt >= :start AND i.createdAt <= :end")
    long countByReviewOrganizationIdAndIssueTypeAndCreatedAtBetween(@Param("orgId") Long orgId, @Param("type") IssueType type, @Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
}
