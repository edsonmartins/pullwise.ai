# Pullwise Backend

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?logo=postgresql&logoColor=white)](https://www.postgresql.org/)

The Spring Boot backend for Pullwise - AI Code Review Platform.

---

## 🚀 Quick Start

```bash
# Build the project
./mvnw clean package

# Run with dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run with Docker
docker-compose up
```

**API:** `http://localhost:8080/api`
**Actuator:** `http://localhost:8080/actuator`

---

## 📁 Project Structure

```
src/main/java/com/pullwise/
├── api/
│   ├── application/service/
│   │   ├── llm/
│   │   │   ├── LangChain4jService.java       # LLM orchestration
│   │   │   ├── OpenRouterService.java       # OpenRouter integration
│   │   │   └── OllamaService.java           # Local model support
│   │   ├── review/
│   │   │   ├── ReviewService.java          # Review orchestration
│   │   │   ├── pipeline/
│   │   │   │   ├── SastAnalysisPass.java   # SAST integration
│   │   │   │   ├── LLMReviewPass.java      # AI-powered review
│   │   │   │   └── ConsolidationPass.java   # Merge and deduplicate
│   │   │   ├── autofix/
│   │   │   │   ├── AutoFixService.java     # Generate fixes
│   │   │   │   └── FixApplier.java          # Apply fixes to PRs
│   │   │   └── graph/
│   │   │       └── CodeGraphService.java   # Dependency analysis
│   │   ├── plugin/
│   │   │   ├── PluginService.java          # Plugin system core
│   │   │   ├── PluginRegistry.java        # SPI registry
│   │   │   └── executor/
│   │   │       ├── RustToolExecutor.java   # Ruff, Biome integration
│   │   │       └── SastToolExecutor.java   # SonarQube, ESLint
│   │   ├── sast/
│   │   │   ├── SonarQubeService.java      # SonarQube integration
│   │   │   ├── EsLintService.java         # ESLint integration
│   │   │   └── CheckstyleService.java     # Checkstyle integration
│   │   └── webhook/
│   │       ├── GitHubWebhookHandler.java  # GitHub webhook handling
│   │       └── GitLabWebhookHandler.java  # GitLab webhook handling
│   ├── infrastructure/
│   │   ├── websockets/
│   │   │   └── ReviewWebSocketHandler.java # Real-time updates
│   │   ├── rest/
│   │   │   ├── ReviewController.java      # REST API
│   │   │   ├── PluginController.java      # Plugin management
│   │   │   └── AnalyticsController.java  # Metrics endpoint
│   │   ├── security/
│   │   │   ├── OAuth2UserService.java     # OAuth2 + JWT
│   │   │   └── JwtAuthenticationFilter.java # JWT validation
│   │   ├── persistence/
│   │   │   ├── ReviewRepository.java      # JPA entities
│   │   │   ├── PluginRepository.java      # Plugin storage
│   │   │   └── UserRepository.java        # User management
│   │   └── messaging/
│   │       ├── rabbitmq/
│   │       │   └── ReviewQueueConfig.java # Job queue
│   │       └── websocket/
│   │           └── WebSocketConfig.java   # WebSocket setup
│   └── domain/
│       ├── model/
│       │   ├── Review.java                 # Review entity
│       │   ├── Issue.java                 # Finding entity
│       │   ├── Plugin.java                # Plugin entity
│       │   └── User.java                  # User entity
│       └── repository/
│           ├── ReviewRepository.java      # Review storage
│           └── PluginRepository.java      # Plugin storage
└── resources/
    ├── application.yml                    # Main configuration
    ├── application-dev.yml                # Dev profile
    ├── application-prod.yml               # Production profile
    └── db/migration/                       # Flyway migrations
```

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| **Framework** | Spring Boot 3.2 |
| **Language** | Java 17 |
| **Build** | Maven |
| **Database** | PostgreSQL 16 + pgvector |
| **Cache** | Redis |
| **Message Queue** | RabbitMQ |
| **LLM Integration** | LangChain4j |
| **SAST Tools** | SonarQube, ESLint, Checkstyle, PMD, SpotBugs |
| **WebSocket** | Spring WebSocket + STOMP |
| **Security** | Spring Security + OAuth2 + JWT |
| **API Docs** | SpringDoc OpenAPI |
| **Testing** | JUnit 5, Testcontainers |

---

## 🔧 Core Services

### 1. LLM Router Service

```java
@Service
public class LLMRouterService {

    public Review generateReview(ReviewRequest request) {
        // Select best model based on:
        // - Language (Java, JS, Python)
        // - Complexity (simple, medium, complex)
        // - Cost (local vs cloud)
        // - User tier (CE, Pro, EE)

        LanguageModel model = selectModel(request);
        return model.generate(request);
    }
}
```

### 2. Multi-Pass Review Pipeline

```java
@Service
public class ReviewPipelineService {

    public Review execute(ReviewRequest request) {
        // Pass 1: SAST Analysis (parallel)
        List<Issue> sastIssues = sastPass.analyze(request);

        // Pass 2: LLM Review with context
        List<Issue> llmIssues = llmPass.analyze(request, sastIssues);

        // Pass 3: Consolidation
        List<Issue> consolidated = consolidationPass.merge(sastIssues, llmIssues);

        // Pass 4: Prioritization
        return prioritizationPass.rank(consolidated);
    }
}
```

### 3. Plugin System (SPI)

```java
// Interface
public interface PullwisePlugin {
    String getName();
    String getVersion();
    List<Issue> analyze(ReviewContext context);
}

// Example implementation
@Component
public class RustRuffPlugin implements PullwisePlugin {
    @Override
    public List<Issue> analyze(ReviewContext context) {
        // Run Ruff via Docker
        return executeRuff(context.getDiff());
    }
}
```

### 4. Webhook Handlers

```java
@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    @PostMapping("/github")
    public ResponseEntity<?> handleGitHub(@Payload GitHubPushEvent event) {
        // Parse push event
        // Queue review job
        // Return 202 Accepted
        return ResponseEntity.accepted().build();
    }
}
```

---

## 🔌 API Endpoints

### Reviews

```
GET    /api/reviews              List reviews
GET    /api/reviews/{id}         Get review details
POST   /api/reviews/trigger       Trigger manual review
POST   /api/reviews/{id}/fix     Apply auto-fix
```

### Plugins

```
GET    /api/plugins              List plugins
POST   /api/plugins/install      Install plugin
DELETE /api/plugins/{id}         Uninstall plugin
```

### Analytics

```
GET    /api/analytics/team       Team metrics
GET    /api/analytics/trends     Quality trends
GET    /api/analytics/performance Performance data
```

### Webhooks

```
POST   /api/webhooks/github     GitHub webhook
POST   /api/webhooks/gitlab     GitLab webhook
POST   /api/webhooks/bitbucket  BitBucket webhook
```

---

## 🗄️ Database Schema

### Key Tables

```sql
-- Reviews
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    pull_request_id BIGINT NOT NULL,
    status VARCHAR(31) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Issues
CREATE TABLE issues (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT REFERENCES reviews(id),
    severity VARCHAR(15) NOT NULL,
    rule_id VARCHAR(255),
    file_path TEXT NOT NULL,
    start_line INTEGER,
    end_line INTEGER,
    suggestion TEXT,
    status VARCHAR(31) DEFAULT 'pending'
);

-- Plugins
CREATE TABLE plugins (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    version VARCHAR(50) NOT NULL,
    jar_path TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## 🔐 Security

### Authentication & Authorization

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .oauth2Login(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(server -> server
                .jwt(Customizer.withDefaults())
            );
        return http.build();
    }
}
```

### Row-Level Security

```sql
-- Enable RLS on PostgreSQL
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;

-- Policy: Users see only their org's reviews
CREATE POLICY reviews_org_policy ON reviews
    USING (organization_id = current_setting('app.current_org')::BIGINT);
```

---

## 🧪 Testing

### Unit Tests

```bash
./mvnw test
```

### Integration Tests

```bash
./mvnw verify -P integration-test
```

### Testcontainers

```java
@SpringBootTest
@Testcontainers
class ReviewServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("pullwise")
        .withUsername("pullwise")
        .withPassword("secret");

    @Test
    void shouldGenerateReview() {
        // Test with real database
    }
}
```

---

## 🚀 Deployment

### Production Profile

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### Docker

```bash
docker build -t pullwise-backend .
docker run -p 8080:8080 pullwise-backend
```

### Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pullwise-backend
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: backend
        image: pullwise/backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
```

---

## 📊 Monitoring & Observability

### Metrics (Prometheus)

```java
@Component
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public void recordReviewGenerated(String language) {
        Counter.builder("pullwise.reviews.generated")
            .tag("language", language)
            .register(meterRegistry)
            .increment();
    }
}
```

### Health Checks

```
GET /actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "rabbitmq": { "status": "UP" }
  }
}
```

---

## 🤝 Contributing

We'd love your help! Here are areas we need support:

### Priority Areas

- [ ] **Language Integrations** - Add support for more SAST tools
- [ ] **Plugin Development** - Create new plugins
- [ ] **LLM Providers** - Add more model providers
- [ ] **Performance** - Optimize review pipeline
- [ ] **Testing** - Increase test coverage

### Getting Started

1. **Setup development environment**
   ```bash
   cd backend
   cp .env.example .env
   docker-compose up -d  # Start dependencies
   ```

2. **Build and run**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Write code**
   - Follow our [Code Style Guide](../docs/CODE_STYLE.md)
   - Add tests for new features
   - Update documentation

4. **Submit PR**
   - Describe what you changed and why
   - Link related issues
   - Ensure CI passes

---

## 📝 License

MIT License - see [LICENSE](../LICENSE) for details.

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/integralltech/pullwise-ai/issues)
- **Discord**: [Join Community](https://discord.gg/pullwise)
- **Email**: [hello@pullwise.ai](mailto:hello@pullwise.ai)
