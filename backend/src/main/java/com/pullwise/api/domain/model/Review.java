package com.pullwise.api.domain.model;

import com.pullwise.api.domain.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidade que representa uma análise/review de um Pull Request.
 */
@Entity
@Table(name = "reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pull_request_id", nullable = false)
    private PullRequest pullRequest;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "files_analyzed")
    private Integer filesAnalyzed;

    @Column(name = "lines_added_analyzed")
    private Integer linesAddedAnalyzed;

    @Column(name = "lines_removed_analyzed")
    private Integer linesRemovedAnalyzed;

    @Column(name = "sast_enabled")
    @Builder.Default
    private Boolean sastEnabled = true;

    @Column(name = "llm_enabled")
    @Builder.Default
    private Boolean llmEnabled = true;

    @Column(name = "rag_enabled")
    @Builder.Default
    private Boolean ragEnabled = false;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "review_comment_id", length = 100)
    private String reviewCommentId; // ID do comentário postado no PR

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Issue> issues = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ReviewStatus.PENDING;
        }
        if (sastEnabled == null) {
            sastEnabled = true;
        }
        if (llmEnabled == null) {
            llmEnabled = true;
        }
        if (ragEnabled == null) {
            ragEnabled = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (status == ReviewStatus.COMPLETED || status == ReviewStatus.FAILED) {
            completedAt = LocalDateTime.now();
            if (startedAt != null) {
                durationMs = java.time.Duration.between(startedAt, completedAt).toMillis();
            }
        }
    }

    public void start() {
        this.status = ReviewStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = ReviewStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        if (startedAt != null) {
            this.durationMs = java.time.Duration.between(startedAt, completedAt).toMillis();
        }
    }

    public void fail(String errorMessage) {
        this.status = ReviewStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public long getCriticalIssuesCount() {
        return issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.CRITICAL).count();
    }

    public long getHighIssuesCount() {
        return issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.HIGH).count();
    }

    public long getMediumIssuesCount() {
        return issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.MEDIUM).count();
    }

    public long getLowIssuesCount() {
        return issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.LOW).count();
    }
}
