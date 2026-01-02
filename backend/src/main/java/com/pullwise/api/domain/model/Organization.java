package com.pullwise.api.domain.model;

import com.pullwise.api.domain.enums.PlanType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidade que representa uma organização (tenant).
 * Implementa multi-tenancy no sistema.
 */
@Entity
@Table(name = "organizations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(unique = true, length = 50)
    private String slug;

    @Column(length = 500)
    private String logoUrl;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PlanType planType = PlanType.FREE;

    @Column(name = "max_repositories")
    private Integer maxRepositories;

    @Column(name = "max_reviews_per_month")
    private Integer maxReviewsPerMonth;

    @Column(name = "github_org_id")
    private String githubOrgId;

    @Column(name = "bitbucket_workspace_id")
    private String bitbucketWorkspaceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<OrganizationMember> members = new HashSet<>();

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Project> projects = new HashSet<>();

    @OneToOne(mappedBy = "organization", cascade = CascadeType.ALL)
    private Subscription subscription;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (planType == PlanType.FREE && maxRepositories == null) {
            maxRepositories = planType.getMaxRepositories();
            maxReviewsPerMonth = planType.getMaxReviewsPerMonth();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean hasExceededRepositoryLimit(int currentCount) {
        if (planType.hasUnlimitedRepositories()) {
            return false;
        }
        return maxRepositories != null && currentCount >= maxRepositories;
    }

    public boolean hasExceededReviewLimit(int currentCount) {
        if (planType.hasUnlimitedReviews()) {
            return false;
        }
        return maxReviewsPerMonth != null && currentCount >= maxReviewsPerMonth;
    }
}
