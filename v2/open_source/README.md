# Pullwise.ai - The Open Code Review Platform

## 📋 Overview

**Pullwise.ai** é a plataforma open source de code review com IA. Este repositório contém a documentação técnica, estratégica e de execução completa para transformar Pullwise.ai em líder de mercado.

> 🎯 **Missão:** Democratizar code review de qualidade enterprise através de open source + IA

**Por que Pullwise.ai?**
- ✅ **Open Source** - MIT license, comunidade first
- ✅ **AI-Powered** - Multi-modelo otimizado (GPT-4, Claude, Gemma)
- ✅ **Self-Hosted** - Compliance garantido, dados privados
- ✅ **Plugin System** - Extensível para qualquer necessidade

### 🎯 Principais Melhorias da V2

- ✅ **Multi-Model LLM Router** - Modelos especializados por tipo de tarefa (o3, Claude 3.5, GPT-4.1, Gemma 3)
- ✅ **Pipeline de Múltiplas Passadas** - 3-4 análises recursivas para maior precisão
- ✅ **Code Graph Analysis** - Análise de impacto através de dependency graphs
- ✅ **Sistema de Plugins** - Extensibilidade em Java, TypeScript e Python
- ✅ **Ferramentas Rust** - Biome, Ruff (10-100x mais rápidas)
- ✅ **Auto-Fix One-Click** - Aplicação automática de correções
- ✅ **RAG Aprimorado** - Aprendizado com PRs anteriores
- ✅ **Sandbox Seguro** - Execução isolada de código gerado
- ✅ **Integrações Enterprise** - Jira, Linear, Slack

---

## 📚 Documentação Completa

Este repositório contém **8 documentos principais** (~7,700 linhas) cobrindo todos os aspectos do projeto:

### 🎯 Estratégia e Negócios

📖 **[OPEN_SOURCE_STRATEGY.md](./docs/OPEN_SOURCE_STRATEGY.md)** (22KB)
- 7 modelos de monetização (projeção $14.5M ARR ano 3)
- Cases de sucesso: GitLab, Sentry, PostHog
- Roadmap de open sourcing completo
- Governança e licenciamento
- Community building e growth hacks

📖 **[COMPETITIVE_POSITIONING.md](./docs/COMPETITIVE_POSITIONING.md)** (18KB)
- Análise vs CodeRabbit, SonarQube, Semgrep
- 4 moats defensíveis únicos
- Estratégia de entrada por segmento
- Matriz comparativa de features
- Messaging por audience

📖 **[90_DAYS_EXECUTION_PLAN.md](./docs/90_DAYS_EXECUTION_PLAN.md)** (20KB)
- Plano semana-a-semana para launch
- Launch sequence (HN + Product Hunt)
- Budget $2,500, ROI 24x
- Checklists completos
- Métricas de sucesso

📖 **[PITCH_DECK_OUTLINE.md](./docs/PITCH_DECK_OUTLINE.md)** (17KB)
- Estrutura 15 slides para Série A
- Projeções financeiras 3 anos
- Análise competitiva profunda
- Case studies e unit economics
- Templates de apresentação

📖 **[BRAND_IDENTITY.md](./docs/BRAND_IDENTITY.md)** (15KB)
- Identidade visual completa
- Paleta de cores e tipografia
- Brand voice e tone guidelines
- Templates de comunicação
- Checklist de branding

---

### 🛠️ Implementação Técnica

📖 **[BACKEND_V2.md](./docs/BACKEND_V2.md)** (79KB - o mais completo!)
- Multi-Model LLM Router (código completo)
- Pipeline 3-4 passadas (implementação)
- Code Graph Analysis (JavaParser + Babel)
- Sistema de Plugins (SPI + wrappers)
- Ferramentas Rust (Biome, Ruff)
- Auto-Fix Service
- RAG aprimorado (pgvector)
- Sandbox Executor (Testcontainers)
- Integrações Enterprise (Jira, Linear)

📖 **[FRONTEND_V2.md](./docs/FRONTEND_V2.md)** (43KB)
- Dashboard Analítico (KPIs + charts)
- Code Graph Visualizer (React Flow + D3)
- Auto-Fix Interface (Monaco Editor)
- Plugin Marketplace (browse + install)
- Real-time Updates (WebSockets)
- Team Analytics Dashboard
- Advanced Filtering

📖 **[PLUGIN_ARCHITECTURE.md](./docs/PLUGIN_ARCHITECTURE.md)** (35KB)
- Guia completo plugins Java, TypeScript, Python
- Templates prontos para uso
- Exemplos práticos funcionais
- Marketplace e distribuição
- Governance e CLA

---

## 📊 Números Chave

### Projeção de Crescimento (Open Source)

```yaml
Ano 1 (2026):
  ARR: $600K
  Clientes: 100
  Instalações OSS: 10,000
  GitHub Stars: 5,000

Ano 2 (2027):
  ARR: $3.3M
  Clientes: 500
  Instalações OSS: 50,000
  Team: 25 pessoas

Ano 3 (2028):
  ARR: $14.5M
  Clientes: 2,000
  Instalações OSS: 100,000+
  Série A: $15-30M
```

### Fontes de Receita (7 Streams)

1. **Open Core Enterprise** - $1.2M-8M/ano
2. **Managed Cloud SaaS** - $180K-1.2M/ano
3. **Enterprise Support** - $800K-2M/ano
4. **Plugin Marketplace** - $216K-800K/ano (30% rev share)
5. **Training & Certification** - $350K-1M/ano
6. **Sponsored Features** - $300K-500K/ano
7. **White-Label Licensing** - $250K-1M/ano

---

Cobertura completa:
- Arquitetura Multi-Modelo de LLMs
- Pipeline de Análise em Múltiplas Passadas
- Code Graph Service (JavaParser + Babel)
- Sistema de Plugins (SPI + Wrappers)
- Ferramentas Rust-Based (Biome, Ruff)
- Auto-Fix Service
- RAG Knowledge Base Aprimorado
- Sandbox Executor (Testcontainers)
- Integrações Enterprise (Jira, Linear)

**Stack Técnico:**
- Java 17 + Spring Boot
- LangChain4j + LangGraph4j
- PostgreSQL + pgvector
- OpenRouter + Ollama
- JavaParser + JGraphT
- Testcontainers

---

### Frontend

📖 **[FRONTEND_V2.md](./docs/FRONTEND_V2.md)** - Extensões do Frontend

Cobertura completa:
- Dashboard Analítico com métricas em tempo real
- Code Graph Visualização (React Flow + D3.js)
- Auto-Fix Interface com Diff Viewer
- Plugin Marketplace
- Real-time Updates (WebSockets)
- Team Analytics Dashboard
- Advanced Filtering

**Stack Técnico:**
- React 18 + TypeScript
- Vite
- TanStack Query (React Query)
- Zustand (State Management)
- Recharts + React Flow + D3.js
- Monaco Editor
- Socket.io Client
- Tailwind CSS

---

### Arquitetura de Plugins

📖 **[PLUGIN_ARCHITECTURE.md](./docs/PLUGIN_ARCHITECTURE.md)** - Sistema de Plugins

Cobertura completa:
- Conceitos Fundamentais
- Plugin API (interfaces e contratos)
- Criando Plugins Java (SPI)
- Criando Plugins TypeScript (Node.js)
- Criando Plugins Python (Jep/subprocess)
- Distribuição e Marketplace
- Templates Prontos para Uso

**Tipos de Plugins Suportados:**
- SAST (Análise Estática)
- LINTER (Code Style)
- SECURITY (Vulnerabilidades)
- PERFORMANCE (Otimizações)
- CUSTOM_LLM (Modelos Customizados)
- INTEGRATION (Integrações Externas)

---

## 🏗️ Arquitetura Geral V2

```
┌─────────────────────────────────────────────────────────────┐
│                    ARQUITETURA COMPLETA V2                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────┐        ┌─────────────────┐             │
│  │   Frontend     │───────▶│    Backend      │             │
│  │   React +      │        │  Spring Boot +  │             │
│  │   WebSocket    │        │  LangChain4j    │             │
│  └────────────────┘        └─────────────────┘             │
│         │                           │                        │
│         │                           ▼                        │
│         │                  ┌─────────────────┐              │
│         │                  │ Multi-Pass      │              │
│         │                  │ Review Pipeline │              │
│         │                  └─────────────────┘              │
│         │                           │                        │
│         │                           ▼                        │
│         │         ┌─────────────────────────────────┐       │
│         │         │    Plugin Manager (SPI)         │       │
│         │         ├─────────────────────────────────┤       │
│         │         │  Java │ TypeScript │  Python    │       │
│         │         └─────────────────────────────────┘       │
│         │                           │                        │
│         │                           ▼                        │
│         │         ┌─────────────────────────────────┐       │
│         │         │   Multi-Model LLM Router        │       │
│         │         ├─────────────────────────────────┤       │
│         │         │ o3 │ Claude │ GPT-4.1 │ Gemma  │       │
│         │         └─────────────────────────────────┘       │
│         │                           │                        │
│         │                           ▼                        │
│         │         ┌─────────────────────────────────┐       │
│         │         │   Code Graph Analyzer           │       │
│         │         │  (JavaParser + Babel + JGraphT) │       │
│         │         └─────────────────────────────────┘       │
│         │                           │                        │
│         │                           ▼                        │
│         │         ┌─────────────────────────────────┐       │
│         │         │   Enhanced RAG (pgvector)       │       │
│         │         │   + PR Learning System          │       │
│         │         └─────────────────────────────────┘       │
│         │                           │                        │
│         │                           ▼                        │
│         └──────────────────▶ PostgreSQL + Redis             │
│                                                              │
│  External Integrations:                                     │
│  • GitHub/BitBucket (Webhooks + API)                        │
│  • Jira/Linear (Issue Tracking)                             │
│  • Slack (Notifications)                                    │
│  • OpenRouter (LLM API)                                     │
│  • Ollama (Local LLM)                                       │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 Workflow de Review V2

```
1. PR Criado
   ↓
2. Webhook recebido
   ↓
3. Enfileirado no RabbitMQ
   ↓
4. ============ PASSADA 1: SAST (Paralelo) ============
   • SonarQube
   • Checkstyle
   • PMD
   • SpotBugs
   • ESLint
   • Biome (Rust - 10x mais rápido)
   • Ruff (Rust - 100x mais rápido)
   ↓
5. ============ PASSADA 2: LLM Primary ============
   • Router seleciona modelo apropriado
   • Contexto enriquecido com SAST
   • Análise de lógica de negócio
   ↓
6. ============ PASSADA 3: Security Focus ============
   • Claude 3.5 Sonnet (melhor em security)
   • Análise profunda OWASP Top 10
   ↓
7. ============ PASSADA 4: Code Graph Impact ============
   • Análise de dependências
   • Cálculo de blast radius
   • Risk score
   ↓
8. Síntese de Resultados
   • Deduplicação
   • Priorização
   • Geração de Auto-Fixes
   ↓
9. Busca RAG por PRs Similares
   • Embeddings vetoriais
   • Padrões históricos
   ↓
10. Comentário no PR
   • Summary executivo
   • Issues inline
   • Auto-fix suggestions
   ↓
11. WebSocket Update para Frontend
   • Progresso em tempo real
   • Notificações
   ↓
12. (Opcional) Criação de Tickets Jira
   • Para issues CRITICAL
```

---

## 📊 Comparação: V1 vs V2

| Aspecto | V1 (Original) | V2 (Atual) |
|---------|---------------|------------|
| **Modelos LLM** | OpenRouter genérico | Multi-modelo especializado |
| **Análise** | 1 passada | 3-4 passadas recursivas |
| **Ferramentas** | ~6 tools | 40+ tools (incluindo Rust) |
| **Escopo** | Apenas diff | Repositório completo |
| **Scripts** | Fixos | Geração dinâmica por IA |
| **Correção** | Manual | One-click auto-fix |
| **Aprendizado** | RAG básico | RAG + feedback loop |
| **Segurança** | Processo direto | Sandbox com Testcontainers |
| **Extensibilidade** | Fixo | Sistema de plugins |
| **Integrações** | Git básico | Jira, Linear, Slack |
| **Visualização** | Lista de issues | Code graph + analytics |
| **Tempo Real** | Polling | WebSockets |

---

## 🚀 Roadmap de Implementação

### Fase 1 - Core Enhancements (8 semanas)

**Backend (6 semanas):**
- ✅ Multi-Model Router (1 semana)
- ✅ Pipeline Múltiplas Passadas (2 semanas)
- ✅ Code Graph Analysis (2 semanas)
- ✅ RAG Aprimorado (1 semana)

**Frontend (2 semanas):**
- ✅ Dashboard Analítico (1 semana)
- ✅ Real-time Updates (1 semana)

### Fase 2 - Advanced Features (6 semanas)

**Backend (4 semanas):**
- ✅ Sistema de Plugins (3 semanas)
- ✅ Auto-Fix Service (2 semanas)
- ✅ Sandbox Executor (1 semana)

**Frontend (2 semanas):**
- ✅ Code Graph Visualizer (1 semana)
- ✅ Auto-Fix Interface (1 semana)

### Fase 3 - Enterprise & Polish (4 semanas)

**Backend (2 semanas):**
- ✅ Integrações Enterprise (1 semana)
- ✅ Ferramentas Rust (1 semana)

**Frontend (2 semanas):**
- ✅ Plugin Marketplace (1 semana)
- ✅ Team Analytics (1 semana)

**Total:** 18 semanas (~4.5 meses)

---

## 💰 Modelo de Precificação V2

```yaml
Free:
  preco: $0
  features:
    - Repos públicos ilimitados
    - 10 PRs privados/mês
    - SAST básico (Checkstyle + PMD)
    - LLM: Gemma 3 local (rate-limited)
    - 3 usuários

Lite:
  preco: $12-15/dev/mês
  features:
    - PRs privados ilimitados
    - SAST completo (Sonar + Checkstyle + PMD + SpotBugs)
    - LLM: GPT-4.1 via OpenRouter
    - Review linha-a-linha
    - Code graph básico
    - 10 usuários

Pro:
  preco: $24-30/dev/mês
  features:
    - Multi-modelo (o3 + Claude + GPT-4.1)
    - Code graph completo
    - Análise de impacto
    - Auto-fix one-click
    - Jira/Linear integration
    - Geração de docstrings
    - Analytics dashboard
    - RAG com PRs históricos
    - 50 usuários

Enterprise:
  preco: Custom
  features:
    - Self-hosted option
    - Plugins ilimitados
    - SAML/SSO
    - SLA 99.9%
    - Suporte prioritário
    - Multi-org
    - AWS Bedrock (LLM privado)
    - Usuários ilimitados
    - Custom models fine-tuned
```

---

## 🛠️ Tecnologias e Dependências

### Backend

```xml
<!-- Core -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.2.1</version>
</dependency>

<!-- LangChain4j -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.26.0</version>
</dependency>

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

<!-- Sandbox -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
</dependency>

<!-- Vector DB -->
<dependency>
    <groupId>com.pgvector</groupId>
    <artifactId>pgvector</artifactId>
    <version>0.1.2</version>
</dependency>
```

### Frontend

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "zustand": "^4.4.7",
    "@tanstack/react-query": "^5.14.2",
    "recharts": "^2.10.3",
    "reactflow": "^11.10.1",
    "d3": "^7.8.5",
    "@monaco-editor/react": "^4.6.0",
    "socket.io-client": "^4.6.1",
    "axios": "^1.6.2"
  }
}
```

---

## 📈 Métricas de Sucesso

### Performance
- ⏱️ Tempo médio de review: **< 3 minutos**
- 🎯 Taxa de detecção de bugs: **> 45%** (vs 20% SAST tradicional)
- 💰 Custo por review: **$0.05-0.15** (otimizado com multi-modelo)

### Qualidade
- ✅ Falsos positivos: **< 15%**
- 🎨 Cobertura de linguagens: **20+ linguagens**
- 🔧 Auto-fixes aplicáveis: **> 60%** dos issues de style/code smell

### Adoção
- 👥 Usuários ativos: **Meta 10k em 12 meses**
- 📦 Plugins no marketplace: **Meta 50 em 6 meses**
- ⭐ Satisfação (NPS): **> 40**

---

## 🤝 Contribuindo

### Para IntegrAllTech (Uso Interno)

1. Clone o repositório
2. Revise a documentação relevante
3. Implemente features seguindo os guias
4. Teste localmente
5. Crie PR com descrição detalhada

### Para Comunidade (Futuro Open Source)

1. Fork o projeto
2. Crie uma branch para sua feature
3. Implemente seguindo os style guides
4. Adicione testes
5. Submeta PR

---

## 📞 Suporte e Recursos

### Documentação
- 📖 Docs completos: Ver arquivos individuais neste diretório
- 🎥 Vídeos tutoriais: (a ser criado)
- 💬 FAQ: (a ser criado)

### Comunidade
- 💼 Interno IntegrAllTech: Slack #pullwise-ai
- 🌐 Futuro público: Discord/GitHub Discussions

### Contato
- 📧 Email: edson@integralltech.com
- 🏢 Site: https://integralltech.com.br

---

## 📝 Licença

Copyright © 2025 IntegrAllTech

*Documentação proprietária para uso interno da IntegrAllTech. Futuras versões podem ser abertas sob licença open source.*

---

## 🙏 Agradecimentos

Esta V2 foi possível graças a insights de:
- **CodeRabbit** - Arquitetura multi-modelo e auto-fix
- **SonarQube** - SAST robusto e quality gates
- **Semgrep** - Regras customizáveis
- **Comunidade Open Source** - Ferramentas Rust (Ruff, Biome)

---

**Última atualização:** Janeiro 2026  
**Versão da documentação:** 2.0.0  
**Status:** 🚧 Em desenvolvimento ativo
