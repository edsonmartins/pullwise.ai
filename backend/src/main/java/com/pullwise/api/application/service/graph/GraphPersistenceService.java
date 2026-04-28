package com.pullwise.api.application.service.graph;

import com.pullwise.api.application.service.graph.blast.EdgeConfidenceClassifier;
import com.pullwise.api.application.service.graph.model.ClassInfo;
import com.pullwise.api.application.service.graph.model.CodeAnalysisResult;
import com.pullwise.api.application.service.graph.model.MethodInfo;
import com.pullwise.api.domain.enums.ConfidenceTier;
import com.pullwise.api.domain.enums.EdgeKind;
import com.pullwise.api.domain.enums.NodeKind;
import com.pullwise.api.domain.enums.ProgrammingLanguage;
import com.pullwise.api.domain.model.CodeGraphEdge;
import com.pullwise.api.domain.model.CodeGraphNode;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.repository.CodeGraphEdgeRepository;
import com.pullwise.api.domain.repository.CodeGraphNodeRepository;
import com.pullwise.api.domain.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persiste resultados de análise de código como nós e arestas no Code Graph v2.
 *
 * <p>Idempotente: rodar duas vezes a mesma análise gera o mesmo grafo (upsert via
 * unique constraints (project_id, qualified_name) e (project_id, source, target, kind)).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphPersistenceService {

    private final CodeGraphNodeRepository nodeRepository;
    private final CodeGraphEdgeRepository edgeRepository;
    private final ProjectRepository projectRepository;
    private final EdgeConfidenceClassifier confidenceClassifier;

    /**
     * Persiste o resultado de uma análise de arquivo no grafo.
     *
     * @param projectId  ID do projeto pullwise
     * @param result     resultado de {@link com.pullwise.api.application.service.graph.JavaParserService}
     * @return contador de nós e arestas processados
     */
    @Transactional
    public PersistenceStats upsertFromAnalysis(Long projectId, CodeAnalysisResult result) {
        if (result == null || result.getClasses() == null || result.getClasses().isEmpty()) {
            return PersistenceStats.empty();
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        ProgrammingLanguage language = detectLanguage(result.getFilePath());
        String fileQualified = result.getFilePath();

        int nodes = 0;
        int edges = 0;

        // 1) Nó FILE
        upsertNode(project, fileQualified, simpleFileName(result.getFilePath()),
                NodeKind.FILE, result.getFilePath(), language, null, null, false);
        nodes++;

        // 2) Para cada classe/interface/enum: nó + CONTAINS edge do FILE
        for (ClassInfo classInfo : result.getClasses()) {
            NodeKind classKind = classKindOf(classInfo);
            String classQn = classInfo.getQualifiedName();

            upsertNode(project, classQn, classInfo.getSimpleName(),
                    classKind, result.getFilePath(), language,
                    classInfo.getStartLine(), classInfo.getEndLine(),
                    isTestClass(classInfo));
            nodes++;

            upsertEdge(project, fileQualified, classQn, EdgeKind.CONTAINS,
                    ConfidenceTier.EXTRACTED, result.getFilePath(), classInfo.getStartLine());
            edges++;

            // 3) INHERITS / IMPLEMENTS
            if (classInfo.getSuperClass() != null && !classInfo.getSuperClass().isBlank()) {
                String targetQn = resolveQualified(classInfo.getSuperClass(), classInfo.getPackageName());
                ConfidenceTier tier = confidenceClassifier.classify(classInfo.getSuperClass(), targetQn);
                upsertEdge(project, classQn, targetQn, EdgeKind.INHERITS, tier,
                        result.getFilePath(), classInfo.getStartLine());
                edges++;
            }
            if (classInfo.getImplementedInterfaces() != null) {
                for (String iface : classInfo.getImplementedInterfaces()) {
                    String targetQn = resolveQualified(iface, classInfo.getPackageName());
                    ConfidenceTier tier = confidenceClassifier.classify(iface, targetQn);
                    upsertEdge(project, classQn, targetQn, EdgeKind.IMPLEMENTS, tier,
                            result.getFilePath(), classInfo.getStartLine());
                    edges++;
                }
            }

            // 4) IMPORTS_FROM via dependencies da classe
            Set<String> importTargets = new HashSet<>();
            if (classInfo.getDependencies() != null) {
                for (String dep : classInfo.getDependencies()) {
                    String targetQn = resolveQualified(dep, classInfo.getPackageName());
                    if (importTargets.add(targetQn)) {
                        ConfidenceTier tier = confidenceClassifier.classify(dep, targetQn);
                        upsertEdge(project, classQn, targetQn, EdgeKind.IMPORTS_FROM, tier,
                                result.getFilePath(), classInfo.getStartLine());
                        edges++;
                    }
                }
            }

            // 5) Métodos: nó + CONTAINS + CALLS
            if (classInfo.getMethods() != null) {
                for (MethodInfo method : classInfo.getMethods()) {
                    String methodQn = classQn + "#" + method.getName();
                    upsertNode(project, methodQn, method.getName(),
                            NodeKind.METHOD, result.getFilePath(), language,
                            method.getStartLine(), method.getEndLine(), false);
                    nodes++;

                    upsertEdge(project, classQn, methodQn, EdgeKind.CONTAINS,
                            ConfidenceTier.EXTRACTED, result.getFilePath(), method.getStartLine());
                    edges++;

                    if (method.getCalledClasses() != null) {
                        Set<String> seen = new HashSet<>();
                        for (String calledClass : method.getCalledClasses()) {
                            String targetQn = resolveQualified(calledClass, classInfo.getPackageName());
                            if (seen.add(targetQn)) {
                                ConfidenceTier tier = confidenceClassifier.classifyMethodCall(calledClass, targetQn);
                                upsertEdge(project, methodQn, targetQn, EdgeKind.CALLS, tier,
                                        result.getFilePath(), method.getStartLine());
                                edges++;
                            }
                        }
                    }
                }
            }
        }

        log.debug("Persisted graph for {}: {} nodes, {} edges", result.getFilePath(), nodes, edges);
        return new PersistenceStats(nodes, edges);
    }

    private void upsertNode(Project project, String qualifiedName, String simpleName,
                            NodeKind kind, String filePath, ProgrammingLanguage language,
                            Integer lineStart, Integer lineEnd, boolean isTest) {
        CodeGraphNode existing = nodeRepository
                .findByProjectIdAndQualifiedName(project.getId(), qualifiedName)
                .orElse(null);

        if (existing == null) {
            CodeGraphNode node = CodeGraphNode.builder()
                    .project(project)
                    .qualifiedName(qualifiedName)
                    .simpleName(simpleName)
                    .kind(kind)
                    .filePath(filePath)
                    .language(language)
                    .lineStart(lineStart)
                    .lineEnd(lineEnd)
                    .isTest(isTest)
                    .build();
            nodeRepository.save(node);
        } else {
            existing.setSimpleName(simpleName);
            existing.setKind(kind);
            existing.setFilePath(filePath);
            existing.setLanguage(language);
            existing.setLineStart(lineStart);
            existing.setLineEnd(lineEnd);
            existing.setIsTest(isTest);
            nodeRepository.save(existing);
        }
    }

    private void upsertEdge(Project project, String source, String target, EdgeKind kind,
                            ConfidenceTier tier, String sourceFilePath, Integer sourceLine) {
        CodeGraphEdge existing = edgeRepository
                .findByProjectIdAndSourceQualifiedAndTargetQualifiedAndKind(
                        project.getId(), source, target, kind)
                .orElse(null);

        if (existing == null) {
            CodeGraphEdge edge = CodeGraphEdge.builder()
                    .project(project)
                    .sourceQualified(source)
                    .targetQualified(target)
                    .kind(kind)
                    .confidenceTier(tier)
                    .confidenceWeight(tier.getWeight())
                    .sourceFilePath(sourceFilePath)
                    .sourceLine(sourceLine)
                    .build();
            edgeRepository.save(edge);
        } else if (existing.getConfidenceTier() != tier) {
            existing.setConfidenceTier(tier);
            existing.setConfidenceWeight(tier.getWeight());
            existing.setSourceFilePath(sourceFilePath);
            existing.setSourceLine(sourceLine);
            edgeRepository.save(existing);
        }
    }

    /**
     * Resolve um nome (simples ou já qualificado) para um qualified name canônico.
     * Heurística simples: se já contém '.', assume qualificado; senão prefixa com pacote.
     */
    private String resolveQualified(String name, String pkg) {
        if (name == null || name.isBlank()) return name;
        if (name.contains(".")) return name;
        if (pkg == null || pkg.isBlank()) return name;
        return pkg + "." + name;
    }

    private NodeKind classKindOf(ClassInfo info) {
        if (info.isInterface()) return NodeKind.INTERFACE;
        if (info.isEnum()) return NodeKind.ENUM;
        return NodeKind.CLASS;
    }

    private boolean isTestClass(ClassInfo info) {
        String name = info.getSimpleName() == null ? "" : info.getSimpleName().toLowerCase();
        return name.endsWith("test") || name.endsWith("tests") || name.endsWith("spec") || name.endsWith("it");
    }

    private ProgrammingLanguage detectLanguage(String filePath) {
        if (filePath == null) return ProgrammingLanguage.JAVA;
        String lower = filePath.toLowerCase();
        for (ProgrammingLanguage lang : ProgrammingLanguage.values()) {
            for (String ext : lang.getExtensions()) {
                if (lower.endsWith(ext)) return lang;
            }
        }
        return ProgrammingLanguage.JAVA;
    }

    private String simpleFileName(String filePath) {
        if (filePath == null) return "";
        int idx = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return idx >= 0 ? filePath.substring(idx + 1) : filePath;
    }

    /** Resultado da operação de upsert. */
    public record PersistenceStats(int nodesProcessed, int edgesProcessed) {
        public static PersistenceStats empty() {
            return new PersistenceStats(0, 0);
        }
    }
}
