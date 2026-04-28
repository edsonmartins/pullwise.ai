package com.pullwise.api.application.service.graph.blast;

import com.pullwise.api.domain.enums.EdgeKind;
import com.pullwise.api.domain.model.CodeGraphNode;
import com.pullwise.api.domain.repository.CodeGraphEdgeRepository;
import com.pullwise.api.domain.repository.CodeGraphNodeRepository;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calcula o blast-radius (forward BFS) a partir de arquivos alterados, com
 * risk scoring por nó atingido.
 *
 * <p>Usa CTE recursiva no Postgres via {@link CodeGraphEdgeRepository#bfsForward}
 * para escalar em monorepos sem carregar o grafo inteiro em memória. Cacheia o
 * resultado em Redis por 10 min (cache "blast-radius") e expõe métricas via
 * Micrometer.
 */
@Slf4j
@Service
public class BlastRadiusService {

    public static final String CACHE_NAME = "blast-radius";

    private final CodeGraphNodeRepository nodeRepository;
    private final CodeGraphEdgeRepository edgeRepository;
    private final RiskScoringService riskScoringService;
    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    private final Timer hitTimer;
    private final Timer missTimer;
    private final DistributionSummary nodesVisitedSummary;

    @Autowired
    public BlastRadiusService(CodeGraphNodeRepository nodeRepository,
                              CodeGraphEdgeRepository edgeRepository,
                              RiskScoringService riskScoringService,
                              @org.springframework.beans.factory.annotation.Autowired(required = false)
                              CacheManager cacheManager,
                              @org.springframework.beans.factory.annotation.Autowired(required = false)
                              MeterRegistry meterRegistry) {
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.riskScoringService = riskScoringService;
        this.cacheManager = cacheManager;
        this.meterRegistry = meterRegistry;

        if (meterRegistry != null) {
            this.hitTimer = Timer.builder("pullwise.blast_radius.duration")
                    .tag("cache", "hit").register(meterRegistry);
            this.missTimer = Timer.builder("pullwise.blast_radius.duration")
                    .tag("cache", "miss").register(meterRegistry);
            this.nodesVisitedSummary = DistributionSummary.builder("pullwise.blast_radius.nodes_visited")
                    .baseUnit("nodes").register(meterRegistry);
        } else {
            this.hitTimer = null;
            this.missTimer = null;
            this.nodesVisitedSummary = null;
        }
    }

    @Transactional(readOnly = true)
    public BlastRadiusResult computeBlastRadius(Long projectId,
                                                List<String> changedFiles,
                                                BlastRadiusOptions options) {
        if (projectId == null || changedFiles == null || changedFiles.isEmpty()) {
            return BlastRadiusResult.empty();
        }
        BlastRadiusOptions opts = options == null ? BlastRadiusOptions.defaults() : options;

        long startNs = System.nanoTime();
        Cache cache = cacheManager == null ? null : cacheManager.getCache(CACHE_NAME);
        String key = buildCacheKey(projectId, changedFiles, opts);

        if (cache != null) {
            BlastRadiusResult cached = cache.get(key, BlastRadiusResult.class);
            if (cached != null) {
                recordHit(startNs);
                return cached;
            }
        }

        BlastRadiusResult result = computeInternal(projectId, changedFiles, opts);

        if (cache != null) cache.put(key, result);
        recordMiss(startNs, result);
        return result;
    }

    private BlastRadiusResult computeInternal(Long projectId,
                                              List<String> changedFiles,
                                              BlastRadiusOptions opts) {
        // 1) Resolver seeds
        Set<String> seeds = new HashSet<>();
        for (String file : changedFiles) {
            for (CodeGraphNode node : nodeRepository.findByProjectIdAndFilePath(projectId, file)) {
                seeds.add(node.getQualifiedName());
            }
        }
        if (seeds.isEmpty()) {
            log.debug("No seed nodes found for project {} (changed files have no graph entries)", projectId);
            return BlastRadiusResult.empty();
        }

        // 2) BFS forward via CTE recursiva
        String[] seedArray = seeds.toArray(String[]::new);
        String[] kindArray = opts.kinds().stream().map(EdgeKind::name).toArray(String[]::new);

        List<Object[]> rows = edgeRepository.bfsForward(
                projectId, seedArray, kindArray, opts.maxDepth(), opts.maxNodes()
        );
        boolean truncated = rows.size() >= opts.maxNodes();

        // 3) Filtra seeds (depth=0)
        record RawHit(String qn, int depth, double weight) {}
        List<RawHit> raw = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            int depth = ((Number) row[1]).intValue();
            if (depth == 0) continue;
            raw.add(new RawHit((String) row[0], depth, ((Number) row[2]).doubleValue()));
        }
        if (raw.isEmpty()) {
            return new BlastRadiusResult(List.of(), Set.of(), truncated, seeds.size());
        }

        // 4) Lookup metadata + risk scoring
        Map<String, CodeGraphNode> metadata = lookupMetadata(projectId, raw.stream().map(RawHit::qn).toList());

        List<ImpactedNode> impacted = new ArrayList<>(raw.size());
        Set<String> impactedFiles = new LinkedHashSet<>();
        for (RawHit hit : raw) {
            CodeGraphNode node = metadata.get(hit.qn);
            RiskScoringService.RiskScore risk = riskScoringService.score(node, hit.depth, hit.weight);
            String filePath = node == null ? null : node.getFilePath();
            impacted.add(new ImpactedNode(
                    hit.qn, filePath, hit.depth, hit.weight, risk.total(), risk.breakdown()
            ));
            if (filePath != null && !filePath.isBlank()) impactedFiles.add(filePath);
        }

        impacted.sort(Comparator
                .comparingDouble(ImpactedNode::riskScore).reversed()
                .thenComparingInt(ImpactedNode::depth)
                .thenComparing(Comparator.comparingDouble(ImpactedNode::propagatedConfidence).reversed()));

        log.debug("Blast radius for project={} seeds={}: {} impacted nodes, {} files, truncated={}",
                projectId, seeds.size(), impacted.size(), impactedFiles.size(), truncated);

        return new BlastRadiusResult(impacted, impactedFiles, truncated, seeds.size());
    }

    private Map<String, CodeGraphNode> lookupMetadata(Long projectId, List<String> qns) {
        if (qns.isEmpty()) return Map.of();
        Map<String, CodeGraphNode> out = new HashMap<>(qns.size());
        int chunkSize = 500;
        for (int i = 0; i < qns.size(); i += chunkSize) {
            List<String> chunk = qns.subList(i, Math.min(i + chunkSize, qns.size()));
            for (CodeGraphNode node : nodeRepository.findByProjectIdAndQualifiedNameIn(projectId, chunk)) {
                out.put(node.getQualifiedName(), node);
            }
        }
        return out;
    }

    /**
     * Chave de cache determinística: SHA-256 de (projectId, sorted changed files,
     * maxDepth, maxNodes, sorted kinds). Garante hit independentemente da ordem
     * de entrada.
     */
    static String buildCacheKey(Long projectId, List<String> changedFiles, BlastRadiusOptions opts) {
        String filesPart = changedFiles.stream().sorted().collect(Collectors.joining(","));
        String kindsPart = opts.kinds().stream().map(EdgeKind::name).sorted().collect(Collectors.joining(","));
        String raw = projectId + "|" + filesPart + "|" + opts.maxDepth() + "|" + opts.maxNodes() + "|" + kindsPart;
        return sha256Hex(raw);
    }

    private static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private void recordHit(long startNs) {
        if (hitTimer != null) hitTimer.record(System.nanoTime() - startNs, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    private void recordMiss(long startNs, BlastRadiusResult result) {
        if (missTimer != null) {
            missTimer.record(System.nanoTime() - startNs, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
        if (nodesVisitedSummary != null) {
            nodesVisitedSummary.record(result.impactedNodes().size());
        }
        if (meterRegistry != null && result.truncated()) {
            meterRegistry.counter("pullwise.blast_radius.truncated").increment();
        }
    }
}
