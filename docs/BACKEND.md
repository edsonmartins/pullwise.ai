# Backend - Pullwise (SaaS de Code Review com IA)

## 🦉 Pullwise - Wise Reviews for Every Pull

**Domínio Principal:** https://pullwise.ai  
**Documentação:** https://pullwise.dev

---

## Visão Geral do Produto

**Nome:** Pullwise

**Proposta de Valor:** Plataforma SaaS de code review automatizado que combina análise estática (SonarQube, Checkstyle, PMD) com IA (LLMs locais ou via OpenRouter), permitindo reviews completos e contextualizados com padrões customizados por equipe.

**Planos:**
- **Free**: 3 repos, 50 PRs/mês, LLM compartilhado básico
- **Pro**: $24/dev/mês, repos ilimitados, OpenRouter (GPT-4o/Claude), regras customizadas
- **Enterprise**: Custom, LLM próprio (local), SSO, suporte dedicado, on-premise

---

## Stack Tecnológica Backend

```
Java 17
Spring Boot 3.2+
Spring Security + OAuth2
PostgreSQL 15 (dados transacionais + pgvector para RAG)
Redis (cache + rate limiting)
RabbitMQ (fila de análises)
LangChain4j (orquestração LLM)
Docker + Kubernetes (deploy)
```

---

## Arquitetura Geral

```
┌─────────────────────────────────────────────────────────────┐
│                    ARQUITETURA BACKEND                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────┐         ┌─────────────────┐            │
│  │  API Gateway   │────────▶│  Auth Service   │            │
│  │  (Rate Limit)  │         │  (JWT/OAuth2)   │            │
│  └───────┬────────┘         └─────────────────┘            │
│          │                                                   │
│          ├──▶ Webhook Controller (GitHub/BitBucket/SonarQube)│
│          │                                                   │
│          ├──▶ REST API Controllers                          │
│          │    ├─ Projects                                   │
│          │    ├─ Reviews                                    │
│          │    ├─ Configurations                             │
│          │    ├─ Billing                                    │
│          │    └─ Analytics                                  │
│          │                                                   │
│          ▼                                                   │
│  ┌────────────────────────────────────────────┐            │
│  │         Review Orchestrator                 │            │
│  │  ┌──────────────────────────────────────┐  │            │
│  │  │  1. SAST Analysis (paralelo)         │  │            │
│  │  │     - SonarQube API                  │  │            │
│  │  │     - Checkstyle                     │  │            │
│  │  │     - PMD/SpotBugs                   │  │            │
│  │  │                                      │  │            │
│  │  │  2. RAG - Knowledge Base             │  │            │
│  │  │     - Busca guidelines (pgvector)    │  │            │
│  │  │     - ADRs, PRs anteriores           │  │            │
│  │  │                                      │  │            │
│  │  │  3. LLM Review                       │  │            │
│  │  │     - OpenRouter (Pro)               │  │            │
│  │  │     - Ollama local (Enterprise)      │  │            │
│  │  │                                      │  │            │
│  │  │  4. Consolidation & Posting          │  │            │
│  │  │     - Deduplica issues               │  │            │
│  │  │     - Formata comentários            │  │            │
│  │  │     - Posta no GitHub/BitBucket      │  │            │
│  │  └──────────────────────────────────────┘  │            │
│  └────────────────────────────────────────────┘            │
│          │                                                   │
│          ▼                                                   │
│  ┌────────────────┐  ┌─────────────┐  ┌──────────────┐    │
│  │   PostgreSQL   │  │    Redis    │  │  RabbitMQ    │    │
│  │  (multi-tenant)│  │   (cache)   │  │  (queues)    │    │
│  └────────────────┘  └─────────────┘  └──────────────┘    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Estrutura do Projeto

```
backend/
├── src/main/java/com/pullwise/api/
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── LangChain4jConfig.java
│   │   ├── RedisConfig.java
│   │   ├── RabbitMQConfig.java
│   │   └── MultiTenancyConfig.java
│   │
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Organization.java
│   │   │   ├── User.java
│   │   │   ├── Project.java
│   │   │   ├── PullRequest.java
│   │   │   ├── Review.java
│   │   │   ├── Issue.java
│   │   │   ├── Configuration.java
│   │   │   └── Subscription.java
│   │   │
│   │   ├── repository/
│   │   │   ├── OrganizationRepository.java
│   │   │   ├── ProjectRepository.java
│   │   │   ├── ReviewRepository.java
│   │   │   └── VectorKnowledgeRepository.java
│   │   │
│   │   └── enums/
│   │       ├── PlanType.java
│   │       ├── Severity.java
│   │       └── IssueType.java
│   │
│   ├── application/
│   │   ├── service/
│   │   │   ├── auth/
│   │   │   │   ├── AuthenticationService.java
│   │   │   │   └── OAuth2Service.java
│   │   │   │
│   │   │   ├── review/
│   │   │   │   ├── ReviewOrchestrator.java
│   │   │   │   ├── SastAnalysisService.java
│   │   │   │   ├── LLMReviewService.java
│   │   │   │   ├── ConsolidationService.java
│   │   │   │   └── PostingService.java
│   │   │   │
│   │   │   ├── integration/
│   │   │   │   ├── GitHubService.java
│   │   │   │   ├── BitBucketService.java
│   │   │   │   ├── SonarQubeService.java
│   │   │   │   └── OpenRouterService.java
│   │   │   │
│   │   │   ├── config/
│   │   │   │   ├── ConfigurationResolver.java
│   │   │   │   └── RAGService.java
│   │   │   │
│   │   │   ├── billing/
│   │   │   │   ├── StripeService.java
│   │   │   │   ├── UsageTracker.java
│   │   │   │   └── PlanManager.java
│   │   │   │
│   │   │   └── analytics/
│   │   │       ├── MetricsService.java
│   │   │       └── ReportService.java
│   │   │
│   │   └── dto/
│   │       ├── request/
│   │       └── response/
│   │
│   ├── infrastructure/
│   │   ├── webhook/
│   │   │   ├── GitHubWebhookController.java
│   │   │   ├── BitBucketWebhookController.java
│   │   │   └── SonarQubeWebhookController.java
│   │   │
│   │   ├── rest/
│   │   │   ├── ProjectController.java
│   │   │   ├── ReviewController.java
│   │   │   ├── ConfigurationController.java
│   │   │   ├── BillingController.java
│   │   │   └── AnalyticsController.java
│   │   │
│   │   └── messaging/
│   │       ├── ReviewQueueProducer.java
│   │       └── ReviewQueueConsumer.java
│   │
│   └── CodeReviewApplication.java
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── db/migration/  (Flyway)
│       ├── V1__initial_schema.sql
│       ├── V2__add_vector_extension.sql
│       └── V3__add_billing_tables.sql
│
└── pom.xml
```

---

## Schema do Banco de Dados

```sql
-- Extensão para pgvector (RAG)
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- MULTI-TENANCY & AUTH
-- ============================================================

CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    plan_type VARCHAR(50) NOT NULL DEFAULT 'free',
    settings JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    avatar_url VARCHAR(500),
    github_id VARCHAR(100),
    oauth_provider VARCHAR(50),
    oauth_token_encrypted TEXT,
    role VARCHAR(50) DEFAULT 'member',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE organization_members (
    organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) DEFAULT 'member',
    joined_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (organization_id, user_id)
);

-- ============================================================
-- PROJECTS & REPOSITORIES
-- ============================================================

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    repository_url VARCHAR(500) NOT NULL,
    platform VARCHAR(50) NOT NULL,  -- 'github', 'bitbucket'
    repository_id VARCHAR(255),
    default_branch VARCHAR(100) DEFAULT 'main',
    settings JSONB DEFAULT '{}'::jsonb,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_projects_org ON projects(organization_id);
CREATE INDEX idx_projects_repo ON projects(repository_url);

-- ============================================================
-- CONFIGURATIONS (Hierárquicas)
-- ============================================================

CREATE TABLE configurations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scope_type VARCHAR(50) NOT NULL,  -- 'organization', 'project', 'path'
    scope_id UUID NOT NULL,
    path_pattern VARCHAR(500),  -- Glob pattern para path-specific
    config_data JSONB NOT NULL,
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_config_scope ON configurations(scope_type, scope_id);

-- ============================================================
-- KNOWLEDGE BASE (RAG)
-- ============================================================

CREATE TABLE knowledge_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    doc_type VARCHAR(50) NOT NULL,  -- 'adr', 'readme', 'pr_comment', 'code'
    file_path VARCHAR(500),
    content TEXT NOT NULL,
    embedding vector(768),  -- nomic-embed-text dimension
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX ON knowledge_documents USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

CREATE INDEX idx_knowledge_project ON knowledge_documents(project_id);

-- ============================================================
-- REVIEWS & ISSUES
-- ============================================================

CREATE TABLE pull_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    pr_number INTEGER NOT NULL,
    pr_key VARCHAR(100),  -- GitHub: number, BitBucket: id
    title VARCHAR(500),
    author VARCHAR(255),
    source_branch VARCHAR(255),
    target_branch VARCHAR(255),
    commit_sha VARCHAR(100),
    state VARCHAR(50),  -- 'open', 'closed', 'merged'
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (project_id, pr_number)
);

CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pull_request_id UUID REFERENCES pull_requests(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,  -- 'pending', 'in_progress', 'completed', 'failed'
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    
    -- Métricas
    sast_issues_count INTEGER DEFAULT 0,
    llm_issues_count INTEGER DEFAULT 0,
    quality_score INTEGER,  -- 1-10
    
    -- Custos
    tokens_used INTEGER,
    llm_provider VARCHAR(50),
    cost_cents INTEGER,
    
    -- Resultado
    summary TEXT,
    metadata JSONB,
    
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_reviews_pr ON reviews(pull_request_id);

CREATE TABLE issues (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    review_id UUID REFERENCES reviews(id) ON DELETE CASCADE,
    
    -- Localização
    file_path VARCHAR(500) NOT NULL,
    line_number INTEGER,
    
    -- Classificação
    source VARCHAR(50) NOT NULL,  -- 'sonarqube', 'checkstyle', 'pmd', 'llm'
    type VARCHAR(50) NOT NULL,  -- 'bug', 'vulnerability', 'code_smell', 'logic', etc
    severity VARCHAR(50) NOT NULL,  -- 'critical', 'high', 'medium', 'low'
    
    -- Conteúdo
    message TEXT NOT NULL,
    description TEXT,
    reasoning TEXT,  -- Para issues LLM
    suggested_fix TEXT,
    
    -- Estado
    is_resolved BOOLEAN DEFAULT false,
    resolved_at TIMESTAMP,
    resolution_type VARCHAR(50),  -- 'accepted', 'rejected', 'wontfix'
    
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_issues_review ON issues(review_id);
CREATE INDEX idx_issues_severity ON issues(severity);

-- ============================================================
-- FEEDBACK & LEARNING
-- ============================================================

CREATE TABLE issue_feedback (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id UUID REFERENCES issues(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id),
    feedback_type VARCHAR(50) NOT NULL,  -- 'accept', 'reject', 'helpful', 'not_helpful'
    reason TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE rule_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_identifier VARCHAR(255) UNIQUE NOT NULL,
    source VARCHAR(50) NOT NULL,  -- 'sast' ou 'llm'
    
    total_occurrences INTEGER DEFAULT 0,
    accepted_count INTEGER DEFAULT 0,
    rejected_count INTEGER DEFAULT 0,
    
    confidence_score DECIMAL(3,2),  -- 0.00 - 1.00
    
    updated_at TIMESTAMP DEFAULT NOW()
);

-- ============================================================
-- BILLING & SUBSCRIPTIONS
-- ============================================================

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    plan_type VARCHAR(50) NOT NULL,
    stripe_subscription_id VARCHAR(255),
    stripe_customer_id VARCHAR(255),
    
    status VARCHAR(50) NOT NULL,  -- 'active', 'trialing', 'canceled', 'past_due'
    
    current_period_start TIMESTAMP,
    current_period_end TIMESTAMP,
    cancel_at_period_end BOOLEAN DEFAULT false,
    
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE usage_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    period_month DATE NOT NULL,  -- First day of month
    
    reviews_count INTEGER DEFAULT 0,
    tokens_used BIGINT DEFAULT 0,
    cost_cents INTEGER DEFAULT 0,
    
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (organization_id, period_month)
);

-- ============================================================
-- WEBHOOKS & INTEGRATIONS
-- ============================================================

CREATE TABLE webhook_deliveries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    source VARCHAR(50) NOT NULL,  -- 'github', 'bitbucket', 'sonarqube'
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    response_status INTEGER,
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_webhook_project ON webhook_deliveries(project_id, created_at DESC);
```

---

## Modelos de Domínio Principais

```java
// ===== Organization.java =====
@Entity
@Table(name = "organizations")
@Data
@Builder
public class Organization {
    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String slug;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type")
    private PlanType planType;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> settings;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

// ===== Project.java =====
@Entity
@Table(name = "projects")
@Data
public class Project {
    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;
    
    private String name;
    
    @Column(name = "repository_url")
    private String repositoryUrl;
    
    @Enumerated(EnumType.STRING)
    private Platform platform;
    
    @Column(name = "repository_id")
    private String repositoryId;
    
    @Column(name = "default_branch")
    private String defaultBranch;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private ProjectSettings settings;
    
    private Boolean isActive;
}

// ===== Review.java =====
@Entity
@Table(name = "reviews")
@Data
public class Review {
    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "pull_request_id")
    private PullRequest pullRequest;
    
    @Enumerated(EnumType.STRING)
    private ReviewStatus status;
    
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    @Column(name = "sast_issues_count")
    private Integer sastIssuesCount;
    
    @Column(name = "llm_issues_count")
    private Integer llmIssuesCount;
    
    @Column(name = "quality_score")
    private Integer qualityScore;
    
    @Column(name = "tokens_used")
    private Integer tokensUsed;
    
    @Column(name = "llm_provider")
    private String llmProvider;
    
    @Column(name = "cost_cents")
    private Integer costCents;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL)
    private List<Issue> issues = new ArrayList<>();
}

// ===== Issue.java =====
@Entity
@Table(name = "issues")
@Data
public class Issue {
    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "review_id")
    private Review review;
    
    @Column(name = "file_path")
    private String filePath;
    
    @Column(name = "line_number")
    private Integer lineNumber;
    
    @Enumerated(EnumType.STRING)
    private IssueSource source;
    
    @Enumerated(EnumType.STRING)
    private IssueType type;
    
    @Enumerated(EnumType.STRING)
    private Severity severity;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String reasoning;  // LLM explanation
    
    @Column(columnDefinition = "TEXT")
    private String suggestedFix;
    
    private Boolean isResolved;
    
    private LocalDateTime resolvedAt;
    
    @Enumerated(EnumType.STRING)
    private ResolutionType resolutionType;
}
```

---

## Configuração da Aplicação

```yaml
# application.yml
spring:
  application:
    name: codereview-api
  
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/codereview}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
    hikari:
      maximum-pool-size: 10
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  flyway:
    enabled: true
    locations: classpath:db/migration
  
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASS:guest}

# Configuração de segurança
security:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400000  # 24 horas
  oauth2:
    github:
      client-id: ${GITHUB_CLIENT_ID}
      client-secret: ${GITHUB_CLIENT_SECRET}
    bitbucket:
      client-id: ${BITBUCKET_CLIENT_ID}
      client-secret: ${BITBUCKET_CLIENT_SECRET}

# Integrações externas
integrations:
  sonarqube:
    enabled: true
    url: ${SONAR_URL}
    token: ${SONAR_TOKEN}
  
  openrouter:
    enabled: true
    api-key: ${OPENROUTER_API_KEY}
    base-url: https://openrouter.ai/api/v1
    default-model: anthropic/claude-3.5-sonnet
  
  ollama:
    enabled: ${OLLAMA_ENABLED:false}
    base-url: ${OLLAMA_URL:http://localhost:11434}
    model: gemma3:4b

# Billing
stripe:
  api-key: ${STRIPE_API_KEY}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET}
  prices:
    pro-monthly: ${STRIPE_PRICE_PRO}
    enterprise-monthly: ${STRIPE_PRICE_ENTERPRISE}

# Planos
plans:
  free:
    max-repos: 3
    max-reviews-per-month: 50
    llm-provider: shared
  pro:
    max-repos: -1  # Ilimitado
    max-reviews-per-month: -1
    llm-provider: openrouter
  enterprise:
    max-repos: -1
    max-reviews-per-month: -1
    llm-provider: ollama

# Rate limiting
rate-limit:
  enabled: true
  default-requests-per-minute: 60
  premium-requests-per-minute: 300
```

---

## Services Core

### ReviewOrchestrator.java

```java
@Service
@Slf4j
public class ReviewOrchestrator {
    
    @Autowired
    private SastAnalysisService sastAnalysis;
    
    @Autowired
    private LLMReviewService llmReview;
    
    @Autowired
    private ConsolidationService consolidation;
    
    @Autowired
    private PostingService posting;
    
    @Autowired
    private RAGService ragService;
    
    @Autowired
    private UsageTracker usageTracker;
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @Async("reviewExecutor")
    public CompletableFuture<Review> orchestrateReview(PullRequest pr) {
        Review review = Review.builder()
            .pullRequest(pr)
            .status(ReviewStatus.IN_PROGRESS)
            .startedAt(LocalDateTime.now())
            .build();
        
        review = reviewRepository.save(review);
        
        try {
            log.info("Starting review for PR #{} in project {}", 
                pr.getPrNumber(), pr.getProject().getName());
            
            // 1. SAST Analysis (paralelo)
            Map<String, List<Issue>> sastResults = sastAnalysis.analyzeAll(pr);
            List<Issue> sastIssues = sastResults.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
            
            log.info("SAST analysis completed: {} issues found", sastIssues.size());
            
            // 2. RAG - Buscar conhecimento relevante
            List<KnowledgeDocument> relevantDocs = ragService.searchRelevant(
                pr.getProject(),
                pr.getChangedFiles()
            );
            
            // 3. LLM Review (com contexto SAST + RAG)
            LLMReviewRequest llmRequest = buildLLMRequest(
                pr, 
                sastIssues, 
                relevantDocs
            );
            
            List<Issue> llmIssues = llmReview.analyze(llmRequest);
            
            log.info("LLM analysis completed: {} issues found", llmIssues.size());
            
            // 4. Consolidação
            ConsolidatedResult result = consolidation.merge(sastIssues, llmIssues);
            
            // 5. Atualizar review
            review.setSastIssuesCount(sastIssues.size());
            review.setLlmIssuesCount(llmIssues.size());
            review.setQualityScore(result.getQualityScore());
            review.setSummary(result.getSummary());
            review.setIssues(result.getIssues());
            review.setStatus(ReviewStatus.COMPLETED);
            review.setCompletedAt(LocalDateTime.now());
            
            // Tracking de tokens e custo
            if (llmRequest.getTokensUsed() != null) {
                review.setTokensUsed(llmRequest.getTokensUsed());
                review.setLlmProvider(llmRequest.getProvider());
                review.setCostCents(calculateCost(llmRequest));
                
                usageTracker.recordUsage(
                    pr.getProject().getOrganization(),
                    llmRequest.getTokensUsed(),
                    review.getCostCents()
                );
            }
            
            review = reviewRepository.save(review);
            
            // 6. Postar resultados no GitHub/BitBucket
            posting.postReview(pr, result);
            
            log.info("Review completed successfully for PR #{}", pr.getPrNumber());
            
            return CompletableFuture.completedFuture(review);
            
        } catch (Exception e) {
            log.error("Error during review orchestration", e);
            review.setStatus(ReviewStatus.FAILED);
            review.setCompletedAt(LocalDateTime.now());
            reviewRepository.save(review);
            
            return CompletableFuture.failedFuture(e);
        }
    }
    
    private LLMReviewRequest buildLLMRequest(
            PullRequest pr,
            List<Issue> sastIssues,
            List<KnowledgeDocument> knowledgeDocs) {
        
        // Buscar configurações hierárquicas
        Configuration config = configResolver.resolveFor(pr.getProject());
        
        return LLMReviewRequest.builder()
            .pullRequest(pr)
            .sastContext(buildSastContext(sastIssues))
            .knowledgeContext(buildKnowledgeContext(knowledgeDocs))
            .customInstructions(config.getCustomInstructions())
            .pathInstructions(config.getPathInstructions())
            .focusAreas(determineFocusAreas(sastIssues))
            .build();
    }
}
```

### LLMReviewService.java

```java
@Service
public class LLMReviewService {
    
    @Autowired
    private OpenRouterService openRouter;
    
    @Autowired
    private OllamaService ollama;
    
    @Value("${integrations.openrouter.enabled}")
    private Boolean openRouterEnabled;
    
    @Value("${integrations.ollama.enabled}")
    private Boolean ollamaEnabled;
    
    public List<Issue> analyze(LLMReviewRequest request) {
        // Determinar provider baseado no plano da organização
        PlanType plan = request.getPullRequest()
            .getProject()
            .getOrganization()
            .getPlanType();
        
        LLMProvider provider = selectProvider(plan);
        
        String prompt = buildPrompt(request);
        
        LLMResponse response = switch (provider) {
            case OPENROUTER -> openRouter.complete(prompt, request);
            case OLLAMA -> ollama.complete(prompt, request);
            default -> throw new IllegalStateException("No LLM provider available");
        };
        
        request.setTokensUsed(response.getTokensUsed());
        request.setProvider(provider.name());
        
        return parseIssuesFromResponse(response.getContent());
    }
    
    private LLMProvider selectProvider(PlanType plan) {
        return switch (plan) {
            case PRO -> openRouterEnabled ? LLMProvider.OPENROUTER : LLMProvider.SHARED;
            case ENTERPRISE -> ollamaEnabled ? LLMProvider.OLLAMA : LLMProvider.OPENROUTER;
            default -> LLMProvider.SHARED;
        };
    }
    
    private String buildPrompt(LLMReviewRequest request) {
        return String.format("""
            # CODE REVIEW HÍBRIDO - ANÁLISE COMPLEMENTAR
            
            ## CONTEXTO DO PROJETO
            Repositório: %s
            Pull Request: #%d - %s
            
            ## ANÁLISE ESTÁTICA JÁ REALIZADA
            %s
            
            ## CONHECIMENTO DO TIME
            %s
            
            ## INSTRUÇÕES CUSTOMIZADAS
            %s
            
            ## CÓDIGO PARA REVISAR
            %s
            
            ## SUA MISSÃO
            Analise o código focando em:
            - Lógica de negócio e corretude
            - Decisões arquiteturais
            - Manutenibilidade
            - Contexto de domínio
            
            NÃO reporte issues já detectados por SAST.
            
            Retorne JSON estruturado:
            {
              "issues": [
                {
                  "file": "...",
                  "line": 42,
                  "severity": "high|medium|low",
                  "category": "logic|architecture|maintainability|performance",
                  "message": "...",
                  "reasoning": "...",
                  "suggestedFix": "..."
                }
              ]
            }
            """,
            request.getPullRequest().getProject().getName(),
            request.getPullRequest().getPrNumber(),
            request.getPullRequest().getTitle(),
            request.getSastContext(),
            request.getKnowledgeContext(),
            request.getCustomInstructions(),
            formatCodeDiff(request.getPullRequest())
        );
    }
}
```

### OpenRouterService.java

```java
@Service
public class OpenRouterService {
    
    @Value("${integrations.openrouter.api-key}")
    private String apiKey;
    
    @Value("${integrations.openrouter.base-url}")
    private String baseUrl;
    
    @Value("${integrations.openrouter.default-model}")
    private String defaultModel;
    
    @Autowired
    private RestTemplate restTemplate;
    
    public LLMResponse complete(String prompt, LLMReviewRequest context) {
        String url = baseUrl + "/chat/completions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("HTTP-Referer", "https://codereview.integalltech.com.br");
        headers.set("X-Title", "CodeReview SaaS");
        
        Map<String, Object> requestBody = Map.of(
            "model", defaultModel,
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.3,
            "max_tokens", 4096,
            "response_format", Map.of("type", "json_object")
        );
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(requestBody, headers),
                Map.class
            );
            
            Map<String, Object> body = response.getBody();
            String content = extractContent(body);
            int tokensUsed = extractTokens(body);
            
            return LLMResponse.builder()
                .content(content)
                .tokensUsed(tokensUsed)
                .model(defaultModel)
                .provider("openrouter")
                .build();
            
        } catch (Exception e) {
            log.error("Error calling OpenRouter API", e);
            throw new LLMException("Failed to complete review with OpenRouter", e);
        }
    }
    
    private String extractContent(Map<String, Object> response) {
        List<Map> choices = (List<Map>) response.get("choices");
        Map message = (Map) choices.get(0).get("message");
        return (String) message.get("content");
    }
    
    private int extractTokens(Map<String, Object> response) {
        Map usage = (Map) response.get("usage");
        return ((Number) usage.get("total_tokens")).intValue();
    }
}
```

---

## Webhooks Controllers

### GitHubWebhookController.java

```java
@RestController
@RequestMapping("/webhooks/github")
@Slf4j
public class GitHubWebhookController {
    
    @Autowired
    private ReviewOrchestrator orchestrator;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("X-GitHub-Event") String event,
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestBody String payload) {
        
        // 1. Validar assinatura HMAC
        if (!validateSignature(payload, signature)) {
            log.warn("Invalid signature for GitHub webhook");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // 2. Processar apenas eventos de PR
        if (!"pull_request".equals(event)) {
            return ResponseEntity.ok().build();
        }
        
        // 3. Parse payload
        GitHubWebhookPayload webhookPayload = parsePayload(payload);
        String action = webhookPayload.getAction();
        
        // 4. Trigger review para ações relevantes
        if (Arrays.asList("opened", "synchronize", "reopened").contains(action)) {
            PullRequest pr = mapToPullRequest(webhookPayload);
            
            // Buscar projeto configurado
            Optional<Project> project = projectRepository
                .findByRepositoryUrl(pr.getRepositoryUrl());
            
            if (project.isEmpty()) {
                log.warn("No project configured for repository: {}", 
                    pr.getRepositoryUrl());
                return ResponseEntity.notFound().build();
            }
            
            pr.setProject(project.get());
            orchestrator.orchestrateReview(pr);
        }
        
        return ResponseEntity.accepted().build();
    }
    
    private boolean validateSignature(String payload, String signature) {
        String computed = "sha256=" + 
            HmacUtils.hmacSha256Hex(webhookSecret, payload);
        return MessageDigest.isEqual(
            computed.getBytes(),
            signature.getBytes()
        );
    }
}
```

---

## REST API Controllers

### ProjectController.java

```java
@RestController
@RequestMapping("/api/projects")
@Validated
public class ProjectController {
    
    @Autowired
    private ProjectService projectService;
    
    @GetMapping
    public ResponseEntity<Page<ProjectDTO>> listProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        Page<ProjectDTO> projects = projectService.findByOrganization(orgId, page, size);
        
        return ResponseEntity.ok(projects);
    }
    
    @PostMapping
    public ResponseEntity<ProjectDTO> createProject(
            @Valid @RequestBody CreateProjectRequest request) {
        
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        ProjectDTO project = projectService.create(orgId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }
    
    @GetMapping("/{projectId}/reviews")
    public ResponseEntity<Page<ReviewDTO>> getReviews(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<ReviewDTO> reviews = reviewService.findByProject(projectId, page, size);
        
        return ResponseEntity.ok(reviews);
    }
    
    @PostMapping("/{projectId}/sync-knowledge")
    public ResponseEntity<Void> syncKnowledgeBase(@PathVariable UUID projectId) {
        ragService.syncProject(projectId);
        return ResponseEntity.accepted().build();
    }
}
```

---

## Billing & Usage Tracking

### UsageTracker.java

```java
@Service
public class UsageTracker {
    
    @Autowired
    private UsageRecordRepository usageRepository;
    
    public void recordUsage(
            Organization org,
            int tokensUsed,
            int costCents) {
        
        YearMonth currentMonth = YearMonth.now();
        LocalDate periodStart = currentMonth.atDay(1);
        
        UsageRecord record = usageRepository
            .findByOrganizationAndPeriod(org.getId(), periodStart)
            .orElse(UsageRecord.builder()
                .organization(org)
                .periodMonth(periodStart)
                .build());
        
        record.setReviewsCount(record.getReviewsCount() + 1);
        record.setTokensUsed(record.getTokensUsed() + tokensUsed);
        record.setCostCents(record.getCostCents() + costCents);
        
        usageRepository.save(record);
    }
    
    public boolean hasReachedLimit(Organization org) {
        if (org.getPlanType() == PlanType.PRO || 
            org.getPlanType() == PlanType.ENTERPRISE) {
            return false;  // Ilimitado
        }
        
        YearMonth currentMonth = YearMonth.now();
        LocalDate periodStart = currentMonth.atDay(1);
        
        UsageRecord record = usageRepository
            .findByOrganizationAndPeriod(org.getId(), periodStart)
            .orElse(UsageRecord.builder().reviewsCount(0).build());
        
        int limit = 50;  // Free plan limit
        return record.getReviewsCount() >= limit;
    }
}
```

---

## Autenticação & Segurança

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/webhooks/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint()
                    .baseUri("/oauth2/authorize")
                .and()
                .redirectionEndpoint()
                    .baseUri("/oauth2/callback/*")
            )
            .addFilterBefore(jwtAuthFilter(), 
                UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthFilter() {
        return new JwtAuthenticationFilter();
    }
}
```

---

## Deploy & Docker

```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: pgvector/pgvector:pg15
    environment:
      POSTGRES_DB: codereview
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
  
  rabbitmq:
    image: rabbitmq:3-management-alpine
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
  
  api:
    build: .
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/codereview
      REDIS_HOST: redis
      RABBITMQ_HOST: rabbitmq
    depends_on:
      - postgres
      - redis
      - rabbitmq

volumes:
  postgres_data:
```

---

## Próximos Passos

1. ✅ Implementar modelos e repositórios
2. ✅ Criar services core (ReviewOrchestrator, LLMReviewService)
3. ✅ Implementar webhooks (GitHub, BitBucket, SonarQube)
4. ✅ Configurar autenticação OAuth2
5. ✅ Integrar Stripe para billing
6. ✅ Implementar RAG com pgvector
7. ✅ Criar REST APIs
8. ✅ Adicionar testes unitários e integração
9. ✅ Setup CI/CD
10. ✅ Deploy em produção (Kubernetes ou Railway/Render)
