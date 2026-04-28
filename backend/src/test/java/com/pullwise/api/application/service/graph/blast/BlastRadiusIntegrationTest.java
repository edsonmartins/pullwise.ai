package com.pullwise.api.application.service.graph.blast;

import com.pullwise.api.AbstractIntegrationTest;
import com.pullwise.api.domain.enums.ConfidenceTier;
import com.pullwise.api.domain.enums.EdgeKind;
import com.pullwise.api.domain.enums.NodeKind;
import com.pullwise.api.domain.enums.Platform;
import com.pullwise.api.domain.enums.ProgrammingLanguage;
import com.pullwise.api.domain.model.CodeGraphEdge;
import com.pullwise.api.domain.model.CodeGraphNode;
import com.pullwise.api.domain.model.Organization;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.repository.CodeGraphEdgeRepository;
import com.pullwise.api.domain.repository.CodeGraphNodeRepository;
import com.pullwise.api.domain.repository.OrganizationRepository;
import com.pullwise.api.domain.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração reais (Postgres via Testcontainers) que validam:
 * <ul>
 *   <li>CTE recursiva forward em {@code CodeGraphEdgeRepository.bfsForward}</li>
 *   <li>Propagação multiplicativa de confidence ao longo do caminho</li>
 *   <li>Truncamento por {@code maxNodes}</li>
 *   <li>Risk scoring sobre dados reais</li>
 *   <li>Cache hit cross-call</li>
 * </ul>
 */
@Import({BlastRadiusService.class, RiskScoringService.class, SecurityKeywordMatcher.class})
class BlastRadiusIntegrationTest extends AbstractIntegrationTest {

    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private CodeGraphNodeRepository nodeRepository;
    @Autowired private CodeGraphEdgeRepository edgeRepository;
    @Autowired private BlastRadiusService blastRadiusService;

    private Project project;

    @BeforeEach
    void setUp() {
        edgeRepository.deleteAll();
        nodeRepository.deleteAll();

        Organization org = organizationRepository.save(Organization.builder()
                .name("test-org-" + System.nanoTime())
                .planType(com.pullwise.api.domain.enums.PlanType.FREE)
                .build());

        project = projectRepository.save(Project.builder()
                .name("test-project")
                .organization(org)
                .platform(Platform.GITHUB)
                .repositoryUrl("https://github.com/test/project")
                .build());
    }

    /**
     * Grafo:
     * <pre>
     *   A → B → C → D
     *       └→ E
     * </pre>
     * Seed em A: depth 1=B, depth 2=C+E.
     */
    @Test
    @DisplayName("BFS forward depth=2 alcança nós a 2 hops")
    void bfsForward_reachesNodesWithinDepth() {
        node("com.x.A", "/A.java");
        node("com.x.B", "/B.java");
        node("com.x.C", "/C.java");
        node("com.x.D", "/D.java");
        node("com.x.E", "/E.java");

        edge("com.x.A", "com.x.B", EdgeKind.CALLS, ConfidenceTier.EXTRACTED);
        edge("com.x.B", "com.x.C", EdgeKind.CALLS, ConfidenceTier.EXTRACTED);
        edge("com.x.C", "com.x.D", EdgeKind.CALLS, ConfidenceTier.EXTRACTED);
        edge("com.x.B", "com.x.E", EdgeKind.IMPORTS_FROM, ConfidenceTier.EXTRACTED);

        BlastRadiusResult result = blastRadiusService.computeBlastRadius(
                project.getId(), List.of("/A.java"), BlastRadiusOptions.defaults());

        assertThat(result.seedCount()).isEqualTo(1);
        // depth 1: B; depth 2: C, E (D está a depth 3, fora do default maxDepth=2)
        assertThat(result.impactedNodes())
                .extracting(ImpactedNode::qualifiedName)
                .containsExactlyInAnyOrder("com.x.B", "com.x.C", "com.x.E");
        assertThat(result.impactedFiles())
                .containsExactlyInAnyOrder("/B.java", "/C.java", "/E.java");

        ImpactedNode b = result.impactedNodes().stream()
                .filter(n -> n.qualifiedName().equals("com.x.B")).findFirst().orElseThrow();
        assertThat(b.depth()).isEqualTo(1);
        assertThat(b.propagatedConfidence()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Propagação multiplicativa de confidence em cadeia AMBIGUOUS×INFERRED")
    void confidencePropagatesMultiplicatively() {
        node("com.x.A", "/A.java");
        node("com.x.B", "/B.java");
        node("com.x.C", "/C.java");

        edge("com.x.A", "com.x.B", EdgeKind.CALLS, ConfidenceTier.AMBIGUOUS);   // 0.4
        edge("com.x.B", "com.x.C", EdgeKind.CALLS, ConfidenceTier.INFERRED);    // 0.7

        BlastRadiusResult result = blastRadiusService.computeBlastRadius(
                project.getId(), List.of("/A.java"), BlastRadiusOptions.defaults());

        ImpactedNode c = result.impactedNodes().stream()
                .filter(n -> n.qualifiedName().equals("com.x.C")).findFirst().orElseThrow();
        // 1.0 * 0.4 * 0.7 = 0.28
        assertThat(c.propagatedConfidence()).isEqualTo(0.4 * 0.7);
    }

    @Test
    @DisplayName("Truncamento quando resultado excede maxNodes")
    void truncatedFlag_setWhenExceedingMaxNodes() {
        node("com.x.Seed", "/Seed.java");
        // Cria 5 vizinhos diretos
        for (int i = 0; i < 5; i++) {
            node("com.x.Hit" + i, "/Hit" + i + ".java");
            edge("com.x.Seed", "com.x.Hit" + i, EdgeKind.CALLS, ConfidenceTier.EXTRACTED);
        }

        BlastRadiusOptions opts = new BlastRadiusOptions(2, 3, BlastRadiusOptions.DEFAULT_KINDS);
        BlastRadiusResult result = blastRadiusService.computeBlastRadius(
                project.getId(), List.of("/Seed.java"), opts);

        assertThat(result.truncated()).isTrue();
    }

    @Test
    @DisplayName("Cache hit em segunda chamada idêntica")
    void cacheHitsOnSecondCall() {
        node("com.x.A", "/A.java");
        node("com.x.B", "/B.java");
        edge("com.x.A", "com.x.B", EdgeKind.CALLS, ConfidenceTier.EXTRACTED);

        var first = blastRadiusService.computeBlastRadius(
                project.getId(), List.of("/A.java"), BlastRadiusOptions.defaults());
        var second = blastRadiusService.computeBlastRadius(
                project.getId(), List.of("/A.java"), BlastRadiusOptions.defaults());

        // Resultado idêntico mostrando que o cache (ou query determinística) retorna o mesmo
        assertThat(second).isEqualTo(first);
        assertThat(second.impactedNodes()).hasSize(1);
    }

    @Test
    @DisplayName("Risk score não-zero para nó com security keyword e sem testes")
    void riskScore_higherForSensitiveNodes() {
        node("com.x.A", "/A.java");
        nodeWith("com.x.AuthService", "AuthService", "/Auth.java", false);
        edge("com.x.A", "com.x.AuthService", EdgeKind.CALLS, ConfidenceTier.EXTRACTED);

        BlastRadiusResult result = blastRadiusService.computeBlastRadius(
                project.getId(), List.of("/A.java"), BlastRadiusOptions.defaults());

        ImpactedNode auth = result.impactedNodes().stream()
                .filter(n -> n.qualifiedName().equals("com.x.AuthService")).findFirst().orElseThrow();
        assertThat(auth.riskScore()).isGreaterThan(0.3);
        assertThat(auth.riskBreakdown()).containsKeys("security", "test_gap");
    }

    private void node(String qn, String filePath) {
        nodeWith(qn, qn.substring(qn.lastIndexOf('.') + 1), filePath, false);
    }

    private void nodeWith(String qn, String simpleName, String filePath, boolean tested) {
        nodeRepository.save(CodeGraphNode.builder()
                .project(project)
                .qualifiedName(qn)
                .simpleName(simpleName)
                .kind(NodeKind.CLASS)
                .filePath(filePath)
                .language(ProgrammingLanguage.JAVA)
                .hotspotScore(0.0)
                .hasTestCoverage(tested)
                .build());
    }

    private void edge(String source, String target, EdgeKind kind, ConfidenceTier tier) {
        edgeRepository.save(CodeGraphEdge.builder()
                .project(project)
                .sourceQualified(source)
                .targetQualified(target)
                .kind(kind)
                .confidenceTier(tier)
                .confidenceWeight(tier.getWeight())
                .build());
    }
}
