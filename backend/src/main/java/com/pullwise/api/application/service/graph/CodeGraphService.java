package com.pullwise.api.application.service.graph;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

/**
 * Serviço unificado para análise de grafo de código.
 *
 * <p>Combina JavaParserService e DependencyGraphService para fornecer
 * análises completas de código e dependências.
 *
 * <p>Funcionalidades:
 * - Análise AST de código Java
 * - Construção de grafos de dependência
 * - Cálculo de blast radius
 * - Detecção de ciclos e dependências problemáticas
 * - Métricas de acoplamento e instabilidade
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGraphService {

    private final JavaParserService parserService;
    private final DependencyGraphService graphService;

    /**
     * Analisa um arquivo Java e retorna sua estrutura completa.
     *
     * @param filePath Caminho do arquivo .java
     * @return Resultado da análise com classes, métodos e dependências
     */
    public com.pullwise.api.application.service.graph.model.CodeAnalysisResult analyzeFile(Path filePath) {
        return parserService.analyzeFile(filePath);
    }

    /**
     * Constrói o grafo de dependências para um repositório.
     *
     * @param repositoryId ID do repositório
     * @param filePaths    Lista de arquivos .java
     * @return Grafo de dependências
     */
    public org.jgrapht.Graph<String, org.jgrapht.graph.DefaultEdge> buildDependencyGraph(
            String repositoryId, List<Path> filePaths) {
        return graphService.buildDependencyGraph(repositoryId, filePaths);
    }

    /**
     * Calcula o blast radius de uma mudança em um arquivo.
     *
     * @param repositoryId ID do repositório
     * @param filePath     Caminho do arquivo alterado
     * @return Número de arquivos/classes afetadas
     */
    public int calculateBlastRadius(String repositoryId, String filePath) {
        log.debug("Calculating blast radius for {}/{}", repositoryId, filePath);

        // Extrair nome da classe do path do arquivo
        String className = extractClassNameFromPath(filePath);

        if (className != null) {
            int blastRadius = graphService.calculateBlastRadius(repositoryId, className);
            if (blastRadius > 0) {
                return blastRadius;
            }
        }

        // Fallback para heurística baseada no nome do arquivo
        return calculateFallbackBlastRadius(filePath);
    }

    /**
     * Retorna lista de arquivos/classes afetados por uma mudança.
     *
     * @param repositoryId ID do repositório
     * @param filePath     Caminho do arquivo alterado
     * @return Lista de arquivos afetados
     */
    public List<String> getAffectedFiles(String repositoryId, String filePath) {
        log.debug("Getting affected files for {}/{}", repositoryId, filePath);

        String className = extractClassNameFromPath(filePath);
        if (className != null) {
            List<String> affectedClasses = graphService.getAffectedClasses(repositoryId, className);
            if (!affectedClasses.isEmpty()) {
                // Converter nomes qualificados de volta para paths
                return convertClassNamesToPaths(affectedClasses);
            }
        }

        return List.of();
    }

    /**
     * Analisa se uma mudança pode quebrar builds dependentes.
     *
     * @param repositoryId ID do repositório
     * @param filePath     Caminho do arquivo alterado
     * @return true se pode quebrar builds
     */
    public boolean canBreakDependentBuilds(String repositoryId, String filePath) {
        return calculateBlastRadius(repositoryId, filePath) > 10;
    }

    /**
     * Detecta dependências circulares no código.
     *
     * @param repositoryId ID do repositório
     * @return Lista de ciclos encontrados
     */
    public List<List<String>> detectCircularDependencies(String repositoryId) {
        return graphService.detectCycles(repositoryId);
    }

    /**
     * Identifica classes "hub" com alto acoplamento.
     *
     * @param repositoryId ID do repositório
     * @return Lista de classes hub ordenadas por acoplamento
     */
    public List<DependencyGraphService.HubClass> findHubClasses(String repositoryId) {
        return graphService.findHubClasses(repositoryId, 10);
    }

    /**
     * Calcula métricas de acoplamento para uma classe.
     *
     * @param repositoryId ID do repositório
     * @param className    Nome qualificado da classe
     * @return Métricas: [afferent, efferent, instability]
     */
    public double[] calculateCouplingMetrics(String repositoryId, String className) {
        return graphService.calculateCouplingMetrics(repositoryId, className);
    }

    /**
     * Encontra o caminho de dependência mais curto entre duas classes.
     *
     * @param repositoryId ID do repositório
     * @param fromClass    Classe de origem
     * @param toClass      Classe de destino
     * @return Caminho mais curto, ou lista vazia se não houver caminho
     */
    public List<String> findDependencyPath(String repositoryId, String fromClass, String toClass) {
        return graphService.findShortestPath(repositoryId, fromClass, toClass);
    }

    /**
     * Limpa os caches de análise.
     */
    public void clearCache() {
        parserService.clearCache();
        graphService.clearCache();
        log.debug("All code graph caches cleared");
    }

    // ========== Private Helper Methods ==========

    /**
     * Extrai o nome qualificado da classe a partir do path do arquivo.
     */
    private String extractClassNameFromPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        // Converter path para formato de classe Java
        // Ex: src/main/java/com/example/MyClass.java -> com.example.MyClass
        if (filePath.contains("/src/main/java/")) {
            String javaPath = filePath.substring(filePath.indexOf("/src/main/java/") + 16);
            if (javaPath.endsWith(".java")) {
                javaPath = javaPath.substring(0, javaPath.length() - 5);
            }
            return javaPath.replace("/", ".");
        } else if (filePath.contains(".java")) {
            // Fallback: remover .java e substituir / por .
            String className = filePath.substring(0, filePath.lastIndexOf(".java"));
            return className.replace("/", ".");
        }

        return null;
    }

    /**
     * Converte nomes qualificados de classes em paths de arquivos.
     */
    private List<String> convertClassNamesToPaths(List<String> classNames) {
        return classNames.stream()
                .map(className -> className.replace(".", "/") + ".java")
                .toList();
    }

    /**
     * Calcula blast radius usando heurística quando o grafo não está disponível.
     */
    private int calculateFallbackBlastRadius(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return 1;
        }

        String lower = filePath.toLowerCase();

        // Arquivos core afetam muitos outros
        if (lower.contains("common") || lower.contains("shared") ||
                lower.contains("base") || lower.contains("core")) {
            return 20;
        }

        // Services têm impacto médio-alto
        if (lower.contains("service") || lower.contains("business")) {
            return 10;
        }

        // Controllers têm impacto médio
        if (lower.contains("controller") || lower.contains("rest")) {
            return 5;
        }

        // Repositories têm impacto médio-baixo
        if (lower.contains("repository") || lower.contains("dao")) {
            return 3;
        }

        // Arquivos comuns afetam poucos
        return 2;
    }
}
