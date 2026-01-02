# Backend V2 - Extensões e Melhorias do SaaS de Code Review

## 📋 Visão Geral

Este documento **estende** o backend já em desenvolvimento com insights obtidos da análise de mercado (CodeRabbit, SonarQube, ferramentas modernas). As melhorias focam em:

1. **Arquitetura Multi-Modelo de LLMs** - Modelos especializados por tipo de tarefa
2. **Pipeline de Análise em Múltiplas Passadas** - 3-4 passadas recursivas
3. **Code Graph Analysis** - Análise de impacto cross-file
4. **Sistema de Plugins** - Extensibilidade para Java, TypeScript e Python
5. **Ferramentas Rust-Based** - Performance 10-100x superior
6. **Auto-Fix com One-Click** - Sugestões aplicáveis automaticamente
7. **RAG Aprimorado** - Aprendizado com PRs anteriores
8. **Sandbox Seguro** - Execução isolada de código gerado
9. **Integrações Enterprise** - Jira, Linear, Slack

---

## 🏗️ Arquitetura Estendida

```
┌────────────────────────────────────────────────────────────────┐
│                    ARQUITETURA BACKEND V2                       │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐      ┌──────────────────────────────────┐    │
│  │   Webhook   │─────▶│   Review Orchestrator            │    │
│  │   Handler   │      │  ┌────────────────────────────┐  │    │
│  └─────────────┘      │  │  Multi-Pass Pipeline       │  │    │
│         │             │  ├────────────────────────────┤  │    │
│         │             │  │ Pass 1: SAST (40+ tools)   │  │    │
│         ▼             │  │ Pass 2: LLM Primary        │  │    │
│  ┌─────────────┐      │  │ Pass 3: LLM Security       │  │    │
│  │  RabbitMQ   │      │  │ Pass 4: Synthesis          │  │    │
│  │   Queue     │      │  └────────────────────────────┘  │    │
│  └─────────────┘      └──────────────────────────────────┘    │
│         │                           │                          │
│         ▼                           ▼                          │
│  ┌─────────────────────────────────────────────────────┐      │
│  │           Plugin Manager (Java SPI)                  │      │
│  ├─────────────────────────────────────────────────────┤      │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐    │      │
│  │  │ Java       │  │TypeScript  │  │  Python    │    │      │
│  │  │ Plugins    │  │ Plugins    │  │  Plugins   │    │      │
│  │  │(SPI/JAR)   │  │(Node exec) │  │(Jep/Proc)  │    │      │
│  │  └────────────┘  └────────────┘  └────────────┘    │      │
│  └─────────────────────────────────────────────────────┘      │
│         │                                                      │
│         ▼                                                      │
│  ┌─────────────────────────────────────────────────────┐      │
│  │           Multi-Model LLM Router                     │      │
│  ├─────────────────────────────────────────────────────┤      │
│  │  • o3-mini      → Complex reasoning                 │      │
│  │  • Claude 3.5   → Architecture & security           │      │
│  │  • GPT-4.1      → Summarization & QA                │      │
│  │  • Gemma 3 4B   → Fast local analysis               │      │
│  └─────────────────────────────────────────────────────┘      │
│         │                                                      │
│         ▼                                                      │
│  ┌─────────────────────────────────────────────────────┐      │
│  │           Code Graph Analyzer                        │      │
│  ├─────────────────────────────────────────────────────┤      │
│  │  • JavaParser    → Java call graphs                 │      │
│  │  • Babel Parser  → React component graphs           │      │
│  │  • JGraphT       → Impact analysis                  │      │
│  └─────────────────────────────────────────────────────┘      │
│         │                                                      │
│         ▼                                                      │
│  ┌─────────────────────────────────────────────────────┐      │
│  │           Sandbox Executor                           │      │
│  ├─────────────────────────────────────────────────────┤      │
│  │  • Testcontainers → Ephemeral containers            │      │
│  │  • Script generation by LLM                         │      │
│  │  • Timeout & resource limits                        │      │
│  └─────────────────────────────────────────────────────┘      │
│         │                                                      │
│         ▼                                                      │
│  ┌─────────────────────────────────────────────────────┐      │
│  │           Enhanced RAG Knowledge Base                │      │
│  ├─────────────────────────────────────────────────────┤      │
│  │  • PostgreSQL + pgvector                            │      │
│  │  • Embeddings de PRs aceitos/rejeitados             │      │
│  │  • ADRs e documentação                              │      │
│  │  • Padrões customizados por equipe                  │      │
│  └─────────────────────────────────────────────────────┘      │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Novas Features Detalhadas

### 1. Multi-Model LLM Router

**Objetivo:** Usar modelos especializados para diferentes tipos de análise, otimizando custo e precisão.

#### 1.1 Modelo de Domínio

```java
package com.integralltech.codereview.llm;

public enum ReviewTask {
    COMPLEX_REASONING(
        "openai/o3-mini", 
        "Bugs multi-linha, refatorações complexas, análise de algoritmos",
        0.015 // custo por 1K tokens
    ),
    ARCHITECTURE_ANALYSIS(
        "anthropic/claude-3.5-sonnet", 
        "Padrões arquiteturais, dependências, design patterns",
        0.003
    ),
    SUMMARIZATION(
        "openai/gpt-4.1-turbo", 
        "Summaries, docstrings, changelog, QA básico",
        0.001
    ),
    FAST_LOCAL(
        "ollama/gemma-3-4b", 
        "Análise rápida de estilo e formatação",
        0.0 // local
    ),
    SECURITY_FOCUSED(
        "anthropic/claude-3.5-sonnet",
        "Vulnerabilidades, SQL injection, XSS, OWASP",
        0.003
    );

    private final String modelIdentifier;
    private final String description;
    private final double costPer1KTokens;
    
    // getters, constructor
}

@Entity
@Table(name = "llm_routing_decisions")
public class LLMRoutingDecision {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    private PullRequest pullRequest;
    
    @Enumerated(EnumType.STRING)
    private ReviewTask selectedTask;
    
    private String modelUsed;
    private Integer inputTokens;
    private Integer outputTokens;
    private Double cost;
    
    @Column(columnDefinition = "TEXT")
    private String reasoning; // Por que escolheu este modelo
    
    private LocalDateTime createdAt;
    
    // getters, setters
}
```

#### 1.2 Service de Roteamento Inteligente

```java
package com.integralltech.codereview.llm;

@Service
@Slf4j
public class MultiModelLLMRouter {
    
    private final OpenRouterClient openRouterClient;
    private final OllamaClient ollamaClient;
    private final CodeComplexityAnalyzer complexityAnalyzer;
    private final LLMRoutingDecisionRepository routingRepo;
    
    @Value("${llm.cost-optimization.enabled:true}")
    private boolean costOptimizationEnabled;
    
    @Value("${llm.local-first:true}")
    private boolean localFirst;
    
    public ReviewTask selectOptimalTask(CodeReviewContext context) {
        // Análise de complexidade
        int cyclomaticComplexity = complexityAnalyzer.calculate(context.getDiff());
        boolean hasComplexLogic = cyclomaticComplexity > 10;
        boolean isArchitecturalChange = context.hasArchitecturalChanges();
        boolean hasSecurityConcerns = context.hasSecurityPatterns();
        int linesChanged = context.getLinesChanged();
        
        String reasoning;
        ReviewTask selected;
        
        // Lógica de decisão
        if (hasSecurityConcerns) {
            selected = ReviewTask.SECURITY_FOCUSED;
            reasoning = "Detected security-sensitive patterns (SQL, auth, crypto)";
        } else if (hasComplexLogic) {
            selected = ReviewTask.COMPLEX_REASONING;
            reasoning = "High cyclomatic complexity: " + cyclomaticComplexity;
        } else if (isArchitecturalChange) {
            selected = ReviewTask.ARCHITECTURE_ANALYSIS;
            reasoning = "Changes affect core architecture or patterns";
        } else if (linesChanged < 100 && localFirst) {
            selected = ReviewTask.FAST_LOCAL;
            reasoning = "Small change, using local model for speed";
        } else {
            selected = ReviewTask.SUMMARIZATION;
            reasoning = "Standard code review, balanced approach";
        }
        
        log.info("Selected task: {} - Reason: {}", selected, reasoning);
        
        // Persistir decisão para analytics
        var decision = new LLMRoutingDecision();
        decision.setPullRequest(context.getPullRequest());
        decision.setSelectedTask(selected);
        decision.setModelUsed(selected.getModelIdentifier());
        decision.setReasoning(reasoning);
        routingRepo.save(decision);
        
        return selected;
    }
    
    public String executeReview(ReviewTask task, String prompt, CodeReviewContext context) {
        if (task == ReviewTask.FAST_LOCAL) {
            return ollamaClient.generate(task.getModelIdentifier(), prompt);
        } else {
            var response = openRouterClient.generate(
                task.getModelIdentifier(), 
                prompt,
                context.getMaxTokens()
            );
            
            // Atualizar custo
            updateCostMetrics(task, response, context);
            
            return response.getContent();
        }
    }
    
    private void updateCostMetrics(ReviewTask task, LLMResponse response, 
                                   CodeReviewContext context) {
        double cost = (response.getInputTokens() / 1000.0) * task.getCostPer1KTokens() +
                      (response.getOutputTokens() / 1000.0) * task.getCostPer1KTokens();
        
        var decision = routingRepo.findLatestByPullRequest(context.getPullRequest());
        decision.ifPresent(d -> {
            d.setInputTokens(response.getInputTokens());
            d.setOutputTokens(response.getOutputTokens());
            d.setCost(cost);
            routingRepo.save(d);
        });
    }
}
```

#### 1.3 Analyzer de Complexidade

```java
package com.integralltech.codereview.analysis;

@Service
public class CodeComplexityAnalyzer {
    
    private final JavaParserService javaParser;
    private final BabelParserService babelParser;
    
    public int calculate(String diff) {
        // Detectar linguagem
        Language lang = detectLanguage(diff);
        
        return switch (lang) {
            case JAVA -> calculateJavaComplexity(diff);
            case JAVASCRIPT, TYPESCRIPT, JSX -> calculateJSComplexity(diff);
            case PYTHON -> calculatePythonComplexity(diff);
            default -> 5; // complexidade média default
        };
    }
    
    private int calculateJavaComplexity(String code) {
        try {
            CompilationUnit cu = javaParser.parse(code);
            
            AtomicInteger complexity = new AtomicInteger(1); // Base complexity
            
            cu.findAll(IfStmt.class).forEach(ifStmt -> complexity.incrementAndGet());
            cu.findAll(ForStmt.class).forEach(forStmt -> complexity.incrementAndGet());
            cu.findAll(WhileStmt.class).forEach(whileStmt -> complexity.incrementAndGet());
            cu.findAll(CatchClause.class).forEach(catchClause -> complexity.incrementAndGet());
            cu.findAll(ConditionalExpr.class).forEach(ternary -> complexity.incrementAndGet());
            cu.findAll(SwitchEntry.class).forEach(caseStmt -> complexity.incrementAndGet());
            
            return complexity.get();
        } catch (Exception e) {
            log.warn("Failed to parse Java code for complexity", e);
            return 5;
        }
    }
    
    private int calculateJSComplexity(String code) {
        // Usar @babel/parser via Node.js subprocess
        ProcessBuilder pb = new ProcessBuilder(
            "node", 
            "/opt/scripts/calculate-complexity.js"
        );
        
        try {
            Process process = pb.start();
            process.getOutputStream().write(code.getBytes());
            process.getOutputStream().close();
            
            String output = new String(process.getInputStream().readAllBytes());
            return Integer.parseInt(output.trim());
        } catch (Exception e) {
            log.warn("Failed to calculate JS complexity", e);
            return 5;
        }
    }
    
    private Language detectLanguage(String diff) {
        if (diff.contains("public class") || diff.contains("import java.")) {
            return Language.JAVA;
        } else if (diff.contains("function") || diff.contains("const ") || diff.contains("=>")) {
            return Language.JAVASCRIPT;
        } else if (diff.contains("def ") || diff.contains("import ")) {
            return Language.PYTHON;
        }
        return Language.UNKNOWN;
    }
}
```

---

### 2. Pipeline de Múltiplas Passadas

**Objetivo:** Executar 3-4 passadas de análise, cada uma com foco específico, similar ao CodeRabbit.

#### 2.1 Orquestrador de Pipeline

```java
package com.integralltech.codereview.pipeline;

@Service
@Slf4j
public class MultiPassReviewOrchestrator {
    
    private final SASTAggregatorService sastService;
    private final MultiModelLLMRouter llmRouter;
    private final CodeGraphService codeGraphService;
    private final SandboxExecutorService sandboxService;
    private final ResultSynthesizer synthesizer;
    
    @Async("reviewExecutor")
    public CompletableFuture<ReviewResult> executeMultiPassReview(PullRequest pr) {
        log.info("Starting multi-pass review for PR #{}", pr.getNumber());
        
        var context = buildContext(pr);
        
        // ============ PASSADA 1: SAST (Paralelo) ============
        log.info("Pass 1: SAST Analysis");
        var sastFuture = executePass1SAST(pr);
        
        // ============ PASSADA 2: LLM Primary com contexto SAST ============
        var llmPrimaryFuture = sastFuture.thenCompose(sastResults -> {
            log.info("Pass 2: Primary LLM Analysis");
            return executePass2LLMPrimary(context, sastResults);
        });
        
        // ============ PASSADA 3: LLM Security Focused ============
        var securityFuture = llmPrimaryFuture.thenCompose(primaryResults -> {
            log.info("Pass 3: Security-Focused Analysis");
            return executePass3Security(context, primaryResults);
        });
        
        // ============ PASSADA 4: Code Graph Impact ============
        var impactFuture = CompletableFuture.supplyAsync(() -> {
            log.info("Pass 4: Code Graph Impact Analysis");
            return executePass4Impact(pr);
        });
        
        // ============ SÍNTESE FINAL ============
        return CompletableFuture.allOf(sastFuture, llmPrimaryFuture, securityFuture, impactFuture)
            .thenApply(v -> {
                log.info("Synthesizing final results");
                return synthesizer.merge(
                    sastFuture.join(),
                    llmPrimaryFuture.join(),
                    securityFuture.join(),
                    impactFuture.join()
                );
            });
    }
    
    private CompletableFuture<SASTResults> executePass1SAST(PullRequest pr) {
        // Executar ferramentas SAST em paralelo
        List<CompletableFuture<List<Issue>>> futures = List.of(
            CompletableFuture.supplyAsync(() -> sastService.runSonarQube(pr)),
            CompletableFuture.supplyAsync(() -> sastService.runCheckstyle(pr)),
            CompletableFuture.supplyAsync(() -> sastService.runPMD(pr)),
            CompletableFuture.supplyAsync(() -> sastService.runSpotBugs(pr)),
            CompletableFuture.supplyAsync(() -> sastService.runESLint(pr)),
            CompletableFuture.supplyAsync(() -> sastService.runBiome(pr)) // Rust-based
        );
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                var allIssues = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
                
                return new SASTResults(allIssues);
            });
    }
    
    private CompletableFuture<LLMReviewResults> executePass2LLMPrimary(
            CodeReviewContext context, SASTResults sastResults) {
        
        return CompletableFuture.supplyAsync(() -> {
            // Selecionar modelo apropriado
            var task = llmRouter.selectOptimalTask(context);
            
            // Construir prompt com contexto SAST
            String prompt = buildPromptWithSASTContext(context, sastResults, """
                Você está revisando um Pull Request. Ferramentas SAST já detectaram
                os seguintes issues (NÃO reporte novamente):
                
                %s
                
                Foque sua análise em:
                1. Lógica de negócio e correção algorítmica
                2. Padrões arquiteturais e design
                3. Legibilidade e manutenibilidade
                4. Issues que ferramentas estáticas não podem detectar
                
                Código modificado:
                %s
                """, sastResults.summarize(), context.getDiff());
            
            String llmResponse = llmRouter.executeReview(task, prompt, context);
            
            return parseLLMResponse(llmResponse);
        });
    }
    
    private CompletableFuture<SecurityReviewResults> executePass3Security(
            CodeReviewContext context, LLMReviewResults primaryResults) {
        
        return CompletableFuture.supplyAsync(() -> {
            // Sempre usar Claude para segurança (melhor em reasoning)
            String securityPrompt = """
                Foque EXCLUSIVAMENTE em análise de segurança profunda:
                
                1. Vulnerabilidades OWASP Top 10
                2. SQL Injection, XSS, CSRF
                3. Problemas de autenticação/autorização
                4. Exposição de dados sensíveis
                5. Criptografia inadequada
                6. Validação de entrada
                
                Issues já detectados (não reporte):
                %s
                
                Código:
                %s
                """.formatted(primaryResults.summarize(), context.getDiff());
            
            String response = llmRouter.executeReview(
                ReviewTask.SECURITY_FOCUSED, 
                securityPrompt, 
                context
            );
            
            return parseSecurityResponse(response);
        });
    }
    
    private ImpactAnalysisResults executePass4Impact(PullRequest pr) {
        // Análise de impacto usando code graph
        return codeGraphService.analyzeImpact(pr);
    }
    
    private CodeReviewContext buildContext(PullRequest pr) {
        return CodeReviewContext.builder()
            .pullRequest(pr)
            .diff(pr.getDiff())
            .repository(pr.getRepository())
            .changedFiles(pr.getChangedFiles())
            .build();
    }
}
```

#### 2.2 Result Synthesizer

```java
package com.integralltech.codereview.pipeline;

@Service
public class ResultSynthesizer {
    
    private final IssueDuplicationDetector duplicationDetector;
    
    public ReviewResult merge(SASTResults sast, 
                              LLMReviewResults llmPrimary,
                              SecurityReviewResults security,
                              ImpactAnalysisResults impact) {
        
        // Combinar todos os issues
        List<Issue> allIssues = new ArrayList<>();
        allIssues.addAll(sast.getIssues());
        allIssues.addAll(llmPrimary.getIssues());
        allIssues.addAll(security.getIssues());
        
        // Remover duplicatas
        List<Issue> deduplicatedIssues = duplicationDetector.removeDuplicates(allIssues);
        
        // Priorizar por severidade
        deduplicatedIssues.sort(Comparator
            .comparing(Issue::getSeverity).reversed()
            .thenComparing(Issue::getConfidence).reversed()
        );
        
        // Estatísticas
        var stats = ReviewStatistics.builder()
            .totalIssues(deduplicatedIssues.size())
            .criticalIssues(countBySeverity(deduplicatedIssues, Severity.CRITICAL))
            .highIssues(countBySeverity(deduplicatedIssues, Severity.HIGH))
            .mediumIssues(countBySeverity(deduplicatedIssues, Severity.MEDIUM))
            .lowIssues(countBySeverity(deduplicatedIssues, Severity.LOW))
            .sastIssues(sast.getIssues().size())
            .llmIssues(llmPrimary.getIssues().size())
            .securityIssues(security.getIssues().size())
            .impactedFiles(impact.getAffectedFiles().size())
            .build();
        
        // Summary executivo
        String executiveSummary = generateExecutiveSummary(
            deduplicatedIssues, stats, impact
        );
        
        return ReviewResult.builder()
            .issues(deduplicatedIssues)
            .statistics(stats)
            .executiveSummary(executiveSummary)
            .impactAnalysis(impact)
            .recommendations(generateRecommendations(deduplicatedIssues))
            .build();
    }
    
    private String generateExecutiveSummary(List<Issue> issues, 
                                           ReviewStatistics stats,
                                           ImpactAnalysisResults impact) {
        return """
            ## 📊 Review Summary
            
            **Total Issues Found:** %d
            - 🔴 Critical: %d
            - 🟠 High: %d
            - 🟡 Medium: %d
            - ⚪ Low: %d
            
            **Analysis Breakdown:**
            - SAST Tools: %d issues
            - AI Analysis: %d issues
            - Security Focus: %d issues
            
            **Impact Analysis:**
            - Files directly changed: %d
            - Files potentially affected: %d
            - Components impacted: %s
            
            **Recommendation:** %s
            """.formatted(
                stats.getTotalIssues(),
                stats.getCriticalIssues(),
                stats.getHighIssues(),
                stats.getMediumIssues(),
                stats.getLowIssues(),
                stats.getSastIssues(),
                stats.getLlmIssues(),
                stats.getSecurityIssues(),
                impact.getDirectlyChanged(),
                impact.getAffectedFiles().size(),
                String.join(", ", impact.getAffectedComponents()),
                getOverallRecommendation(stats)
            );
    }
    
    private String getOverallRecommendation(ReviewStatistics stats) {
        if (stats.getCriticalIssues() > 0) {
            return "❌ **BLOCK** - Critical issues must be resolved before merge";
        } else if (stats.getHighIssues() > 3) {
            return "⚠️ **REVIEW REQUIRED** - Multiple high-priority issues found";
        } else if (stats.getTotalIssues() > 10) {
            return "📝 **IMPROVEMENTS SUGGESTED** - Consider addressing issues";
        } else {
            return "✅ **APPROVED** - Code looks good";
        }
    }
}
```

---

### 3. Code Graph Analysis

**Objetivo:** Analisar impacto das mudanças através de call graphs e dependency graphs.

#### 3.1 Service de Code Graph

```java
package com.integralltech.codereview.graph;

@Service
@Slf4j
public class CodeGraphService {
    
    private final JavaParserService javaParser;
    private final BabelParserService reactParser;
    private final CacheManager cacheManager;
    
    @Cacheable(value = "code-graphs", key = "#repository.id")
    public DependencyGraph buildFullGraph(Repository repository) {
        log.info("Building code graph for repository: {}", repository.getName());
        
        var javaGraph = buildJavaCallGraph(repository);
        var reactGraph = buildReactComponentGraph(repository);
        
        return DependencyGraph.merge(javaGraph, reactGraph);
    }
    
    private DependencyGraph buildJavaCallGraph(Repository repo) {
        DirectedGraph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        
        // Parsear todos os arquivos Java
        repo.getJavaFiles().forEach(file -> {
            try {
                CompilationUnit cu = javaParser.parse(file.getContent());
                
                // Adicionar classes como vértices
                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
                    String className = cls.getFullyQualifiedName().orElse(cls.getNameAsString());
                    graph.addVertex(className);
                });
                
                // Adicionar chamadas como arestas
                cu.findAll(MethodCallExpr.class).forEach(call -> {
                    String caller = getCurrentClass(call);
                    String callee = call.resolve().getQualifiedName();
                    
                    if (graph.containsVertex(caller) && graph.containsVertex(callee)) {
                        graph.addEdge(caller, callee);
                    }
                });
                
            } catch (Exception e) {
                log.warn("Failed to parse file: {}", file.getPath(), e);
            }
        });
        
        return new DependencyGraph(graph, Language.JAVA);
    }
    
    private DependencyGraph buildReactComponentGraph(Repository repo) {
        // Usar @babel/parser via Node.js subprocess
        ProcessBuilder pb = new ProcessBuilder(
            "node",
            "/opt/scripts/build-react-graph.js",
            repo.getPath()
        );
        
        try {
            Process process = pb.start();
            String graphJson = new String(process.getInputStream().readAllBytes());
            
            return DependencyGraph.fromJson(graphJson, Language.JAVASCRIPT);
        } catch (Exception e) {
            log.error("Failed to build React component graph", e);
            return DependencyGraph.empty(Language.JAVASCRIPT);
        }
    }
    
    public ImpactAnalysisResults analyzeImpact(PullRequest pr) {
        var graph = buildFullGraph(pr.getRepository());
        var changedFiles = pr.getChangedFiles();
        
        // Calcular dependentes transitivos
        Set<String> directlyChanged = changedFiles.stream()
            .map(this::extractClassName)
            .collect(Collectors.toSet());
        
        Set<String> allAffected = new HashSet<>(directlyChanged);
        
        // BFS para encontrar todos os dependentes
        for (String changed : directlyChanged) {
            var dependents = graph.getTransitiveDependents(changed);
            allAffected.addAll(dependents);
        }
        
        // Identificar componentes/módulos afetados
        var affectedComponents = identifyAffectedComponents(allAffected, pr.getRepository());
        
        // Calcular métricas de risco
        double riskScore = calculateRiskScore(directlyChanged.size(), allAffected.size());
        
        return ImpactAnalysisResults.builder()
            .directlyChanged(directlyChanged.size())
            .affectedFiles(allAffected)
            .affectedComponents(affectedComponents)
            .riskScore(riskScore)
            .recommendation(getRiskRecommendation(riskScore))
            .build();
    }
    
    private double calculateRiskScore(int directChanges, int totalAffected) {
        // Score de 0-10 baseado no blast radius
        double blastRadius = (double) totalAffected / Math.max(directChanges, 1);
        
        if (blastRadius > 50) return 10.0; // Very high risk
        if (blastRadius > 20) return 7.5;  // High risk
        if (blastRadius > 10) return 5.0;  // Medium risk
        if (blastRadius > 5) return 2.5;   // Low-medium risk
        return 1.0;                         // Low risk
    }
    
    private Set<String> identifyAffectedComponents(Set<String> files, Repository repo) {
        // Mapear arquivos para componentes/módulos arquiteturais
        return files.stream()
            .map(file -> extractComponentName(file, repo))
            .collect(Collectors.toSet());
    }
}
```

#### 3.2 Script Node.js para React Graph

```javascript
// /opt/scripts/build-react-graph.js
const fs = require('fs');
const path = require('path');
const parser = require('@babel/parser');
const traverse = require('@babel/traverse').default;

function buildReactComponentGraph(repoPath) {
    const graph = { nodes: [], edges: [] };
    const components = new Map();
    
    // Encontrar todos os arquivos .jsx/.tsx
    const files = findReactFiles(repoPath);
    
    files.forEach(file => {
        const content = fs.readFileSync(file, 'utf-8');
        
        try {
            const ast = parser.parse(content, {
                sourceType: 'module',
                plugins: ['jsx', 'typescript']
            });
            
            let currentComponent = null;
            
            traverse(ast, {
                // Detectar definição de componente
                FunctionDeclaration(path) {
                    if (isReactComponent(path.node)) {
                        currentComponent = path.node.id.name;
                        components.set(currentComponent, { file, imports: [] });
                        graph.nodes.push({ id: currentComponent, file });
                    }
                },
                
                VariableDeclarator(path) {
                    if (isReactComponent(path.node.init)) {
                        currentComponent = path.node.id.name;
                        components.set(currentComponent, { file, imports: [] });
                        graph.nodes.push({ id: currentComponent, file });
                    }
                },
                
                // Detectar uso de outros componentes
                JSXElement(path) {
                    if (currentComponent) {
                        const componentName = path.node.openingElement.name.name;
                        if (components.has(componentName)) {
                            graph.edges.push({
                                from: currentComponent,
                                to: componentName
                            });
                        }
                    }
                }
            });
        } catch (e) {
            console.error(`Failed to parse ${file}:`, e.message);
        }
    });
    
    return JSON.stringify(graph, null, 2);
}

function isReactComponent(node) {
    // Heurística: função que retorna JSX
    if (!node) return false;
    
    // Arrow function ou function que retorna JSX
    return node.type === 'ArrowFunctionExpression' ||
           node.type === 'FunctionExpression' ||
           node.type === 'FunctionDeclaration';
}

function findReactFiles(dir) {
    let results = [];
    const files = fs.readdirSync(dir);
    
    files.forEach(file => {
        const filePath = path.join(dir, file);
        const stat = fs.statSync(filePath);
        
        if (stat.isDirectory() && !file.startsWith('.') && file !== 'node_modules') {
            results = results.concat(findReactFiles(filePath));
        } else if (file.endsWith('.jsx') || file.endsWith('.tsx')) {
            results.push(filePath);
        }
    });
    
    return results;
}

// Executar
const repoPath = process.argv[2];
console.log(buildReactComponentGraph(repoPath));
```

---

### 4. Sistema de Plugins

**Objetivo:** Permitir extensibilidade via plugins em Java, TypeScript e Python.

#### 4.1 Interface de Plugin (Java SPI)

```java
package com.integralltech.codereview.plugin.api;

public interface CodeReviewPlugin {
    
    /**
     * Identificador único do plugin
     */
    String getId();
    
    /**
     * Nome legível do plugin
     */
    String getName();
    
    /**
     * Versão do plugin
     */
    String getVersion();
    
    /**
     * Linguagens suportadas
     */
    Set<Language> getSupportedLanguages();
    
    /**
     * Tipo de análise (SAST, LINTER, SECURITY, etc)
     */
    PluginType getType();
    
    /**
     * Inicialização do plugin
     */
    void initialize(PluginContext context) throws PluginException;
    
    /**
     * Executar análise
     */
    AnalysisResult analyze(AnalysisRequest request) throws PluginException;
    
    /**
     * Shutdown do plugin
     */
    void shutdown();
    
    /**
     * Metadados de configuração
     */
    PluginMetadata getMetadata();
}

public enum PluginType {
    SAST,           // Análise estática
    LINTER,         // Code style
    SECURITY,       // Vulnerabilidades
    PERFORMANCE,    // Performance issues
    CUSTOM_LLM,     // LLM customizado
    INTEGRATION     // Integrações externas
}

@Data
@Builder
public class AnalysisRequest {
    private String diff;
    private List<String> changedFiles;
    private Repository repository;
    private PullRequest pullRequest;
    private Map<String, Object> configuration;
}

@Data
@Builder
public class AnalysisResult {
    private List<Issue> issues;
    private Map<String, Object> metadata;
    private Duration executionTime;
    private boolean success;
    private String errorMessage;
}
```

#### 4.2 Plugin Manager

```java
package com.integralltech.codereview.plugin;

@Service
@Slf4j
public class PluginManager {
    
    private final Map<String, CodeReviewPlugin> plugins = new ConcurrentHashMap<>();
    private final PluginConfigRepository configRepo;
    private final ServiceLoader<CodeReviewPlugin> serviceLoader;
    
    @PostConstruct
    public void loadPlugins() {
        log.info("Loading plugins...");
        
        // Carregar plugins Java (SPI)
        serviceLoader = ServiceLoader.load(CodeReviewPlugin.class);
        serviceLoader.forEach(this::registerPlugin);
        
        // Carregar plugins TypeScript
        loadTypeScriptPlugins();
        
        // Carregar plugins Python
        loadPythonPlugins();
        
        log.info("Loaded {} plugins", plugins.size());
    }
    
    private void registerPlugin(CodeReviewPlugin plugin) {
        try {
            var config = configRepo.findByPluginId(plugin.getId())
                .orElse(PluginConfiguration.defaultConfig(plugin.getId()));
            
            if (config.isEnabled()) {
                var context = PluginContext.builder()
                    .configuration(config.getSettings())
                    .dataDirectory(Paths.get("/var/lib/codereview/plugins", plugin.getId()))
                    .build();
                
                plugin.initialize(context);
                plugins.put(plugin.getId(), plugin);
                
                log.info("Registered plugin: {} v{}", plugin.getName(), plugin.getVersion());
            } else {
                log.info("Plugin {} is disabled", plugin.getId());
            }
        } catch (Exception e) {
            log.error("Failed to register plugin: {}", plugin.getId(), e);
        }
    }
    
    private void loadTypeScriptPlugins() {
        Path pluginDir = Paths.get("/opt/codereview/plugins/typescript");
        
        if (!Files.exists(pluginDir)) {
            return;
        }
        
        try (var stream = Files.list(pluginDir)) {
            stream.filter(Files::isDirectory)
                .forEach(dir -> {
                    Path mainFile = dir.resolve("index.js");
                    if (Files.exists(mainFile)) {
                        var plugin = new TypeScriptPluginWrapper(dir);
                        registerPlugin(plugin);
                    }
                });
        } catch (IOException e) {
            log.error("Failed to load TypeScript plugins", e);
        }
    }
    
    private void loadPythonPlugins() {
        Path pluginDir = Paths.get("/opt/codereview/plugins/python");
        
        if (!Files.exists(pluginDir)) {
            return;
        }
        
        try (var stream = Files.list(pluginDir)) {
            stream.filter(Files::isDirectory)
                .forEach(dir -> {
                    Path mainFile = dir.resolve("plugin.py");
                    if (Files.exists(mainFile)) {
                        var plugin = new PythonPluginWrapper(dir);
                        registerPlugin(plugin);
                    }
                });
        } catch (IOException e) {
            log.error("Failed to load Python plugins", e);
        }
    }
    
    public List<AnalysisResult> executePlugins(AnalysisRequest request, PluginType type) {
        return plugins.values().stream()
            .filter(p -> p.getType() == type)
            .filter(p -> supportsLanguage(p, request))
            .map(p -> executePluginSafely(p, request))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    private boolean supportsLanguage(CodeReviewPlugin plugin, AnalysisRequest request) {
        var languages = detectLanguages(request.getChangedFiles());
        return plugin.getSupportedLanguages().stream()
            .anyMatch(languages::contains);
    }
    
    private AnalysisResult executePluginSafely(CodeReviewPlugin plugin, AnalysisRequest request) {
        try {
            log.debug("Executing plugin: {}", plugin.getId());
            return plugin.analyze(request);
        } catch (Exception e) {
            log.error("Plugin execution failed: {}", plugin.getId(), e);
            return AnalysisResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }
}
```

#### 4.3 Wrapper para Plugins TypeScript

```java
package com.integralltech.codereview.plugin.typescript;

@Slf4j
public class TypeScriptPluginWrapper implements CodeReviewPlugin {
    
    private final Path pluginDirectory;
    private PluginMetadata metadata;
    
    public TypeScriptPluginWrapper(Path directory) {
        this.pluginDirectory = directory;
        this.metadata = loadMetadata();
    }
    
    @Override
    public void initialize(PluginContext context) throws PluginException {
        // Executar npm install se necessário
        Path nodeModules = pluginDirectory.resolve("node_modules");
        if (!Files.exists(nodeModules)) {
            executeNpmInstall();
        }
    }
    
    @Override
    public AnalysisResult analyze(AnalysisRequest request) throws PluginException {
        try {
            // Serializar request para JSON
            String requestJson = new ObjectMapper().writeValueAsString(request);
            
            // Executar plugin via Node.js
            ProcessBuilder pb = new ProcessBuilder(
                "node",
                pluginDirectory.resolve("index.js").toString()
            );
            
            pb.directory(pluginDirectory.toFile());
            
            Process process = pb.start();
            
            // Enviar request via stdin
            process.getOutputStream().write(requestJson.getBytes());
            process.getOutputStream().close();
            
            // Ler resultado via stdout
            String output = new String(process.getInputStream().readAllBytes());
            
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                String error = new String(process.getErrorStream().readAllBytes());
                throw new PluginException("Plugin failed: " + error);
            }
            
            // Parse resultado
            return new ObjectMapper().readValue(output, AnalysisResult.class);
            
        } catch (Exception e) {
            throw new PluginException("Failed to execute TypeScript plugin", e);
        }
    }
    
    private PluginMetadata loadMetadata() {
        try {
            Path packageJson = pluginDirectory.resolve("package.json");
            var json = new ObjectMapper().readTree(packageJson.toFile());
            
            return PluginMetadata.builder()
                .id(json.get("name").asText())
                .name(json.get("displayName").asText())
                .version(json.get("version").asText())
                .author(json.get("author").asText())
                .description(json.get("description").asText())
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load plugin metadata", e);
        }
    }
    
    private void executeNpmInstall() {
        try {
            ProcessBuilder pb = new ProcessBuilder("npm", "install");
            pb.directory(pluginDirectory.toFile());
            pb.inheritIO();
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                throw new RuntimeException("npm install failed");
            }
        } catch (Exception e) {
            log.error("Failed to run npm install", e);
        }
    }
    
    @Override
    public String getId() {
        return metadata.getId();
    }
    
    @Override
    public String getName() {
        return metadata.getName();
    }
    
    @Override
    public String getVersion() {
        return metadata.getVersion();
    }
    
    // Outros métodos da interface...
}
```

#### 4.4 Wrapper para Plugins Python

```java
package com.integralltech.codereview.plugin.python;

@Slf4j
public class PythonPluginWrapper implements CodeReviewPlugin {
    
    private final Path pluginDirectory;
    private PluginMetadata metadata;
    private SharedInterpreter interpreter;
    
    public PythonPluginWrapper(Path directory) {
        this.pluginDirectory = directory;
        this.metadata = loadMetadata();
    }
    
    @Override
    public void initialize(PluginContext context) throws PluginException {
        try {
            // Usar Jep (Java Embedded Python) para melhor performance
            JepConfig config = new JepConfig()
                .addIncludePaths(pluginDirectory.toString())
                .setRedirectOutputStreams(true);
            
            this.interpreter = new SharedInterpreter();
            interpreter.exec("import sys");
            interpreter.exec("sys.path.append('" + pluginDirectory + "')");
            interpreter.exec("import plugin");
            
        } catch (JepException e) {
            throw new PluginException("Failed to initialize Python plugin", e);
        }
    }
    
    @Override
    public AnalysisResult analyze(AnalysisRequest request) throws PluginException {
        try {
            // Converter request para dict Python
            interpreter.set("request_json", new ObjectMapper().writeValueAsString(request));
            interpreter.exec("import json");
            interpreter.exec("request = json.loads(request_json)");
            
            // Executar análise
            interpreter.exec("result = plugin.analyze(request)");
            
            // Obter resultado
            String resultJson = (String) interpreter.getValue("json.dumps(result)");
            
            return new ObjectMapper().readValue(resultJson, AnalysisResult.class);
            
        } catch (Exception e) {
            throw new PluginException("Failed to execute Python plugin", e);
        }
    }
    
    @Override
    public void shutdown() {
        if (interpreter != null) {
            try {
                interpreter.close();
            } catch (JepException e) {
                log.error("Failed to close Python interpreter", e);
            }
        }
    }
    
    private PluginMetadata loadMetadata() {
        // Ler plugin.yaml
        Path yamlFile = pluginDirectory.resolve("plugin.yaml");
        try {
            var yaml = new ObjectMapper(new YAMLFactory())
                .readValue(yamlFile.toFile(), Map.class);
            
            return PluginMetadata.builder()
                .id((String) yaml.get("id"))
                .name((String) yaml.get("name"))
                .version((String) yaml.get("version"))
                .author((String) yaml.get("author"))
                .description((String) yaml.get("description"))
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load plugin metadata", e);
        }
    }
    
    // Outros métodos...
}
```

---

### 5. Ferramentas Rust-Based (Performance)

**Objetivo:** Integrar linters Rust que são 10-100x mais rápidos.

#### 5.1 Service para Biome (JS/TS)

```java
package com.integralltech.codereview.linters;

@Service
@Slf4j
public class BiomeLinterService {
    
    @Value("${linters.biome.path:/usr/local/bin/biome}")
    private String biomePath;
    
    public List<Issue> lint(List<File> jsFiles) {
        if (jsFiles.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            List<String> command = new ArrayList<>();
            command.add(biomePath);
            command.add("check");
            command.add("--reporter=json");
            command.addAll(jsFiles.stream()
                .map(f -> f.getAbsolutePath())
                .collect(Collectors.toList()));
            
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            
            // Biome retorna exit code != 0 quando encontra issues
            if (exitCode != 0 && exitCode != 1) {
                String error = new String(process.getErrorStream().readAllBytes());
                log.error("Biome failed: {}", error);
                return Collections.emptyList();
            }
            
            return parseBiomeOutput(output);
            
        } catch (Exception e) {
            log.error("Failed to run Biome", e);
            return Collections.emptyList();
        }
    }
    
    private List<Issue> parseBiomeOutput(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            
            List<Issue> issues = new ArrayList<>();
            
            for (JsonNode diagnostic : root.get("diagnostics")) {
                Issue issue = Issue.builder()
                    .tool("biome")
                    .type(IssueType.STYLE)
                    .severity(mapSeverity(diagnostic.get("severity").asText()))
                    .title(diagnostic.get("message").asText())
                    .filePath(diagnostic.get("location").get("path").asText())
                    .lineStart(diagnostic.get("location").get("span").get("start").asInt())
                    .lineEnd(diagnostic.get("location").get("span").get("end").asInt())
                    .code(diagnostic.get("category").asText())
                    .build();
                
                issues.add(issue);
            }
            
            return issues;
            
        } catch (Exception e) {
            log.error("Failed to parse Biome output", e);
            return Collections.emptyList();
        }
    }
    
    private Severity mapSeverity(String biomeSeverity) {
        return switch (biomeSeverity.toLowerCase()) {
            case "error" -> Severity.HIGH;
            case "warning" -> Severity.MEDIUM;
            case "info" -> Severity.LOW;
            default -> Severity.INFO;
        };
    }
}
```

#### 5.2 Service para Ruff (Python)

```java
package com.integralltech.codereview.linters;

@Service
@Slf4j
public class RuffLinterService {
    
    @Value("${linters.ruff.path:/usr/local/bin/ruff}")
    private String ruffPath;
    
    public List<Issue> lint(List<File> pythonFiles) {
        if (pythonFiles.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            List<String> command = List.of(
                ruffPath,
                "check",
                "--output-format=json",
                pythonFiles.stream()
                    .map(f -> f.getAbsolutePath())
                    .collect(Collectors.joining(" "))
            );
            
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();
            
            return parseRuffOutput(output);
            
        } catch (Exception e) {
            log.error("Failed to run Ruff", e);
            return Collections.emptyList();
        }
    }
    
    private List<Issue> parseRuffOutput(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode violations = mapper.readTree(json);
            
            List<Issue> issues = new ArrayList<>();
            
            for (JsonNode violation : violations) {
                Issue issue = Issue.builder()
                    .tool("ruff")
                    .type(mapViolationType(violation.get("code").asText()))
                    .severity(mapRuffSeverity(violation.get("code").asText()))
                    .title(violation.get("message").asText())
                    .filePath(violation.get("filename").asText())
                    .lineStart(violation.get("location").get("row").asInt())
                    .columnStart(violation.get("location").get("column").asInt())
                    .code(violation.get("code").asText())
                    .fixAvailable(violation.has("fix"))
                    .build();
                
                if (violation.has("fix")) {
                    issue.setSuggestedFix(violation.get("fix").get("content").asText());
                }
                
                issues.add(issue);
            }
            
            return issues;
            
        } catch (Exception e) {
            log.error("Failed to parse Ruff output", e);
            return Collections.emptyList();
        }
    }
    
    private IssueType mapViolationType(String code) {
        if (code.startsWith("S")) return IssueType.SECURITY;
        if (code.startsWith("E") || code.startsWith("F")) return IssueType.BUG;
        if (code.startsWith("C") || code.startsWith("N")) return IssueType.STYLE;
        return IssueType.CODE_SMELL;
    }
    
    private Severity mapRuffSeverity(String code) {
        if (code.startsWith("S") || code.startsWith("F")) return Severity.HIGH;
        if (code.startsWith("E")) return Severity.MEDIUM;
        return Severity.LOW;
    }
}
```

---

### 6. Auto-Fix com One-Click

**Objetivo:** Permitir aplicação automática de sugestões de correção.

#### 6.1 Entities e DTOs

```java
package com.integralltech.codereview.autofix;

@Entity
@Table(name = "fix_suggestions")
public class FixSuggestion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "review_id")
    private CodeReview review;
    
    @ManyToOne
    @JoinColumn(name = "issue_id")
    private Issue issue;
    
    private String filePath;
    private Integer lineStart;
    private Integer lineEnd;
    
    @Column(columnDefinition = "TEXT")
    private String originalCode;
    
    @Column(columnDefinition = "TEXT")
    private String suggestedCode;
    
    @Column(columnDefinition = "TEXT")
    private String explanation;
    
    @Enumerated(EnumType.STRING)
    private FixConfidence confidence; // HIGH, MEDIUM, LOW
    
    private boolean autoApplicable;
    private boolean applied;
    
    private String appliedCommitSha;
    private LocalDateTime appliedAt;
    
    @Enumerated(EnumType.STRING)
    private FixStatus status; // PENDING, APPLIED, REJECTED, SUPERSEDED
    
    // getters, setters
}

public enum FixConfidence {
    HIGH,    // 95%+ confiança, pode aplicar automaticamente
    MEDIUM,  // 70-95%, requer confirmação
    LOW      // <70%, apenas sugestão
}

public enum FixStatus {
    PENDING,     // Aguardando ação
    APPLIED,     // Aplicado com sucesso
    REJECTED,    // Rejeitado pelo dev
    SUPERSEDED   // Substituído por outro fix
}
```

#### 6.2 Service de Auto-Fix

```java
package com.integralltech.codereview.autofix;

@Service
@Slf4j
public class AutoFixService {
    
    private final BitbucketService bitbucketService;
    private final GitService gitService;
    private final FixSuggestionRepository suggestionRepo;
    private final NotificationService notificationService;
    
    @Transactional
    public ApplyFixResult applyFix(UUID suggestionId, User user) {
        var suggestion = suggestionRepo.findById(suggestionId)
            .orElseThrow(() -> new NotFoundException("Suggestion not found"));
        
        if (suggestion.isApplied()) {
            throw new IllegalStateException("Fix already applied");
        }
        
        if (!suggestion.isAutoApplicable()) {
            throw new IllegalStateException("Fix is not auto-applicable");
        }
        
        try {
            // 1. Criar branch temporário
            String branchName = "coderabbit-fix-" + suggestionId.toString().substring(0, 8);
            var pr = suggestion.getReview().getPullRequest();
            
            gitService.createBranch(pr.getRepository(), branchName, pr.getTargetBranch());
            
            // 2. Aplicar mudança
            gitService.applyChange(
                pr.getRepository(),
                branchName,
                suggestion.getFilePath(),
                suggestion.getLineStart(),
                suggestion.getLineEnd(),
                suggestion.getSuggestedCode()
            );
            
            // 3. Commit
            String commitMessage = String.format(
                "🤖 Auto-fix: %s\n\nApplied by Pullwise.ai\nOriginal issue: %s",
                suggestion.getExplanation(),
                suggestion.getIssue().getTitle()
            );
            
            String commitSha = gitService.commit(
                pr.getRepository(),
                branchName,
                commitMessage,
                user.getEmail()
            );
            
            // 4. Comentar no PR
            bitbucketService.comment(pr, String.format(
                """
                ✅ **Auto-fix aplicado**
                
                **Issue resolvido:** %s
                **Arquivo:** `%s` (linhas %d-%d)
                **Commit:** `%s`
                **Branch:** `%s`
                
                **Explicação:**
                %s
                
                Para aceitar esta correção:
                ```bash
                git fetch origin %s
                git cherry-pick %s
                ```
                
                Para reverter:
                ```bash
                git revert %s
                ```
                """,
                suggestion.getIssue().getTitle(),
                suggestion.getFilePath(),
                suggestion.getLineStart(),
                suggestion.getLineEnd(),
                commitSha.substring(0, 8),
                branchName,
                suggestion.getExplanation(),
                branchName,
                commitSha,
                commitSha
            ));
            
            // 5. Atualizar suggestion
            suggestion.setApplied(true);
            suggestion.setAppliedCommitSha(commitSha);
            suggestion.setAppliedAt(LocalDateTime.now());
            suggestion.setStatus(FixStatus.APPLIED);
            suggestionRepo.save(suggestion);
            
            // 6. Notificar via Slack/email
            notificationService.notifyFixApplied(suggestion, user);
            
            return ApplyFixResult.success(commitSha, branchName);
            
        } catch (Exception e) {
            log.error("Failed to apply fix for suggestion: {}", suggestionId, e);
            return ApplyFixResult.failure(e.getMessage());
        }
    }
    
    @Transactional
    public void generateFixSuggestions(CodeReview review) {
        // Para cada issue de alta confiança, gerar sugestão de fix
        review.getIssues().stream()
            .filter(this::canGenerateFix)
            .forEach(issue -> generateFix(issue, review));
    }
    
    private boolean canGenerateFix(Issue issue) {
        // Apenas gerar fixes automáticos para tipos específicos
        return issue.getType() == IssueType.STYLE ||
               issue.getType() == IssueType.CODE_SMELL ||
               (issue.getType() == IssueType.BUG && issue.getSeverity() == Severity.LOW);
    }
    
    private void generateFix(Issue issue, CodeReview review) {
        try {
            // Usar LLM para gerar código corrigido
            String prompt = """
                Dado o seguinte issue de code review:
                
                **Issue:** %s
                **Arquivo:** %s
                **Linhas:** %d-%d
                
                **Código original:**
                ```
                %s
                ```
                
                Gere APENAS o código corrigido, sem explicações.
                O código deve ser sintaticamente correto e resolver o issue completamente.
                """.formatted(
                    issue.getDescription(),
                    issue.getFilePath(),
                    issue.getLineStart(),
                    issue.getLineEnd(),
                    extractOriginalCode(issue)
                );
            
            String suggestedCode = llmService.generate(
                "gpt-4.1-turbo", 
                prompt,
                2048
            );
            
            // Validar se o código é válido
            if (validateGeneratedCode(suggestedCode, issue.getFilePath())) {
                var suggestion = FixSuggestion.builder()
                    .review(review)
                    .issue(issue)
                    .filePath(issue.getFilePath())
                    .lineStart(issue.getLineStart())
                    .lineEnd(issue.getLineEnd())
                    .originalCode(extractOriginalCode(issue))
                    .suggestedCode(suggestedCode)
                    .explanation(issue.getDescription())
                    .confidence(calculateConfidence(issue))
                    .autoApplicable(isAutoApplicable(issue))
                    .status(FixStatus.PENDING)
                    .build();
                
                suggestionRepo.save(suggestion);
            }
            
        } catch (Exception e) {
            log.error("Failed to generate fix for issue: {}", issue.getId(), e);
        }
    }
    
    private FixConfidence calculateConfidence(Issue issue) {
        // Heurística baseada em tipo e ferramenta
        if (issue.getTool().equals("ruff") || issue.getTool().equals("biome")) {
            return FixConfidence.HIGH; // Ferramentas determinísticas
        }
        
        if (issue.getType() == IssueType.STYLE) {
            return FixConfidence.HIGH;
        }
        
        if (issue.getSeverity() == Severity.CRITICAL) {
            return FixConfidence.LOW; // Muito arriscado aplicar automaticamente
        }
        
        return FixConfidence.MEDIUM;
    }
    
    private boolean isAutoApplicable(Issue issue) {
        // Apenas style e code smells de baixa severidade
        return (issue.getType() == IssueType.STYLE) ||
               (issue.getType() == IssueType.CODE_SMELL && issue.getSeverity() == Severity.LOW);
    }
}
```

---

### 7. RAG Knowledge Base Aprimorado

**Objetivo:** Aprender com PRs anteriores aceitos/rejeitados.

#### 7.1 Enhanced RAG Service

```java
package com.integralltech.codereview.rag;

@Service
@Slf4j
public class EnhancedRAGService {
    
    private final KnowledgeRepository knowledgeRepo;
    private final EmbeddingService embeddingService;
    private final PullRequestRepository prRepo;
    
    @Scheduled(cron = "0 0 * * * *") // A cada hora
    public void indexRecentPRs() {
        log.info("Indexing recently merged PRs");
        
        var recentPRs = prRepo.findMergedSince(
            LocalDateTime.now().minusDays(30)
        );
        
        recentPRs.forEach(this::indexPR);
    }
    
    private void indexPR(PullRequest pr) {
        try {
            // Extrair padrões aceitos
            var patterns = extractAcceptedPatterns(pr);
            
            // Criar texto para embedding
            String content = String.format(
                """
                Title: %s
                Description: %s
                Changed Files: %s
                Diff Summary: %s
                Review Comments: %s
                Labels: %s
                """,
                pr.getTitle(),
                pr.getDescription(),
                String.join(", ", pr.getChangedFiles()),
                summarizeDiff(pr.getDiff()),
                summarizeComments(pr.getComments()),
                String.join(", ", pr.getLabels())
            );
            
            // Gerar embedding
            float[] embedding = embeddingService.embed(content);
            
            // Salvar no banco
            var knowledge = Knowledge.builder()
                .type(KnowledgeType.ACCEPTED_PR)
                .sourceType("pull_request")
                .sourceId(pr.getId().toString())
                .content(content)
                .embedding(embedding)
                .patterns(patterns)
                .metadata(Map.of(
                    "repository", pr.getRepository().getName(),
                    "author", pr.getAuthor().getUsername(),
                    "reviewers", pr.getReviewers().stream()
                        .map(User::getUsername)
                        .collect(Collectors.toList()),
                    "labels", pr.getLabels(),
                    "linesAdded", pr.getLinesAdded(),
                    "linesRemoved", pr.getLinesRemoved(),
                    "filesChanged", pr.getChangedFiles().size()
                ))
                .createdAt(LocalDateTime.now())
                .build();
            
            knowledgeRepo.save(knowledge);
            
        } catch (Exception e) {
            log.error("Failed to index PR: {}", pr.getId(), e);
        }
    }
    
    public List<Knowledge> findSimilarPRs(PullRequest currentPR, int limit) {
        String queryText = String.format(
            "%s %s",
            currentPR.getTitle(),
            summarizeDiff(currentPR.getDiff())
        );
        
        float[] embedding = embeddingService.embed(queryText);
        
        // Busca vetorial via pgvector
        return knowledgeRepo.findBySimilarity(
            embedding,
            0.75, // threshold de similaridade
            limit
        );
    }
    
    public String generateContextFromSimilarPRs(PullRequest pr) {
        var similarPRs = findSimilarPRs(pr, 5);
        
        if (similarPRs.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("## 📚 Contexto de PRs Similares\n\n");
        context.append("Encontramos PRs anteriores similares que podem ser relevantes:\n\n");
        
        for (Knowledge knowledge : similarPRs) {
            context.append(String.format(
                """
                **PR #%s** (Similaridade: %.0f%%)
                - Repositório: %s
                - Padrões observados: %s
                
                """,
                knowledge.getSourceId(),
                knowledge.getSimilarityScore() * 100,
                knowledge.getMetadata().get("repository"),
                String.join(", ", knowledge.getPatterns())
            ));
        }
        
        return context.toString();
    }
    
    private List<String> extractAcceptedPatterns(PullRequest pr) {
        List<String> patterns = new ArrayList<>();
        
        // Heurísticas para identificar padrões
        String diff = pr.getDiff().toLowerCase();
        
        if (diff.contains("@transactional")) {
            patterns.add("Uses transactional boundaries");
        }
        
        if (diff.contains("@cacheable")) {
            patterns.add("Implements caching");
        }
        
        if (diff.contains("try") && diff.contains("catch")) {
            patterns.add("Proper exception handling");
        }
        
        if (diff.contains("@test")) {
            patterns.add("Includes unit tests");
        }
        
        if (pr.getLabels().contains("breaking-change")) {
            patterns.add("Breaking change - requires migration");
        }
        
        // Analisar comentários de aprovação
        pr.getComments().stream()
            .filter(c -> c.getType() == CommentType.APPROVAL)
            .forEach(c -> {
                if (c.getContent().toLowerCase().contains("good test coverage")) {
                    patterns.add("Good test coverage");
                }
                if (c.getContent().toLowerCase().contains("well documented")) {
                    patterns.add("Well documented");
                }
            });
        
        return patterns;
    }
}
```

#### 7.2 Repository com pgvector

```java
package com.integralltech.codereview.rag;

public interface KnowledgeRepository extends JpaRepository<Knowledge, UUID> {
    
    @Query(value = """
        SELECT k.*, 
               1 - (k.embedding <=> :embedding::vector) as similarity_score
        FROM knowledge k
        WHERE 1 - (k.embedding <=> :embedding::vector) > :threshold
        ORDER BY k.embedding <=> :embedding::vector
        LIMIT :limit
        """, nativeQuery = true)
    List<Knowledge> findBySimilarity(
        @Param("embedding") float[] embedding,
        @Param("threshold") double threshold,
        @Param("limit") int limit
    );
    
    List<Knowledge> findByTypeAndCreatedAtAfter(
        KnowledgeType type, 
        LocalDateTime since
    );
}
```

---

### 8. Sandbox Executor

**Objetivo:** Executar código gerado por LLM de forma segura e isolada.

#### 8.1 Sandbox Service com Testcontainers

```java
package com.integralltech.codereview.sandbox;

@Service
@Slf4j
public class SandboxExecutorService {
    
    @Value("${sandbox.timeout:60}")
    private int timeoutSeconds;
    
    @Value("${sandbox.memory-limit:512m}")
    private String memoryLimit;
    
    public ExecutionResult executePythonScript(String script, Map<String, String> files) {
        try (var container = createPythonContainer(script, files)) {
            container.start();
            
            // Aguardar execução com timeout
            boolean finished = container.waitingFor(
                Wait.forLogMessage(".*EXECUTION_COMPLETE.*", 1)
            ).timeout(Duration.ofSeconds(timeoutSeconds))
                .waitUntilReady();
            
            if (!finished) {
                container.stop();
                return ExecutionResult.timeout();
            }
            
            // Capturar logs
            String stdout = container.getLogs(OutputFrame.OutputType.STDOUT);
            String stderr = container.getLogs(OutputFrame.OutputType.STDERR);
            
            var containerInfo = container.getCurrentContainerInfo();
            Long exitCode = containerInfo.getState().getExitCodeLong();
            
            return ExecutionResult.builder()
                .success(exitCode == 0)
                .exitCode(exitCode.intValue())
                .stdout(stdout)
                .stderr(stderr)
                .executionTime(getExecutionTime(container))
                .build();
            
        } catch (Exception e) {
            log.error("Sandbox execution failed", e);
            return ExecutionResult.error(e.getMessage());
        }
    }
    
    private GenericContainer<?> createPythonContainer(String script, Map<String, String> files) {
        var container = new GenericContainer<>("python:3.11-slim")
            .withCreateContainerCmdModifier(cmd -> cmd
                .withMemory(parseMemory(memoryLimit))
                .withCpuQuota(50000L) // 50% de 1 CPU
                .withNetworkMode("none") // Sem acesso à rede
                .withUser("nobody") // Usuário sem privilégios
            )
            .withCopyToContainer(
                Transferable.of(script),
                "/tmp/script.py"
            )
            .withCommand("sh", "-c", 
                "python /tmp/script.py && echo EXECUTION_COMPLETE"
            );
        
        // Copiar arquivos adicionais
        files.forEach((path, content) -> {
            container.withCopyToContainer(
                Transferable.of(content),
                path
            );
        });
        
        return container;
    }
    
    public ExecutionResult executeNodeScript(String script) {
        try (var container = new GenericContainer<>("node:18-alpine")
                .withCreateContainerCmdModifier(cmd -> cmd
                    .withMemory(parseMemory(memoryLimit))
                    .withNetworkMode("none")
                    .withUser("nobody")
                )
                .withCopyToContainer(
                    Transferable.of(script),
                    "/tmp/script.js"
                )
                .withCommand("node", "/tmp/script.js")) {
            
            container.start();
            
            String output = container.getLogs();
            Long exitCode = container.getCurrentContainerInfo()
                .getState()
                .getExitCodeLong();
            
            return ExecutionResult.builder()
                .success(exitCode == 0)
                .exitCode(exitCode.intValue())
                .stdout(output)
                .build();
            
        } catch (Exception e) {
            log.error("Failed to execute Node script in sandbox", e);
            return ExecutionResult.error(e.getMessage());
        }
    }
    
    private long parseMemory(String memory) {
        // Converter "512m" para bytes
        if (memory.endsWith("m") || memory.endsWith("M")) {
            return Long.parseLong(memory.substring(0, memory.length() - 1)) * 1024 * 1024;
        } else if (memory.endsWith("g") || memory.endsWith("G")) {
            return Long.parseLong(memory.substring(0, memory.length() - 1)) * 1024 * 1024 * 1024;
        }
        return Long.parseLong(memory);
    }
    
    private Duration getExecutionTime(GenericContainer<?> container) {
        var state = container.getCurrentContainerInfo().getState();
        var started = Instant.parse(state.getStartedAt());
        var finished = Instant.parse(state.getFinishedAt());
        return Duration.between(started, finished);
    }
}
```

---

### 9. Integrações Enterprise

#### 9.1 Jira Integration

```java
package com.integralltech.codereview.integration.jira;

@Service
@Slf4j
public class JiraIntegrationService {
    
    private final JiraClient jiraClient;
    private final OrganizationConfigRepository configRepo;
    
    public void createTicketsForCriticalIssues(ReviewResult review, PullRequest pr) {
        var orgConfig = configRepo.findByOrganization(pr.getOrganization());
        
        if (!orgConfig.isJiraEnabled()) {
            return;
        }
        
        var criticalIssues = review.getIssues().stream()
            .filter(i -> i.getSeverity() == Severity.CRITICAL)
            .filter(i -> shouldCreateTicket(i, orgConfig))
            .collect(Collectors.toList());
        
        for (Issue issue : criticalIssues) {
            try {
                var jiraIssue = JiraIssue.builder()
                    .project(orgConfig.getJiraProject())
                    .issueType("Bug")
                    .priority(mapPriority(issue.getSeverity()))
                    .summary(String.format("[Code Review] %s", issue.getTitle()))
                    .description(formatIssueForJira(issue, pr))
                    .labels(List.of("code-review", "automated", issue.getType().name().toLowerCase()))
                    .components(determineComponents(issue, pr))
                    .build();
                
                String ticketKey = jiraClient.createIssue(jiraIssue);
                
                log.info("Created Jira ticket {} for issue {}", ticketKey, issue.getId());
                
                // Comentar no PR
                bitbucketService.comment(pr, 
                    String.format("🎫 Jira ticket created: [%s](%s)", 
                        ticketKey, 
                        getJiraUrl(ticketKey, orgConfig)
                    )
                );
                
            } catch (Exception e) {
                log.error("Failed to create Jira ticket for issue: {}", issue.getId(), e);
            }
        }
    }
    
    private String formatIssueForJira(Issue issue, PullRequest pr) {
        return String.format("""
            h2. Issue Details
            
            *Pull Request:* [PR #%d|%s]
            *File:* {{%s}}
            *Lines:* %d-%d
            *Severity:* {color:red}%s{color}
            *Type:* %s
            
            h3. Description
            %s
            
            h3. Code Location
            {code:java}
            %s
            {code}
            
            h3. Suggested Fix
            %s
            
            ---
            _Generated by Pullwise.ai_
            """,
            pr.getNumber(),
            pr.getUrl(),
            issue.getFilePath(),
            issue.getLineStart(),
            issue.getLineEnd(),
            issue.getSeverity(),
            issue.getType(),
            issue.getDescription(),
            extractCodeSnippet(issue),
            issue.getSuggestedFix() != null ? issue.getSuggestedFix() : "N/A"
        );
    }
}
```

---

## 📊 Métricas e Observabilidade

### Métricas de Performance

```java
package com.integralltech.codereview.metrics;

@Service
public class ReviewMetricsService {
    
    private final MeterRegistry meterRegistry;
    
    public void recordReviewMetrics(ReviewResult result, Duration executionTime) {
        // Tempo de execução
        meterRegistry.timer("code.review.execution.time")
            .record(executionTime);
        
        // Issues encontrados
        meterRegistry.counter("code.review.issues.total")
            .increment(result.getStatistics().getTotalIssues());
        
        meterRegistry.counter("code.review.issues.critical")
            .increment(result.getStatistics().getCriticalIssues());
        
        // Custos de LLM
        result.getLlmCosts().forEach((model, cost) -> {
            meterRegistry.counter("code.review.llm.cost", "model", model)
                .increment(cost);
        });
        
        // Taxa de aprovação
        if (result.isApproved()) {
            meterRegistry.counter("code.review.approved").increment();
        } else {
            meterRegistry.counter("code.review.rejected").increment();
        }
    }
}
```

---

## 🚀 Próximos Passos

1. **Implementar Multi-Model Router** (1 semana)
2. **Pipeline de Múltiplas Passadas** (2 semanas)
3. **Code Graph Analysis** (2 semanas)
4. **Sistema de Plugins** (3 semanas)
5. **Ferramentas Rust** (1 semana)
6. **Auto-Fix** (2 semanas)
7. **RAG Aprimorado** (1 semana)
8. **Sandbox** (1 semana)
9. **Integrações Enterprise** (2 semanas)

**Total estimado:** ~15 semanas (~4 meses)

---

## 📦 Dependências Adicionais

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Code Analysis -->
    <dependency>
        <groupId>com.github.javaparser</groupId>
        <artifactId>javaparser-core</artifactId>
        <version>3.25.7</version>
    </dependency>
    
    <!-- Graph Analysis -->
    <dependency>
        <groupId>org.jgrapht</groupId>
        <artifactId>jgrapht-core</artifactId>
        <version>1.5.2</version>
    </dependency>
    
    <!-- Python Integration -->
    <dependency>
        <groupId>black.ninia</groupId>
        <artifactId>jep</artifactId>
        <version>4.1.1</version>
    </dependency>
    
    <!-- Testcontainers para Sandbox -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>1.19.3</version>
    </dependency>
    
    <!-- pgvector para RAG -->
    <dependency>
        <groupId>com.pgvector</groupId>
        <artifactId>pgvector</artifactId>
        <version>0.1.2</version>
    </dependency>
</dependencies>
```

Este documento serve como **guia de implementação** para as melhorias do backend. Cada seção pode ser implementada de forma incremental e independente.
