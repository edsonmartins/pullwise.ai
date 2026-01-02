package com.pullwise.api.domain.model;

import com.pullwise.api.domain.enums.Platform;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidade que representa um projeto dentro de uma organização.
 * Geralmente mapeia para um repositório Git.
 */
@Entity
@Table(name = "projects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "platform", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Platform platform;

    @Column(name = "repository_url", nullable = false, length = 500)
    private String repositoryUrl;

    @Column(name = "repository_id", length = 100)
    private String repositoryId; // ID do repositório no GitHub/BitBucket

    @Column(name = "github_installation_id")
    private Long githubInstallationId;

    @Column(name = "webhook_secret", length = 100)
    private String webhookSecret;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "auto_review_enabled", nullable = false)
    @Builder.Default
    private Boolean autoReviewEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<PullRequest> pullRequests = new HashSet<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Configuration> configurations = new HashSet<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<KnowledgeDocument> knowledgeDocuments = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (autoReviewEnabled == null) {
            autoReviewEnabled = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getFullName() {
        if (platform == Platform.GITHUB) {
            return repositoryUrl.replace("https://github.com/", "")
                    .replace(".git", "");
        }
        return name;
    }
}
