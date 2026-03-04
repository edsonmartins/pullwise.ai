# Pullwise - A Plataforma Open Source de Code Review

<div align="center">

  <img src="images/logo_pullwise.png" alt="Pullwise Logo" width="120" />

  **A Plataforma Open Source de Code Review**

  [![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
  [![CI/CD](https://github.com/integralltech/pullwise-ai/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/integralltech/pullwise-ai/actions)
  [![GitHub Stars](https://img.shields.io/github/stars/integralltech/pullwise-ai?style=social)](https://github.com/integralltech/pullwise-ai)

  [Website](https://pullwise.ai) • [Docs](https://docs.pullwise.ai) • [Demo](https://pullwise.ai/demo) • [Discord](https://discord.gg/pullwise)

  **Pronto para produção. Gratuito para sempre. Licença MIT.**

</div>

---

## O que é o Pullwise?

Pullwise é uma **plataforma open source e self-hosted de code review com IA** que combina análise estática (SAST) com modelos de linguagem (LLMs) para fornecer revisões de código inteligentes e automatizadas.

### O Problema

Code reviews são essenciais para a qualidade do software, mas são:
- **Demorados** — Desenvolvedores seniores gastam horas revisando PRs
- **Inconsistentes** — Diferentes revisores encontram diferentes problemas
- **Caros** — Ferramentas enterprise custam milhares por mês
- **Vendor lock-in** — Soluções proprietárias prendem seus dados

### A Solução

**Pullwise Community Edition (Licença MIT):**
- **Gratuito para sempre** — Sem cartão de crédito, sem limites de tempo
- **Self-hosted** — Seu código nunca sai da sua infraestrutura
- **IA Integrada** — Suporte multi-modelo LLM (Claude, GPT-4, modelos locais via Ollama)
- **Integração SAST** — SonarQube, ESLint, Checkstyle, PMD, SpotBugs
- **Multi-Plataforma** — GitHub, GitLab, BitBucket, Azure DevOps
- **Auto-Fix** — Aplique sugestões de IA com um clique
- **Suporte a IDEs** — Extensão VS Code e plugin IntelliJ IDEA
- **CLI** — Interface de linha de comando completa (`pullwise` / `pw`)

---

## Início Rápido

### Docker Compose (recomendado)

```bash
# Clone o repositório
git clone https://github.com/integralltech/pullwise-ai.git
cd pullwise-ai

# Inicie todos os serviços
docker-compose up -d

# Acesse o Pullwise
# Frontend: http://localhost:3000
# API Backend: http://localhost:8080
```

Isso inicia PostgreSQL (com pgvector), Redis, RabbitMQ, backend e frontend.

Para incluir monitoramento (Prometheus, Grafana, Jaeger):

```bash
docker-compose --profile monitoring up -d
# Grafana: http://localhost:3001
# Prometheus: http://localhost:9090
# Jaeger: http://localhost:16686
```

### Requisitos do Sistema

- **Docker** 20.10+ e Docker Compose 2.0+
- **4 GB RAM** mínimo (8 GB recomendado com monitoramento)
- **10 GB** de espaço em disco
- **Linux**, **macOS** ou **Windows** com WSL2

---

## Arquitetura

![Arquitetura Pullwise](images/arquitetura.png)

**Backend:** Java 17, Spring Boot 3.2, PostgreSQL 16 (pgvector), Redis, RabbitMQ

**Frontend:** React 18, TypeScript, Vite, Mantine UI, TanStack Query

**CLI:** Node.js, Commander.js — `npm install -g @pullwise/cli`

**Extensões IDE:** VS Code (.vsix) e plugin IntelliJ IDEA

---

## Funcionalidades Principais

### Reviews Híbridos SAST + IA

O Pullwise combina análise estática com IA em um pipeline multi-pass:

1. **Análise Estática** (execução paralela):
   - SonarQube (bugs, vulnerabilidades, code smells)
   - ESLint (JavaScript/TypeScript)
   - Checkstyle, PMD, SpotBugs (Java)

2. **Review com IA** (com contexto completo):
   - Resultados SAST como baseline
   - Análise de grafo de código (dependency-aware)
   - RAG com base de conhecimento do projeto (pgvector)
   - Instruções customizadas do time

3. **Consolidação Inteligente**:
   - Deduplicação de issues similares
   - Priorização por severidade e risco
   - Formatação de comentários inline acionáveis

### Roteador Multi-Modelo LLM

- **Modelos cloud**: Claude, GPT-4, Gemini Pro via OpenRouter
- **Modelos locais**: Llama 3, Mistral, Gemma via Ollama
- **Estratégias de roteamento**: `cost-optimized`, `quality-first`, `balanced`
- **Fallback**: Degradação graceful quando modelos estão indisponíveis

### Suporte Multi-Plataforma

| Plataforma | Webhooks | Comentários em PR | Status Checks |
|------------|----------|-------------------|---------------|
| GitHub | Sim | Sim | Sim |
| GitLab | Sim | Sim | Sim |
| BitBucket | Sim | Sim | Sim |
| Azure DevOps | Sim | Sim | Sim |

### Auto-Fix

- Sugestões de correção geradas por IA com score de confiança
- Preview seguro com diff de código antes de aplicar
- Operações em lote para múltiplas issues
- Suporte a rollback

### Sistema de Plugins

Extensível via arquitetura SPI:
- Linters de linguagem (Rust, Go, Python, PHP)
- Regras específicas de framework (Laravel, Django, Spring)
- Checks customizados para seu codebase

### CLI

```bash
npm install -g @pullwise/cli

pw auth login                    # Autenticar
pw projects list                 # Listar projetos
pw reviews trigger 42            # Disparar review para PR #42
pw reviews watch 123             # Acompanhar review em tempo real
pw review --staged               # Revisar mudanças staged localmente
pw hooks install                 # Instalar git hooks
```

### Extensões para IDEs

- **VS Code**: Diagnósticos inline, disparar reviews, visualizar issues, integração na status bar
- **IntelliJ IDEA**: External annotator, ações de review, painel de configurações, widget na status bar

---

## Edições

O Pullwise segue um **modelo open-core**:

| Funcionalidade | Community Edition | Professional | Enterprise |
|----------------|------------------|-------------|------------|
| **Preço** | **GRATUITO** | $49/dev/mês | $99/dev/mês |
| **Licença** | MIT | Proprietária | Proprietária |
| **Usuários** | 5 | 50 | Ilimitado |
| **Organizações** | 1 | 3 | Ilimitado |
| **Pipeline de Review** | 2-pass | 4-pass | 4-pass |
| **Code Graph** | -- | Sim | Sim |
| **SSO/SAML** | -- | Sim | Sim |
| **Logs de Auditoria** | -- | 30 dias | 1 ano |
| **SLA** | Comunidade | 48h | 4h |

---

## Desenvolvimento

### Backend (Java 17 + Spring Boot 3.2 + Maven)

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev    # Servidor dev (porta 8080)
mvn test -B                                                # Rodar todos os testes
mvn test -Dtest=ClassName#methodName                       # Rodar teste específico
```

### Frontend (React 18 + TypeScript + Vite)

```bash
cd frontend
npm ci --legacy-peer-deps    # Instalar dependências
npm run dev                  # Servidor dev (porta 3000, proxy /api para 8080)
npm run build                # Build de produção
npm run lint                 # Verificação ESLint
```

### CLI

```bash
cd cli
npm ci                       # Instalar dependências
npm run dev                  # Modo dev com watch
npm run build                # Build para distribuição
```

---

## Deploy

### Docker Compose (recomendado)

```bash
docker-compose up -d
```

Variáveis de ambiente para produção:

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `DB_HOST` | Host do PostgreSQL | `localhost` |
| `DB_PASSWORD` | Senha do banco de dados | `pullwise` |
| `JWT_SECRET` | Chave de assinatura JWT (mín. 32 caracteres) | -- |
| `REDIS_HOST` | Host do Redis | `localhost` |
| `OPENROUTER_API_KEY` | Chave da API OpenRouter para LLMs cloud | -- |
| `PULLWISE_ENCRYPTION_KEY` | Chave AES-256 para configs sensíveis | -- |

---

## Contribuindo

Contribuições são bem-vindas! Áreas prioritárias:

- Integrações de linguagem e plugins
- Integrações de plataforma
- Melhorias na documentação
- Reports de bugs e testes

Veja as [Good First Issues](https://github.com/integralltech/pullwise-ai/issues?q=label%3A%22good+first+issue%22+is%3Aopen+is%3Aissue) para começar.

---

## Licença

**Community Edition** — [Licença MIT](LICENSE)

Livre para usar, modificar e distribuir. Para sempre.

---

<div align="center">

  **[Voltar ao Topo](#pullwise---a-plataforma-open-source-de-code-review)**

  Feito com dedicação pela comunidade Pullwise

  **pullwise.ai** • [@pullwise](https://twitter.com/pullwise) • [hello@pullwise.ai](mailto:hello@pullwise.ai)

</div>
