# Backend Architecture

Deep dive into Pullwise backend architecture.

## Architecture Overview

```mermaid
graph TB
    subgraph "API Layer"
        REST[REST Controllers]
        GraphQL[GraphQL Resolvers]
        WebSocket[WebSocket Handlers]
    end

    subgraph "Service Layer"
        ReviewService[Review Service]
        PluginService[Plugin Service]
        LLMService[LLM Service]
        SastService[SAST Service]
    end

    subgraph "Domain Layer"
        Review[Review Domain]
        Issue[Issue Domain]
        Plugin[Plugin Domain]
    end

    subgraph "Infrastructure Layer"
        JPA[JPA Repositories]
        Redis[Redis Cache]
        RabbitMQ[RabbitMQ Queue]
        Webhook[Webhook Clients]
    end

    REST --> Service Layer
    WebSocket --> Service Layer
    Service Layer --> Domain Layer
    Service Layer --> Infrastructure Layer
```

## Layered Architecture

### 1. API Layer (`infrastructure/rest`)

Handles HTTP requests and responses:

```java
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<ReviewDTO>> listReviews(
        @RequestParam(required = false) Long projectId
    ) {
        return ResponseEntity.ok(reviewService.findByProject(projectId));
    }

    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(
        @Valid @RequestBody CreateReviewRequest request
    ) {
        ReviewDTO review = reviewService.create(request);
        return ResponseEntity.status(201).body(review);
    }
}
```

### 2. Service Layer (`application/service`)

Business logic and orchestration:

```java
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PluginService pluginService;
    private final LLMRouterService llmRouter;

    @Transactional
    public Review create(CreateReviewRequest request) {
        // Create review entity
        Review review = Review.builder()
            .projectId(request.getProjectId())
            .branch(request.getBranch())
            .status(ReviewStatus.QUEUED)
            .build();

        review = reviewRepository.save(review);

        // Trigger async analysis
        triggerAnalysis(review);

        return review;
    }

    private void triggerAnalysis(Review review) {
        // Execute in background
        CompletableFuture.runAsync(() -> {
            executePipeline(review);
        });
    }
}
```

### 3. Domain Layer (`domain`)

Core business entities:

```java
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long projectId;
    private String branch;
    private String commitSha;

    @Enumerated(EnumType.STRING)
    private ReviewStatus status;

    @OneToMany(mappedBy = "review")
    private List<Issue> issues;

    // Getters, setters, builders
}
```

### 4. Infrastructure Layer (`infrastructure`)

External integrations:

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
        RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

## Core Services

### Review Pipeline Service

```java
@Service
@RequiredArgsConstructor
public class ReviewPipelineService {

    private final SastAnalysisPass sastPass;
    private final LLMReviewPass llmPass;
    private final ConsolidationPass consolidationPass;
    private final PrioritizationPass prioritizationPass;

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

### LLM Router Service

```java
@Service
@RequiredArgsConstructor
public class LLMRouterService {

    public LanguageModel selectModel(ReviewRequest request) {
        // Select based on:
        Language language = request.getLanguage();
        Complexity complexity = request.getComplexity();
        UserTier tier = request.getUserTier();

        // Cost optimization
        if (tier == UserTier.COMMUNITY) {
            return selectCheapestModel(language);
        }

        // Performance optimization
        if (complexity == Complexity.LOW) {
            return selectFastestModel(language);
        }

        // Quality optimization
        return selectBestModel(language);
    }
}
```

### Plugin Service

```java
@Service
@RequiredArgsConstructor
public class PluginService {

    private final PluginManager pluginManager;

    public AnalysisResult executePlugins(
        PluginType type,
        PluginLanguage language,
        AnalysisRequest request
    ) {
        List<CodeReviewPlugin> plugins =
            pluginManager.getPluginsByTypeAndLanguage(type, language);

        // Execute in parallel
        List<CompletableFuture<AnalysisResult>> futures =
            plugins.stream()
                .map(plugin -> CompletableFuture.supplyAsync(
                    () -> plugin.analyze(request),
                    executor
                ))
                .toList();

        // Wait for all and collect results
        List<AnalysisResult> results = futures.stream()
            .map(CompletableFuture::join)
            .toList();

        return consolidate(results);
    }
}
```

## Database Schema

### Key Tables

```sql
-- Reviews table
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    pull_request_id BIGINT,
    branch VARCHAR(255),
    commit_sha VARCHAR(40),
    status VARCHAR(31) NOT NULL,
    total_issues INTEGER DEFAULT 0,
    critical_issues INTEGER DEFAULT 0,
    high_issues INTEGER DEFAULT 0,
    medium_issues INTEGER DEFAULT 0,
    low_issues INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP
);

-- Issues table
CREATE TABLE issues (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT REFERENCES reviews(id),
    severity VARCHAR(15) NOT NULL,
    type VARCHAR(31) NOT NULL,
    rule VARCHAR(255),
    file_path TEXT NOT NULL,
    start_line INTEGER,
    end_line INTEGER,
    message TEXT,
    suggestion TEXT,
    status VARCHAR(31) DEFAULT 'PENDING',
    false_positive BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Plugins table
CREATE TABLE plugins (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    version VARCHAR(50) NOT NULL,
    jar_path TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);
```

## Security

### Spring Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .oauth2Login(oauth2 -> oauth2.loginPage("/login"))
            .oauth2ResourceServer(server -> server
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/webhooks/**", "/actuator/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
```

### JWT Authentication

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && jwtValidator.validate(token)) {
            Authentication auth = jwtValidator.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
```

## Async Processing

### RabbitMQ Configuration

```java
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    @Bean
    public Queue reviewQueue() {
        return QueueBuilder.durable("pullwise.reviews").build();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
```

### Review Job Consumer

```java
@Component
@RequiredArgsConstructor
public class ReviewJobConsumer {

    private final ReviewPipelineService pipelineService;

    @RabbitListener(queues = "pullwise.reviews")
    public void handleReviewJob(ReviewJob job) {
        try {
            pipelineService.execute(job);
        } catch (Exception e) {
            log.error("Review job failed", e);
            throw e; // Requeue for retry
        }
    }
}
```

## WebSocket

### WebSocket Configuration

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }
}
```

### Progress Updates

```java
@Service
public class ReviewProgressService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendProgress(Long reviewId, ReviewProgress progress) {
        messagingTemplate.convertAndSend(
            "/topic/reviews/" + reviewId + "/progress",
            progress
        );
    }
}
```

## Caching

### Redis Cache Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues();
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory)
            .cacheDefaults(cacheConfiguration())
            .build();
    }
}
```

## Monitoring

### Metrics

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

    public void recordReviewDuration(Duration duration) {
        Timer.builder("pullwise.reviews.duration")
            .register(meterRegistry)
            .record(duration);
    }
}
```

## Next Steps

- [Frontend Architecture](/docs/developer-guide/architecture/frontend-architecture) - Frontend architecture
- [Database](/docs/developer-guide/architecture/database) - Database details
- [Review Pipeline](/docs/developer-guide/architecture/review-pipeline) - Pipeline deep dive
