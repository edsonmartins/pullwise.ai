package com.pullwise.api.domain.model;

import com.pullwise.api.domain.enums.NodeKind;
import com.pullwise.api.domain.enums.ProgrammingLanguage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Nó do grafo de código (Code Graph v2 / Blast-Radius).
 *
 * <p>Representa um arquivo, classe, interface, enum ou método. Único por
 * (project_id, qualified_name).
 */
@Entity
@Table(name = "code_graph_nodes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "qualified_name"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeGraphNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "qualified_name", nullable = false, length = 512)
    private String qualifiedName;

    @Column(name = "simple_name", nullable = false, length = 255)
    private String simpleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private NodeKind kind;

    @Column(name = "file_path", nullable = false, length = 512)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 20)
    private ProgrammingLanguage language;

    @Column(name = "line_start")
    private Integer lineStart;

    @Column(name = "line_end")
    private Integer lineEnd;

    @Column(name = "is_test", nullable = false)
    @Builder.Default
    private Boolean isTest = false;

    @Column(name = "hotspot_score", nullable = false)
    @Builder.Default
    private Double hotspotScore = 0.0;

    @Column(name = "has_test_coverage", nullable = false)
    @Builder.Default
    private Boolean hasTestCoverage = false;

    /** CSV de keywords de segurança que aparecem no nome ou qualified name. */
    @Column(name = "security_keywords", columnDefinition = "TEXT")
    private String securityKeywords;

    /** JSON livre para metadados adicionais (params, return_type, modifiers...). */
    @Column(name = "extra", columnDefinition = "TEXT")
    private String extra;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
