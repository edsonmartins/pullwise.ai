package com.pullwise.api.domain.repository;

import com.pullwise.api.domain.enums.FixStatus;
import com.pullwise.api.domain.model.FixSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositório de FixSuggestion.
 */
@Repository
public interface FixSuggestionRepository extends JpaRepository<FixSuggestion, Long> {

    /**
     * Busca sugestões por review.
     */
    List<FixSuggestion> findByReviewIdOrderByCreatedAtDesc(Long reviewId);

    /**
     * Busca sugestões por issue.
     */
    List<FixSuggestion> findByIssueId(Long issueId);

    /**
     * Busca sugestões por status.
     */
    List<FixSuggestion> findByStatus(FixStatus status);

    /**
     * Busca sugestões prontas para aplicar (APPROVED + HIGH confidence).
     */
    @Query("SELECT fs FROM FixSuggestion fs " +
            "WHERE fs.status = 'APPROVED' " +
            "AND fs.confidence = 'HIGH' " +
            "AND fs.review.id = :reviewId")
    List<FixSuggestion> findReadyToApply(@Param("reviewId") Long reviewId);

    /**
     * Busca sugestões por review e status.
     */
    List<FixSuggestion> findByReviewIdAndStatus(Long reviewId, FixStatus status);

    /**
     * Conta sugestões por status em um review.
     */
    long countByReviewIdAndStatus(Long reviewId, FixStatus status);

    /**
     * Busca sugestões pendentes antigas (mais de X horas).
     */
    @Query("SELECT fs FROM FixSuggestion fs " +
            "WHERE fs.status = 'PENDING' " +
            "AND fs.createdAt < :threshold")
    List<FixSuggestion> findOldPendingSuggestions(@Param("threshold") LocalDateTime threshold);

    /**
     * Atualiza status de múltiplas sugestões.
     */
    @Modifying
    @Query("UPDATE FixSuggestion fs " +
            "SET fs.status = :status, " +
            "fs.reviewedBy = :reviewedBy, " +
            "fs.reviewedAt = :reviewedAt " +
            "WHERE fs.id IN :ids")
    int bulkUpdateStatus(@Param("ids") List<Long> ids,
                          @Param("status") FixStatus status,
                          @Param("reviewedBy") String reviewedBy,
                          @Param("reviewedAt") LocalDateTime reviewedAt);

    /**
     * Busca sugestão por issue e status.
     */
    Optional<FixSuggestion> findByIssueIdAndStatus(Long issueId, FixStatus status);
}
