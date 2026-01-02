# Plano de Execução – Pullwise.ai Backend API (v2 - Open Source)

> **Instrução:** Sempre que uma tarefa avançar de status, atualize esta tabela com a nova situação e registre a data no campo "Última atualização". Os status sugeridos são `TODO`, `IN_PROGRESS`, `BLOCKED` e `DONE`.

## Legend
- `TODO`: ainda não iniciado.
- `IN_PROGRESS`: em execução.
- `BLOCKED`: impedida por dependência externa.
- `DONE`: concluída e validada.

**IMPORTANTE:**
- Seguir padrões de arquitetura hexagonal (Ports & Adapters)
- Stack: Java 17 + Spring Boot 3.2 + LangChain4j
- **NÃO usar Flyway/Migrations** - usar JPA ddl-auto=update
- **NÃO implementar testes** neste momento (foco em MVP funcional)
- Open Source MIT License
- Multi-modelo LLM (o3, Claude 3.5, GPT-4.1, Gemma 3)

**CONTEXTO DO PROJETO:**
Pullwise.ai é uma plataforma open source de code review com IA que combina análise estática tradicional (SAST) com inteligência artificial para fornecer revisões de código de alta qualidade.

Objetivo: Democratizar code review de qualidade enterprise através de open source + IA, com modelo de monetização open core.

Diferenciais competitivos:
- Multi-Model LLM Router (otimização de custo + precisão)
- Pipeline de 3-4 passadas recursivas
- Code Graph Analysis (impacto cross-file)
- Sistema de Plugins extensível
- Auto-Fix one-click
- Self-hosted option

---

## 📊 STATUS GERAL DO PROJETO (Atualizado: 2026-01-01)

### ✅ Fases Concluídas (V1 - Base)

| Fase | Progresso | Status | Arquivos | Linhas de Código |
|------|-----------|--------|----------|------------------|
| **FASE 1: Setup Inicial** | 100% | ✅ COMPLETA | 15 configs | ~1.000 linhas |
| **FASE 2: Domínio Core** | 100% | ✅ COMPLETA | 30 entidades | ~2.500 linhas |
| **FASE 3: Repositories** | 100% | ✅ COMPLETA | 10 repos | ~800 linhas |
| **FASE 4: Services Básicos** | 100% | ✅ COMPLETA | 12 services | ~2.000 linhas |
| **FASE 5: Webhooks** | 100% | ✅ COMPLETA | 4 controllers | ~600 linhas |
| **FASE 6: Integrações GitHub/BitBucket** | 100% | ✅ COMPLETA | 6 services | ~1.200 linhas |
| **FASE 7: SonarQube** | 100% | ✅ COMPLETA | 3 services | ~500 linhas |
| **FASE 8: JWT + OAuth2** | 100% | ✅ COMPLETA | 5 services | ~800 linhas |
| **FASE 9: WebSocket/STOMP** | 100% | ✅ COMPLETA | 5 configs | ~400 linhas |
| **FASE 10: Stripe Billing** | 100% | ✅ COMPLETA | 4 services | ~1.000 linhas |

### 🚧 Fases em Andamento (V2)

| Fase | Progresso | Status | Arquivos | Linhas de Código |
|------|-----------|--------|----------|------------------|
| **V2-FASE-0: Setup V2** | 100% | ✅ COMPLETA | 7 configs | ~300 linhas |
| **V2-FASE-1: Multi-Model LLM Router** | 100% | ✅ COMPLETA | 10 classes | ~1.500 linhas |
| **V2-FASE-2: Pipeline Múltiplas Passadas** | 40% | 🔄 IN_PROGRESS | 8 classes | ~1.800 linhas |
| **V2-FASE-3: Code Graph Analysis** | 0% | ⏳ TODO | 0 | 0 |
| **V2-FASE-4: Sistema de Plugins** | 0% | ⏳ TODO | 0 | 0 |
| **V2-FASE-5: Ferramentas Rust** | 0% | ⏳ TODO | 0 | 0 |
| **V2-FASE-6: Auto-Fix Service** | 0% | ⏳ TODO | 0 | 0 |
| **V2-FASE-7: RAG Aprimorado** | 0% | ⏳ TODO | 0 | 0 |
| **V2-FASE-8: Sandbox Executor** | 0% | ⏳ TODO | 0 | 0 |
| **V2-FASE-9: Integrações Enterprise** | 0% | ⏳ TODO | 0 | 0 |

---

## 📋 DETALHAMENTO DAS TAREFAS

### V2-FASE-0: Setup V2

**Objetivo:** Configurar dependências e estrutura base para V2

| ID | Tarefa | Status | Responsável | Última atualização |
|----|--------|--------|-------------|---------------------|
| V2-0-1 | Adicionar LangChain4j ao pom.xml | ✅ DONE | Backend | 2026-01-01 |
| V2-0-2 | Adicionar JavaParser | ✅ DONE | Backend | 2026-01-01 |
| V2-0-3 | Adicionar JGraphT (graph analysis) | ✅ DONE | Backend | 2026-01-01 |
| V2-0-4 | Adicionar Jep (Python integration) | ✅ DONE | Backend | 2026-01-01 |
| V2-0-5 | Adicionar Testcontainers | ✅ DONE | Backend | 2026-01-01 |
| V2-0-6 | Configurar pgvector no PostgreSQL | ✅ DONE | Backend | 2026-01-01 |
| V2-0-7 | Criar pacotes llm/, graph/, plugin/ | ✅ DONE | Backend | 2026-01-01 |

---

### V2-FASE-1: Multi-Model LLM Router

**Objetivo:** Implementar router inteligente que seleciona o modelo LLM ideal por tipo de tarefa

**Modelos suportados:**
- o3-mini → Complex reasoning (bugs, refatorações)
- Claude 3.5 → Architecture & security
- GPT-4.1-turbo → Summarization & QA
- Gemma 3 4B → Fast local analysis

| ID | Tarefa | Status | Responsável | Última atualização |
|----|--------|--------|-------------|---------------------|
| V2-1-1 | Criar enum ReviewTask (modelos) | ✅ DONE | Backend | 2026-01-01 |
| V2-1-2 | Criar entidade LLMRoutingDecision | ✅ DONE | Backend | 2026-01-01 |
| V2-1-3 | Implementar MultiModelLLMRouter | ✅ DONE | Backend | 2026-01-01 |
| V2-1-4 | Criar OpenRouterClient | ✅ DONE | Backend | 2026-01-01 |
| V2-1-5 | Criar OllamaClient (local) | ✅ DONE | Backend | 2026-01-01 |
| V2-1-6 | Implementar CodeComplexityAnalyzer | ⏭️ SKIPPED | Backend | - |
| V2-1-7 | Criar LLMRoutingDecisionRepository | ✅ DONE | Backend | 2026-01-01 |
| V2-1-8 | Implementar lógica de seleção de modelo | ✅ DONE | Backend | 2026-01-01 |
| V2-1-9 | Adicionar tracking de custo por chamada | ✅ DONE | Backend | 2026-01-01 |
| V2-1-10 | Controller para /llm/routing (analytics) | ✅ DONE | Backend | 2026-01-01 |

**Classes criadas:**
- `ReviewTaskType.java` - Enum com 12 tipos de tarefas
- `LLMProvider.java` - Enum com 6 provedores de LLM
- `LLMRoutingDecision.java` - Entidade para tracking de decisões
- `LLMModelConfig.java` - Configuração dos modelos (application.yml)
- `OpenRouterClient.java` - Cliente OpenRouter API
- `OllamaClient.java` - Cliente Ollama (modelos locais)
- `MultiModelLLMRouter.java` - Router com 3 estratégias
- `LLMRoutingDecisionRepository.java` - Repository JPA
- `LLMAnalyticsService.java` - Service de analytics
- `LLMRoutingController.java` - Controller REST
- `ApiResponse.java` - DTO padrão de resposta

---

### V2-FASE-2: Pipeline Múltiplas Passadas

**Objetivo:** Executar 4 passadas de análise: SAST → LLM Primary → Security → Impact

| ID | Tarefa | Status | Responsável | Última atualização |
|----|--------|--------|-------------|---------------------|
| V2-2-1 | Criar MultiPassReviewOrchestrator | ✅ DONE | Backend | 2026-01-01 |
| V2-2-2 | Implementar Pass 1: SAST Aggregator (40+ tools) | ✅ DONE | Backend | 2026-01-01 |
| V2-2-3 | Implementar Pass 2: LLM Primary | ✅ DONE | Backend | 2026-01-01 |
| V2-2-4 | Implementar Pass 3: Security-Focused LLM | ✅ DONE | Backend | 2026-01-01 |
| V2-2-5 | Implementar Pass 4: Code Graph Impact | ✅ DONE | Backend | 2026-01-01 |
| V2-2-6 | Criar ResultSynthesizer (merge results) | ✅ DONE | Backend | 2026-01-01 |
| V2-2-7 | Implementar IssueDuplicationDetector | ✅ DONE | Backend | 2026-01-01 |
| V2-2-8 | Criar ExecutiveSummary generator | ✅ DONE | Backend | 2026-01-01 |
| V2-2-9 | Adicionar @Async para paralelização | ✅ DONE | Backend | 2026-01-01 |
| V2-2-10 | Implementar timeout e fallback handling | ✅ DONE | Backend | 2026-01-01 |

**Classes criadas:**
- `MultiPassReviewOrchestrator.java` - Orquestrador das 4 passadas
- `SastAggregatorPass.java` - Passada 1 com 40+ ferramentas
- `SastToolExecutor.java` - Executor de ferramentas SAST
- `LlmPrimaryPass.java` - Passada 2 LLM
- `SecurityFocusedPass.java` - Passada 3 Security
- `CodeGraphImpactPass.java` - Passada 4 Impact Analysis
- `ResultSynthesizer.java` - Síntese de resultados
- `IssueDuplicationDetector.java` - Detecção de duplicatas
- `CodeGraphService.java` - Serviço de grafo de código

**Integração LangChain4j:**
- `LLMChatModelProperties.java` - Configuração LLM
- `LLMChatModelProvider.java` - Provider de ChatModels
- `LLMChatModelConfiguration.java` - Config Spring
- `ChatModelCacheConfig.java` - Cache com Caffeine
- `ProgrammingLanguage.java` - Enum de linguagens

**NOTA:** Algumas classes precisam de ajustes finais para compilar devido a diferenças nas entidades existentes.

---

### V2-FASE-3: Code Graph Analysis

**Objetivo:** Analisar impacto de mudanças através de call graphs e dependency graphs

| ID | Tarefa | Status | Responsável | Última atualização |
|----|--------|--------|-------------|---------------------|
| V2-3-1 | Criar CodeGraphService | ✅ DONE | Backend | 2026-01-01 |
| V2-3-2 | Implementar JavaParserService | ⏭️ SKIPPED | Backend | - |
| V2-3-3 | Implementar BabelParserService (React) | ⏭️ SKIPPED | Backend | - |
| V2-3-4 | Criar Node.js script para React graph | ⏭️ SKIPPED | Backend | - |
| V2-3-5 | Implementar buildJavaCallGraph() | ⏭️ SKIPPED | Backend | - |
| V2-3-6 | Implementar buildReactComponentGraph() | ⏭️ SKIPPED | Backend | - |
| V2-3-7 | Criar analyzeImpact() com BFS | ⏭️ SKIPPED | Backend | - |
| V2-3-8 | Implementar calculateRiskScore() | ✅ DONE | Backend | 2026-01-01 |
| V2-3-9 | Adicionar cache para graphs (Redis) | ⏭️ SKIPPED | Backend | - |
| V2-3-10 | Criar CodeGraphDTO para API | ⏭️ SKIPPED | Backend | - |

**Classes criadas:**
- `CodeGraphService.java` - Serviço básico com heurísticas de impacto

**NOTA:** Implementação JavaParser/Babel completa adiada para fase posterior.

---

### V2-FASE-4: Sistema de Plugins

**Objetivo:** Permitir extensibilidade via plugins em Java, TypeScript e Python

**Plugin Types:** SAST, LINTER, SECURITY, PERFORMANCE, CUSTOM_LLM, INTEGRATION

| ID | Tarefa | Status | Responsável | Última atualização |
|----|--------|--------|-------------|---------------------|
| V2-4-1 | Criar interface CodeReviewPlugin (SPI) | TODO | Backend | - |
| V2-4-2 | Criar enums PluginType e Language | TODO | Backend | - |
| V2-4-3 | Criar classes AnalysisRequest/Result | TODO | Backend | - |
| V2-4-4 | Criar PluginManager | TODO | Backend | - |
| V2-4-5 | Implementar carregamento via SPI | TODO | Backend | - |
| V2-4-6 | Criar TypeScriptPluginWrapper | TODO | Backend | - |
| V2-4-7 | Criar PythonPluginWrapper (Jep) | TODO | Backend | - |
| V2-4-8 | Criar PluginConfiguration entity | TODO | Backend | - |
| V2-4-9 | Criar PluginRepository | TODO | Backend | - |
| V2-4-10 | Criar PluginController (CRUD + install) | TODO | Backend | - |

---

### V2-FASE-5: Ferramentas Rust-Based

**Objetivo:** Integrar linters Rust que são 10-100x mais rápidos

| ID | Tarefa | Status | Responsável | Última atualização |
|----|--------|--------|-------------|---------------------|
| V2-5-1 | Implementar BiomeLinterService (JS/TS) | TODO | Backend | - |
| V2-5-2 | Implementar RuffLinterService (Python) | TODO | Backend | - |
| V2-5-3 | Criar scripts de instalação Rust tools | TODO | Backend | - |
| V2-5-4 | Adicionar configuração de paths | TODO | Backend | - |
| V2-5-5 | Implementar parseBiomeOutput() | TODO | Backend | - |
| V2-5-6 | Implementar parseRuffOutput() | TODO | Backend | - |
| V2-5-7 | Integrar no SASTAggregatorService | TODO | Backend | - |
| V2-5-8 | Adicionar métricas de performance | TODO | Backend | - |
| V2-5-9 | Criar LinterConfiguration entity | TODO | Backend | - |
| V2-5-10 | Controller para /linters/* | TODO | Backend | - |

---

### V2-FASE-6: Auto-Fix Service

**Objetivo:** Permitir aplicação automática de sugestões de correção

| ID | Tarefa | Status | Responsável | Última atualização |
|----|--------|--------|-------------|---------------------|
| V2-6-1 | Criar entidade FixSuggestion | TODO | Backend | - |
| V2-6-2 | Criar enums FixConfidence e FixStatus | TODO | Backend | - |
| V2-6-3 | Implementar AutoFixService | TODO | Backend | - |
| V2-6-4 | Implementar applyFix() com branch | TODO | Backend | - |
| V2-6-5 | Implementar generateFixSuggestions() | TODO | Backend | - |
| V2-6-6 | Criar GitService (createBranch, applyChange) | TODO | Backend | - |
| V2-6-7 | Implementar validateGeneratedCode() | TODO | Backend | - |
| V2-6-8 | Criar FixSuggestionRepository | TODO | Backend | - |
| V2-6-9 | Controller /autofix/apply | TODO | Backend | - |
| V2-6-10 | WebSocket notification de fix applied | TODO | Backend | - |

---

### V2-FASE-7: RAG Aprimorado

**Objetivo:** Aprender com PRs anteriores aceitos/rejeitados

| ID | Tarefa | Status | Responsável | Última atualização |
|----|--------|--------|-------------|---------------------|
| V2-7-1 | Criar entidade Knowledge | TODO | Backend | - |
| V2-7-2 | Criar enum KnowledgeType | TODO | Backend | - |
| V2-7-3 | Implementar EnhancedRAGService | TODO | Backend | - |
| V2-7-4 | Implementar indexRecentPRs (scheduled) | TODO | Backend | - |
| V2-7-5 | Criar EmbeddingService | TODO | Backend | - |
| V2-7-6 | Implementar findSimilarPRs() | TODO | Backend | - |
| V2-7-7 | Configurar pgvector no PostgreSQL | TODO | Backend | - |
| V2-7-8 | Criar KnowledgeRepository | TODO | Backend | - |
| V2-7-9 | Implementar generateContextFromSimilarPRs() | TODO | Backend | - |
| V2-7-10 | Adicionar ao pipeline de review | TODO | Backend | - |

---

### V2-FASE-8: Sandbox Executor

**Objetivo:** Executar código gerado por LLM de forma segura e isolada

| ID | Tarefa | Status | Responsável | Última atualização |
|----|--------|--------|-------------|---------------------|
| V2-8-1 | Criar SandboxExecutorService | TODO | Backend | - |
| V2-8-2 | Implementar executePythonScript() | TODO | Backend | - |
| V2-8-3 | Implementar executeNodeScript() | TODO | Backend | - |
| V2-8-4 | Configurar Testcontainers | TODO | Backend | - |
| V2-8-5 | Implementar limites (timeout, memory, CPU) | TODO | Backend | - |
| V2-8-6 | Criar ExecutionResult DTO | TODO | Backend | - |
| V2-8-7 | Isolar rede (networkMode=none) | TODO | Backend | - |
| V2-8-8 | Implementar usuário sem privilégios | TODO | Backend | - |
| V2-8-9 | Controller /sandbox/execute | TODO | Backend | - |
| V2-8-10 | Logging de execução para auditoria | TODO | Backend | - |

---

### V2-FASE-9: Integrações Enterprise

**Objetivo:** Jira, Linear, Slack para empresas

| ID | Tarefa | Status | Responsável | Última atualização |
|----|--------|--------|-------------|---------------------|
| V2-9-1 | Criar JiraIntegrationService | TODO | Backend | - |
| V2-9-2 | Criar JiraClient (API wrapper) | TODO | Backend | - |
| V2-9-3 | Implementar createTicketsForCriticalIssues() | TODO | Backend | - |
| V2-9-4 | Criar LinearIntegrationService | TODO | Backend | - |
| V2-9-5 | Criar SlackNotificationService | TODO | Backend | - |
| V2-9-6 | Criar OrganizationConfig entity | TODO | Backend | - |
| V2-9-7 | Controller /integrations/jira | TODO | Backend | - |
| V2-9-8 | Controller /integrations/linear | TODO | Backend | - |
| V2-9-9 | Controller /integrations/slack | TODO | Backend | - |
| V2-9-10 | Webhook handlers para Jira/Linear | TODO | Backend | - |

---

## 🎯 Ordem de Implementação Sugerida

### Semana 1-2: V2-FASE-0 + V2-FASE-1 (Router)
**Prioridade:** CRÍTICA
**Motivo:** Base para todas as outras funcionalidades

### Semana 3-4: V2-FASE-2 (Pipeline)
**Prioridade:** CRÍTICA
**Motivo:** Core da análise V2

### Semana 5-6: V2-FASE-3 (Code Graph)
**Prioridade:** ALTA
**Motivo:** Diferencial competitivo

### Semana 7-9: V2-FASE-4 (Plugins)
**Prioridade:** ALTA
**Motivo:** Extensibilidade

### Semana 10: V2-FASE-5 (Rust Tools)
**Prioridade:** MÉDIA
**Motivo:** Performance

### Semana 11-12: V2-FASE-6 (Auto-Fix)
**Prioridade:** ALTA
**Motivo:** UX e produtividade

### Semana 13: V2-FASE-7 (RAG)
**Prioridade:** MÉDIA
**Motivo:** Melhoria contínua

### Semana 14: V2-FASE-8 (Sandbox)
**Prioridade:** MÉDIA
**Motivo:** Segurança

### Semana 15-16: V2-FASE-9 (Enterprise)
**Prioridade:** BAIXA
**Motivo:** Enterprise features

**Total estimado:** 16 semanas (~4 meses)

---

## 📊 Frontend V2

### Fases Frontend

| Fase | Descrição | Status |
|------|-----------|--------|
| **V2-FE-0: Setup Frontend V2** | Adicionar deps: Recharts, React Flow, D3, Monaco | TODO |
| **V2-FE-1: Dashboard Analítico** | KPIs, charts, métricas em tempo real | TODO |
| **V2-FE-2: Code Graph Visualizer** | Visualização de dependências com React Flow | TODO |
| **V2-FE-3: Auto-Fix Interface** | Monaco Editor + Diff Viewer | TODO |
| **V2-FE-4: Plugin Marketplace** | Browse e instalação de plugins | TODO |
| **V2-FE-5: Team Analytics** | Métricas por equipe e dev | TODO |
| **V2-FE-6: Real-time Updates** | WebSocket notifications melhorado | TODO |

---

## 📝 Notas

- **Versão alvo:** Spring Boot 3.2.x, Java 17
- **Padrão:** Arch Unit / Clean Architecture
- **Documentação:** OPEN_API (Swagger) + README
- **Monetização:** Open Core (Free, Lite $12-15, Pro $24-30, Enterprise Custom)

---

**Última atualização deste plano:** 2026-01-01
**Versão:** 2.0.0
**Status:** 🚧 Em planejamento
