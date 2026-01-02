package com.pullwise.api.domain.model;

import com.pullwise.api.domain.enums.Platform;
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
 * Entidade que representa um Pull Request no sistema.
 */
@Entity
@Table(name = "pull_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "platform", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Platform platform;

    @Column(name = "pr_id", nullable = false)
    private Long prId; // ID do PR no GitHub/BitBucket

    @Column(name = "pr_number", nullable = false)
    private Integer prNumber;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "source_branch", nullable = false, length = 100)
    private String sourceBranch;

    @Column(name = "target_branch", nullable = false, length = 100)
    private String targetBranch;

    @Column(name = "author_name", length = 100)
    private String authorName;

    @Column(name = "author_email", length = 100)
    private String authorEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "merged_at")
    private LocalDateTime mergedAt;

    @Column(name = "is_merged")
    @Builder.Default
    private Boolean isMerged = false;

    @Column(name = "is_closed")
    @Builder.Default
    private Boolean isClosed = false;

    @Column(name = "review_url", length = 500)
    private String reviewUrl;

    @OneToMany(mappedBy = "pullRequest", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Review> reviews = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isMerged == null) {
            isMerged = false;
        }
        if (isClosed == null) {
            isClosed = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isOpen() {
        return !isClosed && !isMerged;
    }

    public boolean hasActiveReview() {
        return reviews.stream()
                .anyMatch(r -> !r.getStatus().isTerminal());
    }

    public Review getLatestReview() {
        return reviews.stream()
                .max((r1, r2) -> r1.getCreatedAt().compareTo(r2.getCreatedAt()))
                .orElse(null);
    }
}
