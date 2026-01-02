package com.pullwise.api.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidade que representa o relacionamento entre Usuário e Organização.
 * Implementa o padrão de associação com roles.
 */
@Entity
@Table(name = "organization_members")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private String role = "MEMBER"; // OWNER, ADMIN, MEMBER

    @Column(name = "is_owner", nullable = false)
    @Builder.Default
    private Boolean isOwner = false;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
        if (role == null) {
            role = "MEMBER";
        }
        if (isOwner == null) {
            isOwner = false;
        }
        // Se for OWNER, automaticamente seta a role
        if (isOwner && "MEMBER".equals(role)) {
            role = "OWNER";
        }
    }

    public boolean isOwner() {
        return isOwner != null && isOwner;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role) || "OWNER".equals(role);
    }

    public boolean hasPermission(String requiredRole) {
        if ("OWNER".equals(role)) {
            return true;
        }
        if ("ADMIN".equals(role) && !"OWNER".equals(requiredRole)) {
            return true;
        }
        return role.equals(requiredRole);
    }
}
