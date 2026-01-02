package com.pullwise.api.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidade que representa um documento de conhecimento para RAG.
 * Armazena embeddings usando pgvector.
 */
@Entity
@Table(name = "knowledge_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON com metadados adicionais

    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private String embedding; // Armazenado como string, mas a coluna é vector

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(name = "total_chunks")
    private Integer totalChunks;

    @Column(name = "source_type", length = 50)
    private String sourceType; // README, CONTRIBUTING, CODE, DOC_URL

    @Column(name = "source_path", length = 500)
    private String sourcePath;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
}
