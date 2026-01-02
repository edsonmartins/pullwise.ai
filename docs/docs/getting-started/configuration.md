# Configuration

Customize Pullwise to fit your workflow.

## Configuration Overview

Pullwise can be configured through:

1. **Environment Variables** - For deployment settings
2. **Application YAML** - For Spring Boot configuration
3. **Project Settings** - UI-based configuration per project
4. **Database** - Persistent configuration storage

## Environment Variables

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | `prod` |
| `SPRING_DATASOURCE_URL` | Database JDBC URL | `jdbc:postgresql://localhost:5432/pullwise` |
| `SPRING_DATASOURCE_USERNAME` | Database user | `pullwise` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `your-secure-password` |
| `JWT_SECRET` | JWT signing secret | Min 256 bits random string |

### Authentication

```bash
# GitHub OAuth (recommended)
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret

# OR Demo user (dev only)
DEMO_USER_ENABLED=true
DEMO_USER_EMAIL=demo@pullwise.ai
DEMO_USER_PASSWORD=demo123
```

### LLM Provider

```bash
# OpenRouter (cloud)
OPENROUTER_API_KEY=sk-or-v1-your-key-here
LLM_MODEL=anthropic/claude-3.5-sonnet

# OR Ollama (local)
OLLAMA_BASE_URL=http://localhost:11434
LLM_MODEL=llama3
```

### Cache & Queue

```bash
# Redis
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=your-redis-password

# RabbitMQ (optional)
SPRING_RABBITMQ_HOST=rabbitmq
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=pullwise
SPRING_RABBITMQ_PASSWORD=your-rabbitmq-password
```

## Application Configuration

### application.yml Structure

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:prod}

  # Database
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  # JPA
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  # Redis
  redis:
    host: ${SPRING_REDIS_HOST:localhost}
    port: ${SPRING_REDIS_PORT:6379}
    password: ${SPRING_REDIS_PASSWORD:}
    timeout: 5000
    lettuce:
      pool:
        max-active: 20
        max-idle: 10

  # RabbitMQ
  rabbitmq:
    host: ${SPRING_RABBITMQ_HOST:localhost}
    port: ${SPRING_RABBITMQ_PORT:5672}
    username: ${SPRING_RABBITMQ_USERNAME:guest}
    password: ${SPRING_RABBITMQ_PASSWORD:guest}

# Server
server:
  port: ${SERVER_PORT:8080}
  compression:
    enabled: true
  http2:
    enabled: true

# Pullwise Configuration
pullwise:
  # Security
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 86400000  # 24 hours

  # GitHub
  github:
    client-id: ${GITHUB_CLIENT_ID}
    client-secret: ${GITHUB_CLIENT_SECRET}
    webhook-secret: ${GITHUB_WEBHOOK_SECRET:}

  # LLM Configuration
  llm:
    openrouter:
      api-key: ${OPENROUTER_API_KEY}
      base-url: https://openrouter.ai/api/v1
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
    default-model: ${LLM_MODEL:anthropic/claude-3.5-sonnet}
    timeout: 300000  # 5 minutes
    max-tokens: 4000

  # Review Configuration
  review:
    max-file-size: 1048576  # 1MB
    max-total-size: 10485760  # 10MB
    parallel-sast: true
    include-vendor-dirs: false
    excluded-paths:
      - node_modules/**
      - vendor/**
      - target/**
      - build/**

  # Auto-Fix
  autofix:
    enabled: true
    require-approval: true
    max-suggestions: 10
    create-branch: true

  # Plugins
  plugins:
    directory: /opt/pullwise/plugins
    auto-discover: true
    timeout: 60000  # 1 minute

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
```

## Project Configuration

Configure per-project settings via UI or API:

### Review Settings

```yaml
# API endpoint: PUT /api/configurations/projects/{projectId}

configurations:
  # Which checks to run
  sast:
    enabled: true
    tools:
      sonarqube: true
      eslint: true
      checkstyle: true

  llm:
    enabled: true
    model: anthropic/claude-3.5-sonnet
    temperature: 0.3

  # What to analyze
  scope:
    include:
      - "src/**/*.java"
      - "src/**/*.ts"
    exclude:
      - "src/test/**"
      - "**/*.test.ts"

  # Severity thresholds
  thresholds:
    block_on_critical: true
    block_on_high: false
```

### Custom Rules

```yaml
configurations:
  # Custom patterns to detect
  custom_rules:
    - name: "No TODO comments"
      pattern: "TODO:"
      severity: "LOW"
      message: "Please create an issue instead of leaving TODO"

    - name: "No console.log"
      pattern: "console\\.log\\("
      severity: "LOW"
      language: TYPESCRIPT
```

## Organization Configuration

Configure default settings for all projects:

```yaml
# API endpoint: PUT /api/configurations/organizations/{orgId}

configurations:
  # Default LLM model
  llm:
    default_model: anthropic/claude-3.5-sonnet
    fallback_model: openai/gpt-4

  # Review defaults
  review:
    auto_trigger_on_pr: true
    require_approval_for_merge: false
    post_summary_as_comment: true

  # Notification settings
  notifications:
    slack_webhook: https://hooks.slack.com/...
    notify_on_review_complete: true
    notify_on_critical_issues: true
```

## Profiles

### Development Profile

```yaml
# application-dev.yml
spring:
  jpa:
    show-sql: true
logging:
  level:
    com.pullwise: DEBUG
    org.springframework.security: DEBUG
pullwise:
  review:
    include-vendor-dirs: true
```

### Production Profile

```yaml
# application-prod.yml
spring:
  jpa:
    show-sql: false
logging:
  level:
    com.pullwise: INFO
    root: WARN
  file:
    name: /var/log/pullwise/application.log
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

## Configuration API

### Get Configuration

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/configurations/projects/{projectId}
```

### Update Configuration

```bash
curl -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  http://localhost:8080/api/configurations/projects/{projectId} \
  -d '{
    "configurations": [
      {
        "key": "sast.enabled",
        "value": "true"
      },
      {
        "key": "llm.model",
        "value": "anthropic/claude-3.5-sonnet"
      }
    ]
  }'
```

### Get Default Configuration

```bash
curl http://localhost:8080/api/configurations/defaults
```

## Security Configuration

### CORS

```yaml
# application.yml
spring:
  web:
    cors:
      allowed-origins:
        - https://pullwise.example.com
        - https://*.pullwise.example.com
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
        - OPTIONS
      allowed-headers: "*"
      allow-credentials: true
      max-age: 3600
```

### CSRF

```yaml
# Disable CSRF for API routes
spring:
  security:
    csrf:
      # Disabled for stateless API
      enabled: false
```

### Rate Limiting

```yaml
pullwise:
  security:
    rate-limit:
      enabled: true
      requests-per-minute: 100
      burst: 20
```

## Troubleshooting Configuration

### Validate Configuration

```bash
# Check active profile
curl http://localhost:8080/actuator/info

# Check health
curl http://localhost:8080/actuator/health

# Check configuration
curl http://localhost:8080/actuator/configprops
```

### Test Database Connection

```bash
# From within backend container
docker-compose exec backend \
  psql $SPRING_DATASOURCE_URL \
  -U $SPRING_DATASOURCE_USERNAME \
  -c "SELECT 1"
```

### Test Redis Connection

```bash
# From within backend container
docker-compose exec backend \
  redis-cli -h $SPRING_REDIS_HOST ping
```

## Next Steps

- [Troubleshooting](/docs/getting-started/troubleshooting) - Common issues
- [User Guide](/docs/category/user-guide) - Learn features
- [Deployment](/docs/category/deployment) - Production setup
