package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.config.TestCacheConfig;
import com.pullwise.api.config.TestSecurityConfig;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E HTTP test do {@link BlastRadiusController}.
 *
 * <p>Sobe o full Spring Boot context com Postgres real (Testcontainers),
 * cria projeto + grafo via repositories, e dispara a chamada HTTP através do
 * {@link TestRestTemplate} para validar serialização JSON e roteamento.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "pullwise.llm.enabled=false"
        })
@ActiveProfiles("test")
@Testcontainers
@Import({TestCacheConfig.class, TestSecurityConfig.class})
class BlastRadiusControllerE2ETest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("pullwise_e2e")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init-test-db.sql")
            .withReuse(true);

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private CodeGraphNodeRepository nodeRepository;
    @Autowired private CodeGraphEdgeRepository edgeRepository;

    private Project project;

    @BeforeEach
    void setUp() {
        edgeRepository.deleteAll();
        nodeRepository.deleteAll();

        Organization org = organizationRepository.save(Organization.builder()
                .name("e2e-org-" + System.nanoTime())
                .planType(com.pullwise.api.domain.enums.PlanType.FREE)
                .build());
        project = projectRepository.save(Project.builder()
                .name("e2e-project")
                .organization(org)
                .platform(Platform.GITHUB)
                .repositoryUrl("https://github.com/test/e2e")
                .build());

        seedNode("com.x.A", "/A.java");
        seedNode("com.x.B", "/B.java");
        seedEdge("com.x.A", "com.x.B", EdgeKind.CALLS, ConfidenceTier.EXTRACTED);
    }

    @Test
    @WithMockUser
    void postBlastRadius_returnsJsonResult() {
        Map<String, Object> body = Map.of("changedFiles", List.of("/A.java"));
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/projects/" + project.getId() + "/blast-radius",
                HttpMethod.POST,
                request,
                Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("seedCount")).isEqualTo(1);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> impactedNodes = (List<Map<String, Object>>) response.getBody().get("impactedNodes");
        assertThat(impactedNodes).hasSize(1);
        assertThat(impactedNodes.get(0).get("qualifiedName")).isEqualTo("com.x.B");
    }

    @Test
    @WithMockUser
    void getCodeGraphStats_returnsCounts() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/projects/" + project.getId() + "/code-graph/stats", Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(((Number) response.getBody().get("nodeCount")).longValue()).isEqualTo(2);
        assertThat(((Number) response.getBody().get("edgeCount")).longValue()).isEqualTo(1);
    }

    private void seedNode(String qn, String filePath) {
        nodeRepository.save(CodeGraphNode.builder()
                .project(project)
                .qualifiedName(qn)
                .simpleName(qn.substring(qn.lastIndexOf('.') + 1))
                .kind(NodeKind.CLASS)
                .filePath(filePath)
                .language(ProgrammingLanguage.JAVA)
                .hotspotScore(0.0)
                .hasTestCoverage(false)
                .build());
    }

    private void seedEdge(String source, String target, EdgeKind kind, ConfidenceTier tier) {
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
