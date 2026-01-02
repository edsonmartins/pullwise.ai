package com.pullwise.api.application.service;

import com.pullwise.api.application.service.graph.CodeGraphService;
import com.pullwise.api.application.service.graph.DependencyGraphService;
import com.pullwise.api.application.service.graph.JavaParserService;
import com.pullwise.api.domain.model.CodeGraphData;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Testes de integração para CodeGraphService.
 */
@ExtendWith(MockitoExtension.class)
class CodeGraphServiceTest {

    private CodeGraphService codeGraphService;

    @Mock
    private JavaParserService javaParserService;

    @Mock
    private DependencyGraphService dependencyGraphService;

    @BeforeEach
    void setUp() {
        codeGraphService = new CodeGraphService(javaParserService, dependencyGraphService);
    }

    @Test
    void testAnalyzeCodebaseReturnsGraph() {
        // Arrange
        DirectedMultigraph<String, DefaultEdge> mockGraph = new DirectedMultigraph<>(DefaultEdge.class);
        mockGraph.addVertex("com.example.ClassA");
        mockGraph.addVertex("com.example.ClassB");
        mockGraph.addEdge("com.example.ClassA", "com.example.ClassB");

        when(dependencyGraphService.buildGraph(any())).thenReturn(mockGraph);

        // Act
        CodeGraphData result = codeGraphService.analyzeCodebase(1L, "main");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.projectId()).isEqualTo(1L);
    }

    @Test
    void testCalculateBlastRadius() {
        // Arrange
        DirectedMultigraph<String, DefaultEdge> mockGraph = new DirectedMultigraph<>(DefaultEdge.class);
        mockGraph.addVertex("com.example.ClassA");
        mockGraph.addVertex("com.example.ClassB");
        mockGraph.addVertex("com.example.ClassC");
        mockGraph.addEdge("com.example.ClassA", "com.example.ClassB");
        mockGraph.addEdge("com.example.ClassB", "com.example.ClassC");

        when(dependencyGraphService.buildGraph(any())).thenReturn(mockGraph);

        // Act
        var result = codeGraphService.calculateBlastRadius(1L, "com.example.ClassA", 3);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.affectedNodes()).hasSize(3);
    }

    @Test
    void testGetCyclesInGraph() {
        // Arrange
        DirectedMultigraph<String, DefaultEdge> mockGraph = new DirectedMultigraph<>(DefaultEdge.class);
        mockGraph.addVertex("com.example.ClassA");
        mockGraph.addVertex("com.example.ClassB");
        mockGraph.addEdge("com.example.ClassA", "com.example.ClassB");
        mockGraph.addEdge("com.example.ClassB", "com.example.ClassA");

        when(dependencyGraphService.buildGraph(any())).thenReturn(mockGraph);

        // Act
        var cycles = codeGraphService.detectCycles(1L);

        // Assert
        assertThat(cycles).isNotNull();
        assertThat(cycles.size()).isGreaterThan(0);
    }
}
