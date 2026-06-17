# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pullwise is an open-source, self-hosted AI code review platform combining static analysis (SAST) with LLMs. Monorepo with a Spring Boot backend, React frontend, and Docker-based infrastructure.

## Build & Development Commands

### Backend (Java 17 + Spring Boot 3.2 + Maven)

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev    # Run dev server (port 8080)
mvn test -B                                                # Run all tests
mvn test -Dtest=ClassName                                  # Run a single test class
mvn test -Dtest=ClassName#methodName                       # Run a single test method
mvn clean package -DskipTests                              # Build JAR without tests
```

### Frontend (React 18 + TypeScript + Vite)

```bash
cd frontend
npm ci --legacy-peer-deps    # Install deps (legacy-peer-deps required)
npm run dev                  # Dev server with HMR (port 3000)
npm run build                # Production build (runs tsc first)
npm run lint                 # ESLint check
npx tsc --noEmit             # TypeScript check only
```

### Full Stack (Docker Compose)

```bash
docker-compose up -d                              # Start all services
docker-compose --profile monitoring up -d         # Include Prometheus/Grafana/Jaeger
```

Services: PostgreSQL (5432), Redis (6379), RabbitMQ (5672/15672), Backend (8080), Frontend (3000)

## Architecture

### Backend (`backend/src/main/java/com/pullwise/api/`)

Layered architecture with four top-level packages:

- **`config/`** — Spring configuration beans (Security, Redis, WebSocket, Artemis, Jackson, OpenAPI)
- **`application/`** — Business logic services and DTOs
- **`domain/`** — JPA entities, repositories, and enums
- **`infrastructure/`** — REST controllers, webhook handlers, WebSocket handlers

Key subsystems in `application/service/`:

- **`review/pipeline/`** — 4-pass code review pipeline: SAST Analysis → LLM Review → Security Focus → Code Graph Impact, followed by Consolidation (Blast-Radius severity promotion) → Comment Positioning → Dedup → Prioritization → Reflection Filter → Synthesis. **Comment Positioning** (`synthesis/CommentPositioner` + `UnifiedDiffParser`) corrects LLM line-number drift deterministically by matching each finding's `existing_code` snippet against the diff (toggle: `review.position_correction`). **Reflection Filter** (`synthesis/ReviewReflectionFilter`) drops LLM findings the diff *proves* wrong ("falsify, don't verify"), marking `isFalsePositive` — one extra LLM call per file with findings (toggle: `review.reflection_enabled`). The **LLM Review** pass itself injects a deterministic per-file-type checklist resolved by glob (`pipeline/rules/ReviewRuleResolver` + `GlobPattern`, rules in `resources/review-rules/`; toggle: `review.rule_guidance_enabled`) and, for large files, runs a **Plan Phase** that produces a risk map to focus analysis (toggles: `review.plan_phase_enabled`, `review.plan_line_threshold`, default 50). All four patterns are inspired by Alibaba's Open Code Review.
- **`llm/router/MultiModelLLMRouter`** — Routes reviews to optimal LLM based on language, complexity, cost, and user tier. Supports OpenRouter (cloud) and Ollama (local)
- **`plugin/`** — SPI-based plugin system with dynamic loading and sandboxed execution
- **`graph/`** — Code dependency analysis (JGraphT for legacy in-memory; persistent Code Graph v2 in PostgreSQL via `code_graph_nodes`/`code_graph_edges`)
- **`graph/blast/`** — **Blast-Radius v2**: forward BFS via Postgres CTE recursiva, confidence tiers (`EXTRACTED`/`INFERRED`/`AMBIGUOUS`) e risk scoring (depth, hotspot, security keywords, test gap × propagated confidence). Cache Redis (`blast-radius`, TTL 10 min) e métricas Micrometer (`pullwise.blast_radius.*`). Endpoints em `/api/projects/{id}/blast-radius`, `/code-graph/stats`, `/code-graph/hotspots/recompute`.
- **`autofix/`** — Auto-fix generation and application via Git

Database: PostgreSQL 16 with pgvector (for RAG embeddings). Migrations via Flyway in `src/main/resources/db/migration/` (V1-V9). V9 introduced `code_graph_nodes` and `code_graph_edges` for the Blast-Radius v2 feature. Hibernate DDL set to `validate` — all schema changes must go through Flyway migrations.

Messaging: RabbitMQ for async job processing; WebSocket (STOMP) for real-time updates to frontend.

### Frontend (`frontend/src/`)

- **`pages/`** — Route-level components. V1 pages at root, V2 pages in `pages/v2/`
- **`components/ui/`** — Shadcn/ui primitives (button, card, accordion, etc.)
- **`components/landing/`** — Landing page sections (Hero, Features, Pricing, etc.)
- **`store/v2-store.ts`** — Zustand store for client state
- **`lib/api.ts`** — Centralized Axios client (base URL from `VITE_API_URL`)
- **`lib/translations.ts`** — i18n strings for en/pt/es
- **`contexts/`** — React contexts for Auth, WebSocket, Language, Theme

Path alias: `@/*` maps to `./src/*` (configured in vite.config.ts and tsconfig.json).

UI stack: TailwindCSS + Shadcn/ui for primitives, Mantine for complex components (charts, forms, modals, notifications). Server state via TanStack Query v5. Monaco Editor for code display. React Flow + D3 for graph visualization.

Vite dev server proxies `/api` and `/webhooks` to `localhost:8080`.

### CI/CD (`.github/workflows/ci-cd.yml`)

Pipeline: backend-test (JUnit + pgvector service container) → backend-build → frontend-test (tsc + lint + build) → docker-build (push to ghcr.io on main) → deploy-staging (on develop). Security scan via Trivy runs in parallel.

## Key Configuration

- **Backend profiles**: `dev` (local), `docker` (compose), `prod` (production). Set via `SPRING_PROFILES_ACTIVE`.
- **LLM routing strategy**: Configured in `application.yml` under `llm.router.strategy` — options: `cost-optimized`, `quality-first`, `balanced`.
- **Frontend env**: Only `VITE_API_URL` is required (defaults to `http://localhost:8080/api`).
- **Backend uses Lombok** — annotation processing is configured in `pom.xml`.
