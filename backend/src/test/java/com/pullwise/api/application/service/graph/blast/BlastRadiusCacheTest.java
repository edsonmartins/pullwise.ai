package com.pullwise.api.application.service.graph.blast;

import com.pullwise.api.domain.enums.NodeKind;
import com.pullwise.api.domain.enums.ProgrammingLanguage;
import com.pullwise.api.domain.model.CodeGraphNode;
import com.pullwise.api.domain.repository.CodeGraphEdgeRepository;
import com.pullwise.api.domain.repository.CodeGraphNodeRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica integração do cache Redis (em memória aqui via {@link ConcurrentMapCacheManager})
 * e métricas Micrometer.
 */
@ExtendWith(MockitoExtension.class)
class BlastRadiusCacheTest {

    private static final Long PROJECT_ID = 11L;

    @Mock private CodeGraphNodeRepository nodeRepository;
    @Mock private CodeGraphEdgeRepository edgeRepository;

    private BlastRadiusService service;
    private CacheManager cacheManager;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        cacheManager = new ConcurrentMapCacheManager(BlastRadiusService.CACHE_NAME);
        meterRegistry = new SimpleMeterRegistry();

        SecurityKeywordMatcher matcher = new SecurityKeywordMatcher();
        RiskScoringService risk = new RiskScoringService(matcher);
        service = new BlastRadiusService(nodeRepository, edgeRepository, risk,
                cacheManager, meterRegistry);

        lenient().when(nodeRepository.findByProjectIdAndFilePath(eq(PROJECT_ID), eq("/src/A.java")))
                .thenReturn(List.of(node("com.x.A", "/src/A.java")));
        List<Object[]> rows = java.util.Collections.singletonList(new Object[]{"com.x.B", 1, 0.7});
        lenient().when(edgeRepository.bfsForward(eq(PROJECT_ID), any(), any(), anyInt(), anyInt()))
                .thenReturn(rows);
        lenient().when(nodeRepository.findByProjectIdAndQualifiedNameIn(eq(PROJECT_ID), any()))
                .thenReturn(List.of(node("com.x.B", "/src/B.java")));
    }

    @Test
    void firstCallMisses_secondCallHits() {
        BlastRadiusOptions opts = BlastRadiusOptions.defaults();
        List<String> files = List.of("/src/A.java");

        BlastRadiusResult first = service.computeBlastRadius(PROJECT_ID, files, opts);
        BlastRadiusResult second = service.computeBlastRadius(PROJECT_ID, files, opts);

        // CTE só foi chamada uma vez (segundo bate no cache)
        verify(edgeRepository, times(1)).bfsForward(eq(PROJECT_ID), any(), any(), anyInt(), anyInt());
        assertThat(second).isEqualTo(first);

        // Cache foi populado
        Cache cache = cacheManager.getCache(BlastRadiusService.CACHE_NAME);
        assertThat(cache).isNotNull();
        assertThat(cache.get(BlastRadiusService.buildCacheKey(PROJECT_ID, files, opts))).isNotNull();
    }

    @Test
    void differentArgumentsHaveSeparateCacheEntries() {
        // Seed também para /src/B.java
        when(nodeRepository.findByProjectIdAndFilePath(eq(PROJECT_ID), eq("/src/B.java")))
                .thenReturn(List.of(node("com.x.A2", "/src/B.java")));

        BlastRadiusOptions opts = BlastRadiusOptions.defaults();
        service.computeBlastRadius(PROJECT_ID, List.of("/src/A.java"), opts);
        service.computeBlastRadius(PROJECT_ID, List.of("/src/B.java"), opts);

        verify(edgeRepository, times(2)).bfsForward(eq(PROJECT_ID), any(), any(), anyInt(), anyInt());
    }

    @Test
    void emptyOrInvalid_doesNotCacheNorCompute() {
        service.computeBlastRadius(null, List.of("/x"), BlastRadiusOptions.defaults());
        service.computeBlastRadius(PROJECT_ID, List.of(), BlastRadiusOptions.defaults());

        verify(edgeRepository, never()).bfsForward(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void cacheKey_isOrderInsensitiveOnFilesAndKinds() {
        BlastRadiusOptions opts = BlastRadiusOptions.defaults();
        String key1 = BlastRadiusService.buildCacheKey(PROJECT_ID, List.of("/a.java", "/b.java"), opts);
        String key2 = BlastRadiusService.buildCacheKey(PROJECT_ID, List.of("/b.java", "/a.java"), opts);
        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void metrics_recordedOnHitAndMiss() {
        List<String> files = List.of("/src/A.java");
        service.computeBlastRadius(PROJECT_ID, files, BlastRadiusOptions.defaults());
        service.computeBlastRadius(PROJECT_ID, files, BlastRadiusOptions.defaults());

        var miss = meterRegistry.find("pullwise.blast_radius.duration").tag("cache", "miss").timer();
        var hit = meterRegistry.find("pullwise.blast_radius.duration").tag("cache", "hit").timer();
        var nodesVisited = meterRegistry.find("pullwise.blast_radius.nodes_visited").summary();

        assertThat(miss).isNotNull();
        assertThat(hit).isNotNull();
        assertThat(miss.count()).isEqualTo(1);
        assertThat(hit.count()).isEqualTo(1);
        assertThat(nodesVisited).isNotNull();
        assertThat(nodesVisited.count()).isEqualTo(1); // só registrado em miss
    }

    private CodeGraphNode node(String qn, String filePath) {
        return CodeGraphNode.builder()
                .qualifiedName(qn)
                .simpleName(qn.substring(qn.lastIndexOf('.') + 1))
                .kind(NodeKind.CLASS)
                .filePath(filePath)
                .language(ProgrammingLanguage.JAVA)
                .hotspotScore(0.0)
                .hasTestCoverage(false)
                .build();
    }
}
