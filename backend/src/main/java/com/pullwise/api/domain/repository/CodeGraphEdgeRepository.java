package com.pullwise.api.domain.repository;

import com.pullwise.api.domain.enums.EdgeKind;
import com.pullwise.api.domain.model.CodeGraphEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório de arestas do grafo de código (Code Graph v2).
 */
@Repository
public interface CodeGraphEdgeRepository extends JpaRepository<CodeGraphEdge, Long> {

    Optional<CodeGraphEdge> findByProjectIdAndSourceQualifiedAndTargetQualifiedAndKind(
            Long projectId, String sourceQualified, String targetQualified, EdgeKind kind
    );

    List<CodeGraphEdge> findByProjectIdAndSourceQualified(Long projectId, String sourceQualified);

    List<CodeGraphEdge> findByProjectIdAndTargetQualified(Long projectId, String targetQualified);

    @Query("SELECT COUNT(e) FROM CodeGraphEdge e WHERE e.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    @Modifying
    @Query("DELETE FROM CodeGraphEdge e WHERE e.project.id = :projectId AND e.sourceFilePath IN :filePaths")
    int deleteByProjectIdAndSourceFilePathIn(
            @Param("projectId") Long projectId,
            @Param("filePaths") List<String> filePaths
    );

    /**
     * BFS forward via CTE recursiva (Postgres). Retorna qualified names alcançados
     * a partir dos seeds, propagando o weight (multiplicação ao longo do caminho).
     *
     * <p>Cada linha: [qualified_name, depth, propagated_weight]
     */
    @Query(value = """
            WITH RECURSIVE impacted(qn, depth, weight_acc) AS (
                SELECT seed AS qn, 0 AS depth, CAST(1.0 AS DOUBLE PRECISION) AS weight_acc
                FROM unnest(CAST(:seeds AS text[])) AS seed
                UNION
                SELECT e.target_qualified, i.depth + 1, i.weight_acc * e.confidence_weight
                FROM impacted i
                JOIN code_graph_edges e
                    ON e.source_qualified = i.qn
                    AND e.project_id = :projectId
                    AND e.kind = ANY(CAST(:kinds AS text[]))
                WHERE i.depth < :maxDepth
            )
            SELECT qn, MIN(depth) AS depth, MAX(weight_acc) AS weight
            FROM impacted
            GROUP BY qn
            LIMIT :maxNodes
            """, nativeQuery = true)
    List<Object[]> bfsForward(
            @Param("projectId") Long projectId,
            @Param("seeds") String[] seeds,
            @Param("kinds") String[] kinds,
            @Param("maxDepth") int maxDepth,
            @Param("maxNodes") int maxNodes
    );
}
