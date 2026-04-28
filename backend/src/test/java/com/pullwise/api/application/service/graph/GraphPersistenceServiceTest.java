package com.pullwise.api.application.service.graph;

import com.pullwise.api.application.service.graph.blast.EdgeConfidenceClassifier;
import com.pullwise.api.application.service.graph.model.ClassInfo;
import com.pullwise.api.application.service.graph.model.CodeAnalysisResult;
import com.pullwise.api.application.service.graph.model.MethodInfo;
import com.pullwise.api.domain.enums.ConfidenceTier;
import com.pullwise.api.domain.enums.EdgeKind;
import com.pullwise.api.domain.enums.NodeKind;
import com.pullwise.api.domain.model.CodeGraphEdge;
import com.pullwise.api.domain.model.CodeGraphNode;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.repository.CodeGraphEdgeRepository;
import com.pullwise.api.domain.repository.CodeGraphNodeRepository;
import com.pullwise.api.domain.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do {@link GraphPersistenceService}: validam construção de nós/arestas
 * a partir de {@code CodeAnalysisResult} e idempotência (upsert).
 */
@ExtendWith(MockitoExtension.class)
class GraphPersistenceServiceTest {

    private static final Long PROJECT_ID = 42L;

    @Mock
    private CodeGraphNodeRepository nodeRepository;

    @Mock
    private CodeGraphEdgeRepository edgeRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Spy
    private EdgeConfidenceClassifier confidenceClassifier = new EdgeConfidenceClassifier();

    @InjectMocks
    private GraphPersistenceService service;

    private Project project;

    /** Estado em memória que simula a unique-constraint do banco. */
    private Map<String, CodeGraphNode> nodeStore;
    private Map<String, CodeGraphEdge> edgeStore;

    @BeforeEach
    void setUp() {
        project = Project.builder().id(PROJECT_ID).name("test-project").build();
        nodeStore = new HashMap<>();
        edgeStore = new HashMap<>();

        // Stubs gerais do setUp são lenient: nem todos os tests percorrem todas
        // as branches (e.g. emptyResult não toca nodeRepository).
        lenient().when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        lenient().when(nodeRepository.findByProjectIdAndQualifiedName(eq(PROJECT_ID), anyString()))
                .thenAnswer(inv -> Optional.ofNullable(nodeStore.get(inv.getArgument(1, String.class))));
        lenient().when(nodeRepository.save(any(CodeGraphNode.class))).thenAnswer(inv -> {
            CodeGraphNode n = inv.getArgument(0);
            if (n.getCreatedAt() == null) {
                n.setCreatedAt(LocalDateTime.now());
                n.setUpdatedAt(LocalDateTime.now());
            }
            nodeStore.put(n.getQualifiedName(), n);
            return n;
        });

        lenient().when(edgeRepository.findByProjectIdAndSourceQualifiedAndTargetQualifiedAndKind(
                eq(PROJECT_ID), anyString(), anyString(), any(EdgeKind.class)))
                .thenAnswer(inv -> Optional.ofNullable(edgeStore.get(edgeKey(
                        inv.getArgument(1), inv.getArgument(2), inv.getArgument(3)))));
        lenient().when(edgeRepository.save(any(CodeGraphEdge.class))).thenAnswer(inv -> {
            CodeGraphEdge e = inv.getArgument(0);
            if (e.getCreatedAt() == null) {
                e.setCreatedAt(LocalDateTime.now());
                e.setUpdatedAt(LocalDateTime.now());
            }
            edgeStore.put(edgeKey(e.getSourceQualified(), e.getTargetQualified(), e.getKind()), e);
            return e;
        });
    }

    @Test
    void emptyResult_returnsZeroStats() {
        var stats = service.upsertFromAnalysis(PROJECT_ID, null);
        assertThat(stats.nodesProcessed()).isZero();
        assertThat(stats.edgesProcessed()).isZero();

        stats = service.upsertFromAnalysis(PROJECT_ID, CodeAnalysisResult.builder().classes(List.of()).build());
        assertThat(stats.nodesProcessed()).isZero();
    }

    @Test
    void singleClassWithMethods_createsNodesAndContainsEdges() {
        CodeAnalysisResult result = sampleResult();

        var stats = service.upsertFromAnalysis(PROJECT_ID, result);

        // 1 FILE + 1 CLASS + 2 METHOD = 4 nodes
        assertThat(stats.nodesProcessed()).isEqualTo(4);
        // 1 FILE→CLASS + 2 CLASS→METHOD + 1 INHERITS + 1 IMPLEMENTS + 2 IMPORTS_FROM
        // + 1 CALLS (from method m1 to UserRepository)
        assertThat(stats.edgesProcessed()).isGreaterThanOrEqualTo(8);

        assertThat(nodeStore).containsKey("com.example.UserService");
        assertThat(nodeStore.get("com.example.UserService").getKind()).isEqualTo(NodeKind.CLASS);
        assertThat(nodeStore).containsKey("com.example.UserService#findUser");
        assertThat(nodeStore.get("com.example.UserService#findUser").getKind()).isEqualTo(NodeKind.METHOD);
    }

    @Test
    void runningTwice_isIdempotent() {
        CodeAnalysisResult result = sampleResult();

        var first = service.upsertFromAnalysis(PROJECT_ID, result);
        int nodeCountAfterFirst = nodeStore.size();
        int edgeCountAfterFirst = edgeStore.size();

        var second = service.upsertFromAnalysis(PROJECT_ID, result);

        assertThat(nodeStore).hasSize(nodeCountAfterFirst);
        assertThat(edgeStore).hasSize(edgeCountAfterFirst);
        assertThat(second.nodesProcessed()).isEqualTo(first.nodesProcessed());
    }

    @Test
    void inferredTier_appliedToSimpleNameDependencies() {
        CodeAnalysisResult result = sampleResult();
        service.upsertFromAnalysis(PROJECT_ID, result);

        // BaseService é qualificado (com.example.BaseService) → EXTRACTED
        CodeGraphEdge inheritsEdge = edgeStore.get(edgeKey(
                "com.example.UserService", "com.example.BaseService", EdgeKind.INHERITS));
        assertThat(inheritsEdge).isNotNull();
        assertThat(inheritsEdge.getConfidenceTier()).isEqualTo(ConfidenceTier.EXTRACTED);

        // UserRepository é simple-name → INFERRED, resolvido para com.example.UserRepository
        CodeGraphEdge importsEdge = edgeStore.values().stream()
                .filter(e -> e.getKind() == EdgeKind.IMPORTS_FROM
                        && e.getTargetQualified().equals("com.example.UserRepository"))
                .findFirst()
                .orElse(null);
        assertThat(importsEdge).isNotNull();
        assertThat(importsEdge.getConfidenceTier()).isEqualTo(ConfidenceTier.INFERRED);
    }

    @Test
    void ambiguousTier_appliedToGenericTypes() {
        ClassInfo classInfo = ClassInfo.builder()
                .simpleName("Container")
                .packageName("com.example")
                .qualifiedName("com.example.Container")
                .startLine(1).endLine(10)
                .dependencies(List.of("List<String>"))
                .methods(List.of())
                .build();

        CodeAnalysisResult result = CodeAnalysisResult.builder()
                .filePath("/src/Container.java")
                .classes(List.of(classInfo))
                .allMethods(List.of())
                .build();

        service.upsertFromAnalysis(PROJECT_ID, result);

        CodeGraphEdge ambiguousEdge = edgeStore.values().stream()
                .filter(e -> e.getKind() == EdgeKind.IMPORTS_FROM)
                .findFirst()
                .orElseThrow();
        assertThat(ambiguousEdge.getConfidenceTier()).isEqualTo(ConfidenceTier.AMBIGUOUS);
        assertThat(ambiguousEdge.getConfidenceWeight()).isEqualTo(ConfidenceTier.AMBIGUOUS.getWeight());
    }

    @Test
    void missingProject_throws() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.upsertFromAnalysis(999L, sampleResult()));
    }

    private CodeAnalysisResult sampleResult() {
        MethodInfo m1 = MethodInfo.builder()
                .name("findUser")
                .returnType("User")
                .parameterTypes(List.of("Long"))
                .startLine(10).endLine(15)
                .calledClasses(List.of("UserRepository"))
                .calledMethods(List.of("findById"))
                .complexity(2)
                .build();

        MethodInfo m2 = MethodInfo.builder()
                .name("save")
                .returnType("void")
                .parameterTypes(List.of("User"))
                .startLine(17).endLine(20)
                .calledClasses(List.of())
                .calledMethods(List.of())
                .complexity(1)
                .build();

        ClassInfo userService = ClassInfo.builder()
                .simpleName("UserService")
                .packageName("com.example")
                .qualifiedName("com.example.UserService")
                .startLine(1).endLine(25)
                .superClass("com.example.BaseService")
                .implementedInterfaces(List.of("UserOperations"))
                .dependencies(List.of("UserRepository", "com.example.UserOperations"))
                .methods(List.of(m1, m2))
                .build();

        return CodeAnalysisResult.builder()
                .filePath("/src/main/java/com/example/UserService.java")
                .classes(List.of(userService))
                .allMethods(List.of(m1, m2))
                .build();
    }

    private static String edgeKey(String src, String tgt, EdgeKind kind) {
        return src + "→" + tgt + "/" + kind.name();
    }
}
