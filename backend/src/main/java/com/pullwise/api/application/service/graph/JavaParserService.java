package com.pullwise.api.application.service.graph;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.pullwise.api.application.service.graph.model.ClassInfo;
import com.pullwise.api.application.service.graph.model.ClassInfo.ClassInfoBuilder;
import com.pullwise.api.application.service.graph.model.CodeAnalysisResult;
import com.pullwise.api.application.service.graph.model.MethodInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço para análise de código Java usando JavaParser.
 *
 * <p>Funcionalidades:
 * - Parse de arquivos Java para extrair estrutura de classes e métodos
 * - Análise de dependências entre classes
 * - Cálculo de complexidade ciclomática
 * - Detecção de métodos chamados (call graph)
 * - Cálculo de blast radius para mudanças
 *
 * <p>Usa JavaParser 3.25+ para análise AST.
 */
@Slf4j
@Service
public class JavaParserService {

    private final JavaParser parser = new JavaParser();

    // Cache de análises por arquivo
    private final Map<String, CodeAnalysisResult> analysisCache = new ConcurrentHashMap<>();

    /**
     * Analisa um arquivo Java e retorna sua estrutura completa.
     *
     * @param filePath Caminho do arquivo .java
     * @return Resultado da análise com classes, métodos e dependências
     */
    public CodeAnalysisResult analyzeFile(Path filePath) {
        return analyzeFile(filePath.toFile(), null);
    }

    /**
     * Analisa um arquivo Java e retorna sua estrutura completa.
     *
     * @param file        Arquivo .java
     * @param repositoryId ID do repositório (opcional)
     * @return Resultado da análise
     */
    public CodeAnalysisResult analyzeFile(File file, String repositoryId) {
        String cacheKey = file.getAbsolutePath() + ":" + repositoryId;
        if (analysisCache.containsKey(cacheKey)) {
            log.debug("Returning cached analysis for {}", file.getName());
            return analysisCache.get(cacheKey);
        }

        log.debug("Analyzing Java file: {}", file.getAbsolutePath());

        try {
            CompilationUnit cu = parser.parse(new FileInputStream(file)).getResult()
                    .orElseThrow(() -> new IOException("Failed to parse file"));

            // Extrair package
            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");

            // Analisar cada classe/interface/enum no arquivo
            List<ClassInfo> classes = new ArrayList<>();
            List<MethodInfo> allMethods = new ArrayList<>();

            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
                ClassInfo classInfo = analyzeClass(classDecl, packageName);
                classes.add(classInfo);
                allMethods.addAll(classInfo.getMethods());
            });

            cu.findAll(EnumDeclaration.class).forEach(enumDecl -> {
                ClassInfo enumInfo = analyzeEnum(enumDecl, packageName);
                classes.add(enumInfo);
                allMethods.addAll(enumInfo.getMethods());
            });

            // Calcular linhas de código
            int totalLines = countLines(file);
            int linesOfCode = countLinesOfCode(cu.toString());

            // Criar resultado
            CodeAnalysisResult result = CodeAnalysisResult.builder()
                    .filePath(file.getAbsolutePath())
                    .repositoryId(repositoryId)
                    .analyzedAt(LocalDateTime.now())
                    .classes(classes)
                    .allMethods(allMethods)
                    .totalLines(totalLines)
                    .linesOfCode(linesOfCode)
                    .fanIn(0)
                    .fanOut(0)
                    .instability(0)
                    .totalComplexity(0)
                    .averageComplexity(0)
                    .build();

            // Calcular métricas de dependência
            calculateDependencyMetrics(result);

            // Calcular complexidade
            calculateComplexityMetrics(result);

            // Cache do resultado
            analysisCache.put(cacheKey, result);

            log.debug("Analyzed {} classes with {} methods",
                    classes.size(), allMethods.size());

            return result;

        } catch (Exception e) {
            log.warn("Failed to analyze file {}: {}", file.getName(), e.getMessage());
            return createEmptyResult(file.getAbsolutePath(), repositoryId);
        }
    }

    /**
     * Analisa uma classe/interface e extrai suas informações.
     */
    private ClassInfo analyzeClass(ClassOrInterfaceDeclaration classDecl, String packageName) {
        // Coletar dependências
        List<String> dependencies = new ArrayList<>();

        // Super classe
        String superClass = null;
        for (ClassOrInterfaceType extType : classDecl.getExtendedTypes()) {
            superClass = extType.getNameAsString();
            dependencies.add(superClass);
        }

        // Interfaces implementadas
        List<String> interfaces = new ArrayList<>();
        for (ClassOrInterfaceType implType : classDecl.getImplementedTypes()) {
            String implName = implType.getNameAsString();
            interfaces.add(implName);
            dependencies.add(implName);
        }

        // Campos
        List<String> fields = new ArrayList<>();
        for (FieldDeclaration field : classDecl.getFields()) {
            for (VariableDeclarator var : field.getVariables()) {
                fields.add(var.getNameAsString());
                // Adicionar tipo do campo como dependência
                String typeName = var.getTypeAsString();
                if (isCustomType(typeName)) {
                    dependencies.add(typeName);
                }
            }
        }

        // Métodos
        List<MethodInfo> methods = new ArrayList<>();
        for (MethodDeclaration method : classDecl.getMethods()) {
            MethodInfo methodInfo = analyzeMethod(method, dependencies);
            methods.add(methodInfo);
        }

        return ClassInfo.builder()
                .simpleName(classDecl.getNameAsString())
                .packageName(packageName)
                .qualifiedName(packageName + "." + classDecl.getNameAsString())
                .isInterface(classDecl.isInterface())
                .isAbstract(classDecl.isAbstract())
                .startLine(classDecl.getBegin().map(pos -> pos.line).orElse(0))
                .endLine(classDecl.getEnd().map(pos -> pos.line).orElse(0))
                .superClass(superClass)
                .implementedInterfaces(interfaces)
                .dependencies(dependencies)
                .methods(methods)
                .fields(fields)
                .build();
    }

    /**
     * Analisa um enum e extrai suas informações.
     */
    private ClassInfo analyzeEnum(EnumDeclaration enumDecl, String packageName) {
        // Coletar dependências
        List<String> dependencies = new ArrayList<>();

        // Campos do enum (constantes)
        List<String> fields = new ArrayList<>();
        for (var entry : enumDecl.getEntries()) {
            fields.add(entry.getNameAsString());
        }

        // Métodos do enum
        List<MethodInfo> methods = new ArrayList<>();
        for (MethodDeclaration method : enumDecl.getMethods()) {
            MethodInfo methodInfo = analyzeMethod(method, dependencies);
            methods.add(methodInfo);
        }

        return ClassInfo.builder()
                .simpleName(enumDecl.getNameAsString())
                .packageName(packageName)
                .qualifiedName(packageName + "." + enumDecl.getNameAsString())
                .isEnum(true)
                .startLine(enumDecl.getBegin().map(pos -> pos.line).orElse(0))
                .endLine(enumDecl.getEnd().map(pos -> pos.line).orElse(0))
                .dependencies(dependencies)
                .methods(methods)
                .fields(fields)
                .build();
    }

    /**
     * Analisa um método e extrai suas informações.
     */
    private MethodInfo analyzeMethod(MethodDeclaration method, List<String> classDependencies) {
        Set<String> calledMethods = new HashSet<>();
        Set<String> calledClasses = new HashSet<>();

        // Visitor para encontrar chamadas de método
        method.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodCallExpr n, Void arg) {
                super.visit(n, arg);
                calledMethods.add(n.getNameAsString());

                // Tentar encontrar a classe do método chamado
                n.getScope().ifPresent(scope -> {
                    String scopeStr = scope.toString();
                    if (!scopeStr.equals("this") && !scopeStr.equals("super")) {
                        calledClasses.add(scopeStr);
                    }
                });
            }

            @Override
            public void visit(ObjectCreationExpr n, Void arg) {
                super.visit(n, arg);
                String typeName = n.getTypeAsString();
                calledClasses.add(typeName);
            }
        }, null);

        return MethodInfo.builder()
                .name(method.getNameAsString())
                .returnType(method.getTypeAsString())
                .parameterTypes(extractParameterTypes(method))
                .startLine(method.getBegin().map(pos -> pos.line).orElse(0))
                .endLine(method.getEnd().map(pos -> pos.line).orElse(0))
                .isStatic(method.isStatic())
                .isPublic(method.isPublic())
                .isOverride(method.getAnnotationByName("Override").isPresent())
                .calledMethods(new ArrayList<>(calledMethods))
                .calledClasses(new ArrayList<>(calledClasses))
                .complexity(calculateCyclomaticComplexity(method))
                .build();
    }

    /**
     * Extrai os tipos dos parâmetros de um método.
     */
    private List<String> extractParameterTypes(MethodDeclaration method) {
        List<String> paramTypes = new ArrayList<>();
        method.getParameters().forEach(param -> {
            paramTypes.add(param.getTypeAsString());
        });
        return paramTypes;
    }

    /**
     * Calcula a complexidade ciclomática de McCabe de um método.
     *
     * <p>Baseada no número de decisões:
     * if, for, while, case, catch, ?:, &&
     */
    private int calculateCyclomaticComplexity(MethodDeclaration method) {
        final int[] complexity = {1};  // Base complexity

        method.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(com.github.javaparser.ast.stmt.IfStmt n, Void arg) {
                super.visit(n, arg);
                complexity[0]++;
            }

            @Override
            public void visit(com.github.javaparser.ast.stmt.ForStmt n, Void arg) {
                super.visit(n, arg);
                complexity[0]++;
            }

            @Override
            public void visit(com.github.javaparser.ast.stmt.WhileStmt n, Void arg) {
                super.visit(n, arg);
                complexity[0]++;
            }

            @Override
            public void visit(com.github.javaparser.ast.stmt.DoStmt n, Void arg) {
                super.visit(n, arg);
                complexity[0]++;
            }

            @Override
            public void visit(com.github.javaparser.ast.stmt.ForEachStmt n, Void arg) {
                super.visit(n, arg);
                complexity[0]++;
            }

            @Override
            public void visit(com.github.javaparser.ast.stmt.SwitchStmt n, Void arg) {
                super.visit(n, arg);
                complexity[0] += n.getEntries().size();
            }

            @Override
            public void visit(com.github.javaparser.ast.stmt.CatchClause n, Void arg) {
                super.visit(n, arg);
                complexity[0]++;
            }

            @Override
            public void visit(com.github.javaparser.ast.expr.ConditionalExpr n, Void arg) {
                super.visit(n, arg);
                complexity[0]++;
            }

            @Override
            public void visit(com.github.javaparser.ast.expr.BinaryExpr n, Void arg) {
                super.visit(n, arg);
                if (n.getOperator() == com.github.javaparser.ast.expr.BinaryExpr.Operator.AND ||
                    n.getOperator() == com.github.javaparser.ast.expr.BinaryExpr.Operator.OR) {
                    complexity[0]++;
                }
            }
        }, null);

        return complexity[0];
    }

    /**
     * Calcula métricas de dependência para o resultado da análise.
     */
    private void calculateDependencyMetrics(CodeAnalysisResult result) {
        List<ClassInfo> classes = result.getClasses();
        if (classes.isEmpty()) {
            return;
        }

        // Calcular fanOut (dependências para fora)
        Set<String> allDependencies = new HashSet<>();
        for (ClassInfo classInfo : classes) {
            allDependencies.addAll(classInfo.getDependencies());
        }

        // Remover referências internas (do mesmo arquivo)
        Set<String> internalClasses = new HashSet<>();
        for (ClassInfo classInfo : classes) {
            internalClasses.add(classInfo.getSimpleName());
            internalClasses.add(classInfo.getQualifiedName());
        }
        allDependencies.removeAll(internalClasses);

        int fanOut = allDependencies.size();

        // FanIn requer análise global de todos os arquivos
        // Por ora, usamos uma estimativa baseada em heurísticas
        int fanIn = estimateFanIn(classes);

        result.setFanOut(fanOut);
        result.setFanIn(fanIn);

        // Instabilidade = fanOut / (fanIn + fanOut)
        int total = fanIn + fanOut;
        double instability = total > 0 ? (double) fanOut / total : 0;
        result.setInstability(instability);
    }

    /**
     * Estima o FanIn baseado em heurísticas.
     *
     * <p>Em produção, isso requer análise de todos os arquivos do projeto.
     */
    private int estimateFanIn(List<ClassInfo> classes) {
        int estimatedFanIn = 0;

        for (ClassInfo classInfo : classes) {
            // Classes com nomes sugestivos de "base", "util", "helper" tendem a ter alto FanIn
            String name = classInfo.getSimpleName().toLowerCase();
            if (name.contains("base") || name.contains("abstract") ||
                name.contains("util") || name.contains("helper") ||
                name.contains("service") || name.contains("manager")) {
                estimatedFanIn += 5;
            }
            // Interfaces tendem a ter mais dependentes
            if (classInfo.isInterface()) {
                estimatedFanIn += 3;
            }
        }

        return Math.max(1, estimatedFanIn);
    }

    /**
     * Calcula métricas de complexidade para o resultado.
     */
    private void calculateComplexityMetrics(CodeAnalysisResult result) {
        List<MethodInfo> methods = result.getAllMethods();
        if (methods.isEmpty()) {
            result.setTotalComplexity(0);
            result.setAverageComplexity(0);
            return;
        }

        int totalComplexity = methods.stream()
                .mapToInt(MethodInfo::getComplexity)
                .sum();

        double averageComplexity = (double) totalComplexity / methods.size();

        result.setTotalComplexity(totalComplexity);
        result.setAverageComplexity(averageComplexity);
    }

    /**
     * Conta o número total de linhas em um arquivo.
     */
    private int countLines(File file) {
        try {
            return (int) java.nio.file.Files.lines(file.toPath()).count();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Conta linhas de código efetivo (excluindo linhas em branco e comentários).
     */
    private int countLinesOfCode(String source) {
        return (int) source.lines()
                .filter(line -> !line.trim().isEmpty())
                .filter(line -> !line.trim().startsWith("//"))
                .filter(line -> !line.trim().startsWith("/*"))
                .filter(line -> !line.trim().startsWith("*"))
                .filter(line -> !line.trim().startsWith("import"))
                .filter(line -> !line.trim().startsWith("package"))
                .count();
    }

    /**
     * Retorna o tipo como string.
     */
    private String typeAsString(com.github.javaparser.ast.type.Type type) {
        return type.toString();
    }

    /**
     * Verifica se um tipo é custom (não é do Java padrão).
     */
    private boolean isCustomType(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return false;
        }

        // Tipos primitivos e java.lang não são custom
        String lower = typeName.toLowerCase();
        if (lower.equals("int") || lower.equals("long") || lower.equals("double") ||
            lower.equals("float") || lower.equals("boolean") || lower.equals("char") ||
            lower.equals("byte") || lower.equals("short") || lower.equals("void")) {
            return false;
        }

        // Tipos comuns do java.lang e java.util
        if (typeName.startsWith("java.lang.") || typeName.startsWith("java.util.")) {
            String simpleName = typeName.substring(typeName.lastIndexOf('.') + 1);
            return !Set.of("String", "Integer", "Long", "Double", "Float",
                    "Boolean", "Character", "Byte", "Short", "Object",
                    "List", "ArrayList", "Set", "HashSet", "Map",
                    "HashMap", "Optional", "Collection").contains(simpleName);
        }

        return true;
    }

    /**
     * Cria um resultado vazio para arquivos que não puderam ser analisados.
     */
    private CodeAnalysisResult createEmptyResult(String filePath, String repositoryId) {
        return CodeAnalysisResult.builder()
                .filePath(filePath)
                .repositoryId(repositoryId)
                .analyzedAt(LocalDateTime.now())
                .classes(Collections.emptyList())
                .allMethods(Collections.emptyList())
                .totalLines(0)
                .linesOfCode(0)
                .fanIn(0)
                .fanOut(0)
                .instability(0)
                .totalComplexity(0)
                .averageComplexity(0)
                .build();
    }

    /**
     * Limpa o cache de análises.
     */
    public void clearCache() {
        analysisCache.clear();
        log.debug("Analysis cache cleared");
    }

    /**
     * Limpa o cache para um arquivo específico.
     */
    public void clearCacheForFile(String filePath) {
        analysisCache.keySet().removeIf(key -> key.startsWith(filePath));
        log.debug("Analysis cache cleared for file: {}", filePath);
    }

    /**
     * Retorna o tamanho do cache de análises.
     */
    public int getCacheSize() {
        return analysisCache.size();
    }
}
