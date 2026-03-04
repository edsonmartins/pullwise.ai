package com.pullwise.api.domain.repository;

import com.pullwise.api.domain.model.ReviewAttestation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório para ReviewAttestation.
 */
@Repository
public interface ReviewAttestationRepository extends JpaRepository<ReviewAttestation, Long> {

    Optional<ReviewAttestation> findByReviewId(Long reviewId);

    boolean existsByReviewId(Long reviewId);
}
