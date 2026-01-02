package com.pullwise.api.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidade que representa um usuário do sistema.
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 100)
    private String displayName;

    @Column(length = 500)
    private String avatarUrl;

    @Column(name = "github_id")
    private String githubId;

    @Column(name = "github_login")
    private String githubLogin;

    @Column(name = "bitbucket_uuid")
    private String bitbucketUuid;

    @Column(name = "bitbucket_username")
    private String bitbucketUsername;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<OrganizationMember> memberships = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isMemberOfOrganization(Long organizationId) {
        return memberships.stream()
                .anyMatch(m -> m.getOrganization().getId().equals(organizationId));
    }

    public boolean isOwnerOfOrganization(Long organizationId) {
        return memberships.stream()
                .filter(m -> m.getOrganization().getId().equals(organizationId))
                .anyMatch(OrganizationMember::isOwner);
    }
}
