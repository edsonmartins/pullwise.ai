package com.pullwise.api.domain.repository;

import com.pullwise.api.domain.model.ReviewCoverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para ReviewCoverage.
 */
@Repository
public interface ReviewCoverageRepository extends JpaRepository<ReviewCoverage, Long> {

    List<ReviewCoverage> findByReviewId(Long reviewId);

    Optional<ReviewCoverage> findByReviewIdAndFilePath(Long reviewId, String filePath);

    @Query("SELECT COALESCE(AVG(rc.coveragePercentage), 0) FROM ReviewCoverage rc WHERE rc.review.id = :reviewId")
    BigDecimal calculateAverageCoverage(@Param("reviewId") Long reviewId);

    @Query("SELECT COUNT(rc) FROM ReviewCoverage rc WHERE rc.review.id = :reviewId AND rc.linesReviewed > 0")
    long countReviewedFiles(@Param("reviewId") Long reviewId);

    @Query("SELECT COUNT(rc) FROM ReviewCoverage rc WHERE rc.review.id = :reviewId")
    long countTotalFiles(@Param("reviewId") Long reviewId);
}
