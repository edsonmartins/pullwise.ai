package com.pullwise.api.application.service.graph.blast;

import com.pullwise.api.domain.repository.CodeGraphNodeRepository;
import com.pullwise.api.domain.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recalcula {@code hotspot_score} de cada nó do code graph com base em quantas
 * issues foram detectadas no arquivo nos últimos {@code lookbackDays} dias.
 *
 * <p>Usado como proxy de churn enquanto a tabela {@code pr_changed_files}
 * (que daria frequência real de modificação) não existe. Implementação
 * intencionalmente simples: log-normalizado para [0..1] por projeto.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotspotComputeService {

    public static final int DEFAULT_LOOKBACK_DAYS = 90;

    private final IssueRepository issueRepository;
    private final CodeGraphNodeRepository nodeRepository;

    /** Recalcula com a janela default de 90 dias. */
    @Transactional
    public HotspotStats recompute(Long projectId) {
        return recompute(projectId, DEFAULT_LOOKBACK_DAYS);
    }

    /**
     * Recalcula {@code hotspot_score} de todos os nós do projeto.
     *
     * @param projectId    projeto a processar
     * @param lookbackDays janela em dias para contagem de issues
     * @return estatísticas da operação
     */
    @Transactional
    public HotspotStats recompute(Long projectId, int lookbackDays) {
        if (projectId == null || lookbackDays < 1) {
            return HotspotStats.empty();
        }

        LocalDateTime since = LocalDateTime.now().minusDays(lookbackDays);
        List<Object[]> rows = issueRepository.countByProjectIdGroupedByFilePathSince(projectId, since);

        if (rows.isEmpty()) {
            log.debug("No issues found for project {} in last {} days; hotspots untouched", projectId, lookbackDays);
            return HotspotStats.empty();
        }

        Map<String, Long> countsByFile = new HashMap<>(rows.size());
        long maxCount = 0;
        for (Object[] row : rows) {
            String filePath = (String) row[0];
            long count = ((Number) row[1]).longValue();
            countsByFile.put(filePath, count);
            if (count > maxCount) maxCount = count;
        }

        // Normalização log: score = ln(1+count) / ln(1+maxCount), domain [0..1]
        double denom = Math.log1p(maxCount);
        int filesUpdated = 0;
        int nodesAffected = 0;
        for (Map.Entry<String, Long> entry : countsByFile.entrySet()) {
            double score = denom > 0 ? Math.log1p(entry.getValue()) / denom : 0.0;
            int affected = nodeRepository.updateHotspotScoreByFile(projectId, entry.getKey(), score);
            if (affected > 0) {
                filesUpdated++;
                nodesAffected += affected;
            }
        }

        log.info("Recomputed hotspots for project={}: {} files updated, {} nodes affected",
                projectId, filesUpdated, nodesAffected);
        return new HotspotStats(filesUpdated, nodesAffected, maxCount);
    }

    public record HotspotStats(int filesUpdated, int nodesAffected, long maxIssuesPerFile) {
        public static HotspotStats empty() {
            return new HotspotStats(0, 0, 0);
        }
    }
}
