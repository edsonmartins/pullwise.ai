package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.service.graph.blast.BlastRadiusOptions;
import com.pullwise.api.application.service.graph.blast.BlastRadiusResult;
import com.pullwise.api.application.service.graph.blast.BlastRadiusService;
import com.pullwise.api.application.service.graph.blast.HotspotComputeService;
import com.pullwise.api.domain.enums.EdgeKind;
import com.pullwise.api.domain.repository.CodeGraphEdgeRepository;
import com.pullwise.api.domain.repository.CodeGraphNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Endpoints REST do Blast-Radius v2 e do Code Graph.
 */
@Slf4j
@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class BlastRadiusController {

    private final BlastRadiusService blastRadiusService;
    private final HotspotComputeService hotspotComputeService;
    private final CodeGraphNodeRepository nodeRepository;
    private final CodeGraphEdgeRepository edgeRepository;

    /**
     * Calcula o blast-radius para um conjunto de arquivos alterados.
     */
    @PostMapping("/blast-radius")
    public ResponseEntity<BlastRadiusResult> computeBlastRadius(
            @PathVariable Long projectId,
            @RequestBody BlastRadiusRequest request) {

        BlastRadiusOptions opts = request.toOptions();
        BlastRadiusResult result = blastRadiusService.computeBlastRadius(
                projectId, request.changedFiles(), opts);
        return ResponseEntity.ok(result);
    }

    /**
     * Estatísticas atuais do code graph (contagens) — útil para troubleshooting
     * e dashboards admin.
     */
    @GetMapping("/code-graph/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable Long projectId) {
        long nodeCount = nodeRepository.countByProjectId(projectId);
        long edgeCount = edgeRepository.countByProjectId(projectId);
        return ResponseEntity.ok(Map.of(
                "projectId", projectId,
                "nodeCount", nodeCount,
                "edgeCount", edgeCount
        ));
    }

    /**
     * Recalcula os hotspots (issues frequency proxy de churn) do projeto.
     */
    @PostMapping("/code-graph/hotspots/recompute")
    public ResponseEntity<HotspotComputeService.HotspotStats> recomputeHotspots(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "90") int lookbackDays) {

        HotspotComputeService.HotspotStats stats = hotspotComputeService.recompute(projectId, lookbackDays);
        return ResponseEntity.ok(stats);
    }

    /**
     * Payload da requisição POST /blast-radius.
     */
    public record BlastRadiusRequest(
            List<String> changedFiles,
            Integer maxDepth,
            Integer maxNodes,
            Set<EdgeKind> kinds
    ) {
        public BlastRadiusOptions toOptions() {
            int depth = maxDepth == null ? BlastRadiusOptions.DEFAULT_DEPTH : maxDepth;
            int nodes = maxNodes == null ? BlastRadiusOptions.DEFAULT_MAX_NODES : maxNodes;
            Set<EdgeKind> resolvedKinds = (kinds == null || kinds.isEmpty())
                    ? BlastRadiusOptions.DEFAULT_KINDS
                    : EnumSet.copyOf(kinds);
            return new BlastRadiusOptions(depth, nodes, new HashSet<>(resolvedKinds));
        }
    }
}
