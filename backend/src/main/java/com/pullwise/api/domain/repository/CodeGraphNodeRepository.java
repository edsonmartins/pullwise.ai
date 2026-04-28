package com.pullwise.api.domain.repository;

import com.pullwise.api.domain.enums.NodeKind;
import com.pullwise.api.domain.model.CodeGraphNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório de nós do grafo de código (Code Graph v2).
 */
@Repository
public interface CodeGraphNodeRepository extends JpaRepository<CodeGraphNode, Long> {

    Optional<CodeGraphNode> findByProjectIdAndQualifiedName(Long projectId, String qualifiedName);

    List<CodeGraphNode> findByProjectIdAndFilePath(Long projectId, String filePath);

    List<CodeGraphNode> findByProjectIdAndKind(Long projectId, NodeKind kind);

    @Query("SELECT n FROM CodeGraphNode n WHERE n.project.id = :projectId AND n.qualifiedName IN :qualifiedNames")
    List<CodeGraphNode> findByProjectIdAndQualifiedNameIn(
            @Param("projectId") Long projectId,
            @Param("qualifiedNames") List<String> qualifiedNames
    );

    @Query("SELECT COUNT(n) FROM CodeGraphNode n WHERE n.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    @Modifying
    @Query("DELETE FROM CodeGraphNode n WHERE n.project.id = :projectId AND n.filePath IN :filePaths")
    int deleteByProjectIdAndFilePathIn(
            @Param("projectId") Long projectId,
            @Param("filePaths") List<String> filePaths
    );

    /**
     * Atualiza em batch o {@code hotspot_score} de todos os nós de um arquivo.
     * Usado pelo HotspotComputeService.
     */
    @Modifying
    @Query("UPDATE CodeGraphNode n SET n.hotspotScore = :score, n.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE n.project.id = :projectId AND n.filePath = :filePath")
    int updateHotspotScoreByFile(
            @Param("projectId") Long projectId,
            @Param("filePath") String filePath,
            @Param("score") Double score
    );
}
