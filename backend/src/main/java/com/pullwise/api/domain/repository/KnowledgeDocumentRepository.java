package com.pullwise.api.domain.repository;

import com.pullwise.api.domain.model.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório para a entidade KnowledgeDocument.
 * Suporta busca vetorial usando pgvector.
 */
@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    List<KnowledgeDocument> findByProjectId(Long projectId);

    List<KnowledgeDocument> findByOrganizationId(Long organizationId);

    @Query("SELECT kd FROM KnowledgeDocument kd WHERE kd.project.id = :projectId AND kd.isActive = true")
    List<KnowledgeDocument> findActiveByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT kd FROM KnowledgeDocument kd WHERE kd.organization.id = :orgId AND kd.isActive = true")
    List<KnowledgeDocument> findActiveByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT kd FROM KnowledgeDocument kd WHERE kd.project.id = :projectId AND kd.sourceType = :sourceType")
    List<KnowledgeDocument> findByProjectIdAndSourceType(@Param("projectId") Long projectId, @Param("sourceType") String sourceType);

    @Query(value = """
        SELECT * FROM knowledge_documents
        WHERE project_id = :projectId
        AND is_active = true
        AND embedding IS NOT NULL
        ORDER BY embedding <=> CAST(:embeddingVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<KnowledgeDocument> findNearestNeighbors(
            @Param("projectId") Long projectId,
            @Param("embeddingVector") String embeddingVector,
            @Param("limit") int limit
    );

    @Query(value = """
        SELECT * FROM knowledge_documents
        WHERE organization_id = :orgId
        AND is_active = true
        AND embedding IS NOT NULL
        ORDER BY embedding <=> CAST(:embeddingVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<KnowledgeDocument> findNearestNeighborsByOrganization(
            @Param("orgId") Long orgId,
            @Param("embeddingVector") String embeddingVector,
            @Param("limit") int limit
    );

    @Query("SELECT COUNT(kd) FROM KnowledgeDocument kd WHERE kd.project.id = :projectId AND kd.isActive = true")
    long countActiveByProjectId(@Param("projectId") Long projectId);
}
