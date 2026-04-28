package com.pullwise.api.domain.model;

import com.pullwise.api.domain.enums.ConfidenceTier;
import com.pullwise.api.domain.enums.EdgeKind;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Aresta do grafo de código (Code Graph v2 / Blast-Radius).
 *
 * <p>Representa uma dependência direcional source → target (CALLS, IMPORTS_FROM,
 * INHERITS, etc). Única por (project_id, source_qualified, target_qualified, kind).
 */
@Entity
@Table(name = "code_graph_edges", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "source_qualified", "target_qualified", "kind"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeGraphEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "source_qualified", nullable = false, length = 512)
    private String sourceQualified;

    @Column(name = "target_qualified", nullable = false, length = 512)
    private String targetQualified;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private EdgeKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_tier", nullable = false, length = 15)
    @Builder.Default
    private ConfidenceTier confidenceTier = ConfidenceTier.EXTRACTED;

    @Column(name = "confidence_weight", nullable = false)
    @Builder.Default
    private Double confidenceWeight = 1.0;

    @Column(name = "source_file_path", length = 512)
    private String sourceFilePath;

    @Column(name = "source_line")
    private Integer sourceLine;

    /** JSON livre para metadados adicionais. */
    @Column(name = "extra", columnDefinition = "TEXT")
    private String extra;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (confidenceWeight == null && confidenceTier != null) {
            confidenceWeight = confidenceTier.getWeight();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
