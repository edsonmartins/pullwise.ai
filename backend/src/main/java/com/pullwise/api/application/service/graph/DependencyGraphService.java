package com.pullwise.api.application.service.graph;

import com.pullwise.api.application.service.graph.model.ClassInfo;
import com.pullwise.api.application.service.graph.model.CodeAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço para análise de grafos de dependência usando JGraphT.
 *
 * <p>Funcionalidades:
 * - Construir grafo de dependências entre classes
 * - Detectar ciclos (circular dependencies)
 * - Calcular blast radius de mudanças
 * - Encontrar caminhos de dependência
 * - Calcular métricas de acoplamento
 */
@Slf4j
@Service
public class DependencyGraphService {

    private final JavaParserService parserService;

    // Cache de grafos por repositório
    private final Map<String, Graph<String, DefaultEdge>> graphCache = new ConcurrentHashMap<>();

    public DependencyGraphService(JavaParserService parserService) {
        this.parserService = parserService;
    }

    /**
     * Constrói um grafo de dependências para um repositório.
     *
     * @param repositoryId ID do repositório
     * @param filePaths    Lista de arquivos .java para analisar
     * @return Grafo de dependências
     */
    public Graph<String, DefaultEdge> buildDependencyGraph(String repositoryId, List<Path> filePaths) {
        if (graphCache.containsKey(repositoryId)) {
            log.debug("Returning cached graph for repository: {}", repositoryId);
            return graphCache.get(repositoryId);
        }

        log.debug("Building dependency graph for repository: {}", repositoryId);

        // Criar grafo direcionado
        Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        // Mapeamento de nome qualificado para nome simples
        Map<String, String> qualifiedToSimple = new HashMap<>();
        Map<String, String> simpleToQualified = new HashMap<>();

        // Primeira passagem: registrar todas as classes
        for (Path filePath : filePaths) {
            try {
                CodeAnalysisResult result = parserService.analyzeFile(filePath);

                for (ClassInfo classInfo : result.getClasses()) {
                    String qualified = classInfo.getQualifiedName();
                    String simple = classInfo.getSimpleName();

                    qualifiedToSimple.put(qualified, simple);
                    simpleToQualified.put(simple, qualified);

                    graph.addVertex(qualified);
                }
            } catch (Exception e) {
                log.debug("Failed to analyze file {}: {}", filePath, e.getMessage());
            }
        }

        // Segunda passagem: adicionar arestas de dependência
        for (Path filePath : filePaths) {
            try {
                CodeAnalysisResult result = parserService.analyzeFile(filePath);

                for (ClassInfo classInfo : result.getClasses()) {
                    String fromClass = classInfo.getQualifiedName();

                    for (String dependency : classInfo.getDependencies()) {
                        // Tentar resolver o nome qualificado da dependência
                        String toClass = resolveClassName(dependency, simpleToQualified);

                        if (toClass != null && graph.containsVertex(toClass)) {
                            graph.addEdge(fromClass, toClass);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to process dependencies for {}: {}", filePath, e.getMessage());
            }
        }

        // Cache do grafo
        graphCache.put(repositoryId, graph);

        log.debug("Built dependency graph: {} vertices, {} edges",
                graph.vertexSet().size(), graph.edgeSet().size());

        return graph;
    }

    /**
     * Resolve o nome qualificado de uma classe a partir de um nome simples.
     */
    private String resolveClassName(String className, Map<String, String> simpleToQualified) {
        // Se já é qualificado, retorna
        if (className.contains(".")) {
            return className;
        }

        // Tenta resolver usando o mapa
        String qualified = simpleToQualified.get(className);
        if (qualified != null) {
            return qualified;
        }

        // Se não encontrou e parece ser um tipo do java.lang/java.util, ignora
        if (isJavaStandardLibrary(className)) {
            return null;
        }

        // Não conseguiu resolver
        return null;
    }

    /**
     * Verifica se um nome de classe parece ser da biblioteca padrão Java.
     */
    private boolean isJavaStandardLibrary(String className) {
        String lower = className.toLowerCase();
        Set<String> standardClasses = Set.of("string", "integer", "long", "double",
                "float", "boolean", "object", "list", "arraylist", "map",
                "hashmap", "set", "hashset", "optional", "collection",
                "date", "localdate", "localdatetime", "timestamp");
        return standardClasses.contains(lower);
    }

    /**
     * Calcula o blast radius de uma mudança em uma classe.
     *
     * <p>Blast radius = número de classes que dependem direta ou indiretamente
     * da classe alterada.
     *
     * @param repositoryId ID do repositório
     * @param className    Nome qualificado da classe alterada
     * @return Número de classes afetadas
     */
    public int calculateBlastRadius(String repositoryId, String className) {
        Graph<String, DefaultEdge> graph = graphCache.get(repositoryId);
        if (graph == null) {
            log.debug("No graph found for repository: {}", repositoryId);
            return 0;
        }

        if (!graph.containsVertex(className)) {
            log.debug("Class {} not found in graph", className);
            return 0;
        }

        // Blast radius = número de classes que dependem desta (descendentes no grafo reverso)
        Set<String> affectedClasses = new HashSet<>();
        findDependents(graph, className, affectedClasses, new HashSet<>(), 5);

        return affectedClasses.size();
    }

    /**
     * Encontra todas as classes que dependem direta ou indiretamente de uma classe.
     *
     * @param graph           Grafo de dependências
     * @param className       Classe alvo
     * @param dependents      Acumulador de dependentes encontrados
     * @param visited         Classes já visitadas
     * @param maxDepth        Profundidade máxima de busca
     */
    private void findDependents(Graph<String, DefaultEdge> graph, String className,
                               Set<String> dependents, Set<String> visited, int maxDepth) {
        if (maxDepth <= 0 || visited.contains(className)) {
            return;
        }

        visited.add(className);

        // Encontrar todas as arestas que chegam nesta classe (classes que dependem dela)
        Set<DefaultEdge> incomingEdges = graph.incomingEdgesOf(className);

        for (DefaultEdge edge : incomingEdges) {
            String dependent = graph.getEdgeSource(edge);
            if (!dependents.contains(dependent)) {
                dependents.add(dependent);
                findDependents(graph, dependent, dependents, visited, maxDepth - 1);
            }
        }
    }

    /**
     * Retorna a lista de arquivos/classes afetados por uma mudança.
     *
     * @param repositoryId ID do repositório
     * @param className    Nome qualificado da classe alterada
     * @return Lista de classes afetadas
     */
    public List<String> getAffectedClasses(String repositoryId, String className) {
        Graph<String, DefaultEdge> graph = graphCache.get(repositoryId);
        if (graph == null) {
            return List.of();
        }

        Set<String> affectedClasses = new HashSet<>();
        findDependents(graph, className, affectedClasses, new HashSet<>(), 10);

        return new ArrayList<>(affectedClasses);
    }

    /**
     * Detecta ciclos nas dependências (circular dependencies).
     *
     * @param repositoryId ID do repositório
     * @return Lista de ciclos encontrados
     */
    public List<List<String>> detectCycles(String repositoryId) {
        Graph<String, DefaultEdge> graph = graphCache.get(repositoryId);
        if (graph == null) {
            return List.of();
        }

        CycleDetector<String, DefaultEdge> detector = new CycleDetector<>(graph);
        if (!detector.detectCycles()) {
            return List.of();
        }

        // Extrair ciclos
        Set<String> verticesInCycles = detector.findCycles();
        List<List<String>> cycles = new ArrayList<>();

        // Agrupar por ciclo conectado
        Set<String> processed = new HashSet<>();
        for (String vertex : verticesInCycles) {
            if (processed.contains(vertex)) {
                continue;
            }

            Set<String> cycle = detector.findCyclesContainingVertex(vertex);
            cycles.add(new ArrayList<>(cycle));
            processed.addAll(cycle);
        }

        return cycles;
    }

    /**
     * Encontra o caminho mais curto entre duas classes.
     *
     * @param repositoryId ID do repositório
     * @param fromClass    Classe de origem
     * @param toClass      Classe de destino
     * @return Caminho mais curto, ou lista vazia se não houver caminho
     */
    public List<String> findShortestPath(String repositoryId, String fromClass, String toClass) {
        Graph<String, DefaultEdge> graph = graphCache.get(repositoryId);
        if (graph == null) {
            return List.of();
        }

        DijkstraShortestPath<String, DefaultEdge> pathFinder =
                new DijkstraShortestPath<>(graph);

        var path = pathFinder.getPath(fromClass, toClass);
        if (path == null) {
            return List.of();
        }

        return path.getVertexList();
    }

    /**
     * Calcula a distância de dependência entre duas classes.
     *
     * @param repositoryId ID do repositório
     * @param fromClass    Classe de origem
     * @param toClass      Classe de destino
     * @return Número de arestas no caminho mais curto, ou -1 se não houver caminho
     */
    public int getDependencyDistance(String repositoryId, String fromClass, String toClass) {
        List<String> path = findShortestPath(repositoryId, fromClass, toClass);
        return path.isEmpty() ? -1 : path.size() - 1;
    }

    /**
     * Calcula métricas de acoplamento para uma classe.
     *
     * @param repositoryId ID do repositório
     * @param className    Nome qualificado da classe
     * @return Métricas: [afferent, efferent, instability]
     */
    public double[] calculateCouplingMetrics(String repositoryId, String className) {
        Graph<String, DefaultEdge> graph = graphCache.get(repositoryId);
        if (graph == null || !graph.containsVertex(className)) {
            return new double[]{0, 0, 0};
        }

        // Afferent coupling (Ca): quantas classes dependem desta
        int ca = graph.inDegreeOf(className);

        // Efferent coupling (Ce): quantas classes esta depende
        int ce = graph.outDegreeOf(className);

        // Instabilidade = Ce / (Ca + Ce)
        double instability = (ca + ce) > 0 ? (double) ce / (ca + ce) : 0;

        return new double[]{ca, ce, instability};
    }

    /**
     * Identifica classes "hub" - aquelas com muitas dependências (alto acoplamento).
     *
     * @param repositoryId ID do repositório
     * @param threshold    Limite para considerar como hub (padrão: 10)
     * @return Lista de classes hub ordenadas por grau de acoplamento
     */
    public List<HubClass> findHubClasses(String repositoryId, int threshold) {
        Graph<String, DefaultEdge> graph = graphCache.get(repositoryId);
        if (graph == null) {
            return List.of();
        }

        List<HubClass> hubs = new ArrayList<>();

        for (String vertex : graph.vertexSet()) {
            int inDegree = graph.inDegreeOf(vertex);
            int outDegree = graph.outDegreeOf(vertex);
            int totalDegree = inDegree + outDegree;

            if (totalDegree >= threshold) {
                double instability = (inDegree + outDegree) > 0
                        ? (double) outDegree / (inDegree + outDegree)
                        : 0;

                hubs.add(new HubClass(
                        vertex,
                        inDegree,
                        outDegree,
                        totalDegree,
                        instability
                ));
            }
        }

        // Ordenar por grau total decrescente
        hubs.sort((a, b) -> Integer.compare(b.totalDegree(), a.totalDegree()));

        return hubs;
    }

    /**
     * Limpa o cache de grafos.
     */
    public void clearCache() {
        graphCache.clear();
        log.debug("Dependency graph cache cleared");
    }

    /**
     * Limpa o cache para um repositório específico.
     */
    public void clearCacheForRepository(String repositoryId) {
        graphCache.remove(repositoryId);
        log.debug("Dependency graph cache cleared for repository: {}", repositoryId);
    }

    /**
     * Representa uma classe "hub" com alto acoplamento.
     */
    public record HubClass(
            String className,
            int afferentCoupling,    // Ca - quantas dependem desta
            int efferentCoupling,    // Ce - quantas esta depende
            int totalDegree,         // Ca + Ce
            double instability       // Ce / (Ca + Ce)
    ) {
        public String getSimpleName() {
            int lastDot = className.lastIndexOf('.');
            return lastDot > 0 ? className.substring(lastDot + 1) : className;
        }
    }
}
