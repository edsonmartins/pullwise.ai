package com.pullwise.api.application.service.graph.blast;

import com.pullwise.api.domain.enums.NodeKind;
import com.pullwise.api.domain.enums.ProgrammingLanguage;
import com.pullwise.api.domain.model.CodeGraphNode;
import com.pullwise.api.domain.repository.CodeGraphEdgeRepository;
import com.pullwise.api.domain.repository.CodeGraphNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do {@link BlastRadiusService}: validam expansão de seeds, mapeamento das
 * linhas da CTE, propagação de confidence e ordenação por riskScore. A CTE
 * recursiva real fica para o teste de integração com Testcontainers.
 */
@ExtendWith(MockitoExtension.class)
class BlastRadiusServiceTest {

    private static final Long PROJECT_ID = 7L;

    @Mock private CodeGraphNodeRepository nodeRepository;
    @Mock private CodeGraphEdgeRepository edgeRepository;

    private BlastRadiusService service;

    @BeforeEach
    void setUp() {
        // Risk scoring com instâncias reais para validar integração.
        // Cache e metrics são opcionais (null = desligado), o que mantém os
        // tests focados em lógica e não em infraestrutura.
        SecurityKeywordMatcher matcher = new SecurityKeywordMatcher();
        RiskScoringService riskScoringService = new RiskScoringService(matcher);
        service = new BlastRadiusService(nodeRepository, edgeRepository, riskScoringService, null, null);

        // Lookup default vazio
        lenient().when(nodeRepository.findByProjectIdAndQualifiedNameIn(eq(PROJECT_ID), any()))
                .thenReturn(List.of());
    }

    @Test
    void emptyChangedFiles_returnsEmpty() {
        BlastRadiusResult result = service.computeBlastRadius(PROJECT_ID, List.of(), null);
        assertThat(result.impactedNodes()).isEmpty();
        assertThat(result.impactedFiles()).isEmpty();
        assertThat(result.truncated()).isFalse();
    }

    @Test
    void noNodesForFile_returnsEmpty() {
        when(nodeRepository.findByProjectIdAndFilePath(PROJECT_ID, "/src/Foo.java"))
                .thenReturn(List.of());

        BlastRadiusResult result = service.computeBlastRadius(
                PROJECT_ID, List.of("/src/Foo.java"), BlastRadiusOptions.defaults());

        assertThat(result.seedCount()).isZero();
        assertThat(result.impactedNodes()).isEmpty();
    }

    @Test
    void seedsBuiltFromChangedFiles_andSentToCte() {
        when(nodeRepository.findByProjectIdAndFilePath(PROJECT_ID, "/src/A.java"))
                .thenReturn(List.of(
                        node("com.x.A", "/src/A.java"),
                        node("com.x.A#go", "/src/A.java")
                ));
        when(edgeRepository.bfsForward(eq(PROJECT_ID), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        new Object[]{"com.x.A", 0, 1.0},
                        new Object[]{"com.x.A#go", 0, 1.0},
                        new Object[]{"com.x.B", 1, 0.7}
                ));

        BlastRadiusResult result = service.computeBlastRadius(
                PROJECT_ID, List.of("/src/A.java"), BlastRadiusOptions.defaults());

        ArgumentCaptor<String[]> seedsCaptor = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<String[]> kindsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(edgeRepository).bfsForward(eq(PROJECT_ID), seedsCaptor.capture(),
                kindsCaptor.capture(), eq(2), eq(500));

        assertThat(seedsCaptor.getValue()).containsExactlyInAnyOrder("com.x.A", "com.x.A#go");
        assertThat(kindsCaptor.getValue()).containsExactlyInAnyOrder("CALLS", "IMPORTS_FROM", "INHERITS");
        assertThat(result.seedCount()).isEqualTo(2);
        // depth=0 (seeds) é filtrado
        assertThat(result.impactedNodes()).hasSize(1);
        assertThat(result.impactedNodes().get(0).qualifiedName()).isEqualTo("com.x.B");
        assertThat(result.impactedNodes().get(0).depth()).isEqualTo(1);
        assertThat(result.impactedNodes().get(0).propagatedConfidence()).isEqualTo(0.7);
    }

    @Test
    void resultsSortedByRiskScoreDescending() {
        when(nodeRepository.findByProjectIdAndFilePath(PROJECT_ID, "/src/A.java"))
                .thenReturn(List.of(node("com.x.A", "/src/A.java")));

        when(edgeRepository.bfsForward(eq(PROJECT_ID), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        new Object[]{"com.x.LowRisk", 1, 1.0},
                        new Object[]{"com.x.HighRisk", 3, 0.7},
                        new Object[]{"com.x.MidRisk", 2, 1.0}
                ));
        // Metadados controlam o risk: AuthService (security keywords) sem testes >> Util com testes
        when(nodeRepository.findByProjectIdAndQualifiedNameIn(eq(PROJECT_ID), any()))
                .thenReturn(List.of(
                        nodeFull("com.x.LowRisk", "/src/L.java", 0.0, true, "MathUtils"),
                        nodeFull("com.x.HighRisk", "/src/H.java", 1.0, false, "AuthTokenService"),
                        nodeFull("com.x.MidRisk", "/src/M.java", 0.5, false, "Helper")
                ));

        BlastRadiusResult result = service.computeBlastRadius(
                PROJECT_ID, List.of("/src/A.java"), BlastRadiusOptions.defaults());

        List<String> ordered = result.impactedNodes().stream().map(ImpactedNode::qualifiedName).toList();
        assertThat(ordered).containsExactly("com.x.HighRisk", "com.x.MidRisk", "com.x.LowRisk");
        // Risk score deve estar populado
        assertThat(result.impactedNodes().get(0).riskScore()).isGreaterThan(
                result.impactedNodes().get(2).riskScore());
        assertThat(result.impactedNodes().get(0).riskBreakdown()).containsKeys(
                "depth_inverse", "hotspot", "security", "test_gap", "confidence");
    }

    @Test
    void truncated_whenResultsHitMaxNodes() {
        when(nodeRepository.findByProjectIdAndFilePath(PROJECT_ID, "/src/A.java"))
                .thenReturn(List.of(node("com.x.A", "/src/A.java")));

        BlastRadiusOptions opts = new BlastRadiusOptions(2, 3, BlastRadiusOptions.DEFAULT_KINDS);
        when(edgeRepository.bfsForward(eq(PROJECT_ID), any(), any(), eq(2), eq(3)))
                .thenReturn(List.of(
                        new Object[]{"a", 1, 1.0},
                        new Object[]{"b", 1, 0.7},
                        new Object[]{"c", 1, 0.4}
                ));

        BlastRadiusResult result = service.computeBlastRadius(
                PROJECT_ID, List.of("/src/A.java"), opts);

        assertThat(result.truncated()).isTrue();
        assertThat(result.impactedNodes()).hasSize(3);
    }

    @Test
    void optionsClampedToHardCaps() {
        BlastRadiusOptions opts = new BlastRadiusOptions(99, 999_999, null);
        assertThat(opts.maxDepth()).isEqualTo(BlastRadiusOptions.DEPTH_HARD_CAP);
        assertThat(opts.maxNodes()).isEqualTo(BlastRadiusOptions.NODES_HARD_CAP);
        assertThat(opts.kinds()).isEqualTo(BlastRadiusOptions.DEFAULT_KINDS);
    }

    @Test
    void impactedFiles_aggregatesUniquePathsFromImpactedNodes() {
        when(nodeRepository.findByProjectIdAndFilePath(PROJECT_ID, "/src/A.java"))
                .thenReturn(List.of(node("com.x.A", "/src/A.java")));
        when(edgeRepository.bfsForward(eq(PROJECT_ID), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        new Object[]{"com.x.B", 1, 0.7},
                        new Object[]{"com.x.C", 1, 0.7},
                        new Object[]{"com.x.D", 2, 0.49}
                ));
        when(nodeRepository.findByProjectIdAndQualifiedNameIn(eq(PROJECT_ID), any()))
                .thenReturn(new ArrayList<>(List.of(
                        node("com.x.B", "/src/B.java"),
                        node("com.x.C", "/src/B.java"),  // mesmo arquivo que B
                        node("com.x.D", "/src/D.java")
                )));

        BlastRadiusResult result = service.computeBlastRadius(
                PROJECT_ID, List.of("/src/A.java"), BlastRadiusOptions.defaults());

        assertThat(result.impactedFiles()).containsExactlyInAnyOrder("/src/B.java", "/src/D.java");
        // Cada ImpactedNode deve ter filePath populado
        assertThat(result.impactedNodes()).allMatch(n -> n.filePath() != null);
    }

    private CodeGraphNode node(String qn, String filePath) {
        return CodeGraphNode.builder()
                .qualifiedName(qn)
                .simpleName(qn.substring(qn.lastIndexOf('.') + 1))
                .kind(qn.contains("#") ? NodeKind.METHOD : NodeKind.CLASS)
                .filePath(filePath)
                .language(ProgrammingLanguage.JAVA)
                .hotspotScore(0.0)
                .hasTestCoverage(false)
                .build();
    }

    private CodeGraphNode nodeFull(String qn, String filePath, double hotspot, boolean tested, String simpleName) {
        return CodeGraphNode.builder()
                .qualifiedName(qn)
                .simpleName(simpleName)
                .kind(NodeKind.CLASS)
                .filePath(filePath)
                .language(ProgrammingLanguage.JAVA)
                .hotspotScore(hotspot)
                .hasTestCoverage(tested)
                .build();
    }
}
