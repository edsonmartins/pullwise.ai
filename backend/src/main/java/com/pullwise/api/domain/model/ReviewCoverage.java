package com.pullwise.api.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade que rastreia a cobertura de review por arquivo.
 * Cada registro representa a cobertura de um arquivo específico em um review.
 */
@Entity
@Table(name = "review_coverage", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"review_id", "file_path"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCoverage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "total_lines_changed", nullable = false)
    @Builder.Default
    private Integer totalLinesChanged = 0;

    @Column(name = "lines_reviewed", nullable = false)
    @Builder.Default
    private Integer linesReviewed = 0;

    @Column(name = "coverage_percentage")
    @Builder.Default
    private BigDecimal coveragePercentage = BigDecimal.ZERO;

    @Column(name = "first_reviewed_at")
    private LocalDateTime firstReviewedAt;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Recalcula a porcentagem de cobertura.
     */
    public void recalculateCoverage() {
        if (totalLinesChanged > 0) {
            double pct = (double) linesReviewed / totalLinesChanged * 100.0;
            this.coveragePercentage = BigDecimal.valueOf(Math.min(pct, 100.0)).setScale(2, java.math.RoundingMode.HALF_UP);
        } else {
            this.coveragePercentage = BigDecimal.valueOf(100.0);
        }
    }

    /**
     * Marca linhas adicionais como revisadas.
     */
    public void addReviewedLines(int count) {
        this.linesReviewed = Math.min(this.linesReviewed + count, this.totalLinesChanged);
        this.lastReviewedAt = LocalDateTime.now();
        if (this.firstReviewedAt == null) {
            this.firstReviewedAt = LocalDateTime.now();
        }
        recalculateCoverage();
    }
}
