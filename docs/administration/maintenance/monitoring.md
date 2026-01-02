# System Monitoring

Monitoring Pullwise health and performance.

## Overview

Comprehensive monitoring covers:
- Application metrics
- Database performance
- System resources
- User activity
- Error tracking

## Health Checks

### Endpoints

```bash
# Overall health
GET /actuator/health

# Liveness (is the app running?)
GET /actuator/health/liveness

# Readiness (can the app handle traffic?)
GET /actuator/health/readiness

# Detailed health
GET /actuator/health/db
GET /actuator/health/redis
GET /actuator/health/llm
```

### Health Response

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "SELECT 1"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0"
      }
    },
    "llm": {
      "status": "UP",
      "details": {
        "providers": ["openai", "anthropic"]
      }
    }
  }
}
```

## Key Metrics

### Application Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `review_total` | Counter | Total reviews |
| `review_duration_seconds` | Summary | Review duration |
| `issues_total` | Counter | Issues found |
| `llm_requests_total` | Counter | LLM API calls |
| `http_server_requests_seconds` | Timer | HTTP request times |
| `hikaricp_connections_active` | Gauge | DB connections |

### Database Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `hikaricp_connections_active` | Gauge | Active connections |
| `hikaricp_connections_idle` | Gauge | Idle connections |
| `hikaricp_connections_pending` | Gauge | Pending requests |
| `postgres_connections` | Gauge | Total connections |
| `postgres_stat_database_size` | Gauge | DB size in bytes |

### JVM Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `jvm_memory_used_bytes` | Gauge | Memory used |
| `jvm_gc_pause_seconds` | Summary | GC pause times |
| `jvm_threads_live` | Gauge | Thread count |
| `jvm_buffer_pool_count` | Gauge | Buffer pools |

## Alerting Rules

### Critical Alerts

```yaml
groups:
- name: pullwise-critical
  rules:
  - alert: PullwiseDown
    expr: up{job="pullwise"} == 0
    for: 2m
    annotations:
      summary: "Pullwise is down"
      description: "Pullwise has been down for more than 2 minutes"
    labels:
      severity: critical

  - alert: PullwiseHighErrorRate
    expr: |
      rate(http_server_requests_seconds_count{status=~"5.."}[5m])
      /
      rate(http_server_requests_seconds_count[5m]) > 0.05
    for: 5m
    annotations:
      summary: "High error rate"
      description: "Error rate is {{ $value | humanizePercentage }}"
    labels:
      severity: critical

  - alert: PullwiseDatabaseDown
    expr: up{job="postgresql"} == 0
    for: 2m
    annotations:
      summary: "Database is down"
      description: "PostgreSQL has been down for more than 2 minutes"
    labels:
      severity: critical
```

### Warning Alerts

```yaml
groups:
- name: pullwise-warnings
  rules:
  - alert: PullwiseSlowReviews
    expr: |
      histogram_quantile(0.95, review_duration_seconds_bucket) > 300
    for: 10m
    annotations:
      summary: "Slow reviews"
      description: "p95 review duration is {{ $value }}s"
    labels:
      severity: warning

  - alert: PullwiseHighGCPause
    expr: |
      rate(jvm_gc_pause_seconds_sum[5m]) > 0.1
    for: 5m
    annotations:
      summary: "High GC pause time"
      description: "GC pause rate is {{ $value }}s/s"
    labels:
      severity: warning

  - alert: PullwiseDatabasePoolExhausted
    expr: |
      hikaricp_connections_active / hikaricp_connections_max > 0.9
    for: 5m
    annotations:
      summary: "Database pool nearly full"
      description: "Pool usage is {{ $value | humanizePercentage }}"
    labels:
      severity: warning
```

## Dashboards

### Overview Dashboard

```json
{
  "name": "Pullwise Overview",
  "panels": [
    {
      "title": "Request Rate",
      "query": "sum(rate(http_server_requests_seconds_count[5m]))"
    },
    {
      "title": "Error Rate",
      "query": "sum(rate(http_server_requests_seconds_count{status=~'5..'}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))"
    },
    {
      "title": "P95 Latency",
      "query": "histogram_quantile(0.95, http_server_requests_seconds_bucket)"
    },
    {
      "title": "Active Reviews",
      "query": "reviews_active"
    }
  ]
}
```

### Database Dashboard

```json
{
  "name": "Database Health",
  "panels": [
    {
      "title": "Connection Pool Usage",
      "query": "hikaricp_connections_active / hikaricp_connections_max * 100"
    },
    {
      "title": "Query Duration (P95)",
      "query": "histogram_quantile(0.95, hikaricp_queries_seconds_bucket)"
    },
    {
      "title": "Database Size",
      "query": "postgres_stat_database_size_bytes"
    },
    {
      "title": "Transactions per Second",
      "query": "rate(pg_stat_database_xact_commit[1m])"
    }
  ]
}
```

## Logging

### Log Levels

```yaml
# application.yml
logging:
  level:
    com.pullwise: INFO
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Structured Logging

```java
@Slf4j
@Component
public class ReviewService {

    public Review execute(ReviewRequest request) {
        log.info("Starting review",
            "projectId", request.getProjectId(),
            "branch", request.getBranch(),
            "userId", SecurityContextHolder.getContext().getAuthentication().getName());

        try {
            Review result = doExecute(request);
            log.info("Review completed",
                "reviewId", result.getId(),
                "issues", result.getIssues().size());
            return result;
        } catch (Exception e) {
            log.error("Review failed",
                "projectId", request.getProjectId(),
                "error", e.getMessage());
            throw e;
        }
    }
}
```

### Log Aggregation

```yaml
# Loki configuration
loki:
  url: http://loki:3100/loki/api/v1/push

# Or Elasticsearch
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

## Monitoring Stack

### Prometheus + Grafana

```bash
# Install kube-prometheus-stack
helm repo add prometheus-community \
  https://prometheus-community.github.io/helm-charts

helm install kube-prometheus prometheus-community/kube-prometheus-stack \
  -n monitoring --create-namespace
```

### Loki + Promtail

```yaml
# promtail-config.yaml
server:
  http_listen_port: 9080

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
- job_name: pullwise-logs
  kubernetes_sd_configs:
  - role: pod
    namespaces:
      names:
      - pullwise
  relabel_configs:
  - source_labels: [__meta_kubernetes_pod_label_app]
    action: keep
    regex: pullwise
```

## Performance Monitoring

### Application Performance Monitoring (APM)

```java
@Configuration
public class MicrometerConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
            "application", "pullwise",
            "environment", env.getProperty("spring.profiles.active"),
            "region", env.getProperty("aws.region")
        );
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

@Timed(value = "review.execute", percentiles = {0.5, 0.95, 0.99})
public Review execute(ReviewRequest request) {
    // ...
}
```

### Custom Metrics

```java
@Service
public class MonitoringService {

    private final Counter reviewCounter;
    private final Gauge activeReviewsGauge;
    private final AtomicInteger activeReviews = new AtomicInteger(0);

    public MonitoringService(MeterRegistry registry) {
        this.reviewCounter = Counter.builder("review.total")
            .description("Total reviews")
            .register(registry);

        this.activeReviewsGauge = Gauge.builder("review.active", activeReviews, AtomicInteger::get)
            .description("Active reviews")
            .register(registry);
    }

    public void recordReviewStart() {
        activeReviews.incrementAndGet();
    }

    public void recordReviewComplete() {
        activeReviews.decrementAndGet();
        reviewCounter.increment();
    }
}
```

## User Activity Monitoring

### Activity Tracking

```sql
-- Track user actions
CREATE TABLE user_activity (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100),
    resource_type VARCHAR(50),
    resource_id BIGINT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_activity_user_id ON user_activity(user_id, created_at DESC);
```

### Analytics Queries

```sql
-- Reviews per user (30 days)
SELECT u.name, COUNT(*) as review_count
FROM user_activity ua
JOIN users u ON u.id = ua.user_id
WHERE ua.action = 'review.completed'
  AND ua.created_at > NOW() - INTERVAL '30 days'
GROUP BY u.id, u.name
ORDER BY review_count DESC;

-- Most active projects
SELECT p.name, COUNT(*) as review_count
FROM user_activity ua
JOIN projects p ON p.id = ua.resource_id::BIGINT
WHERE ua.action = 'review.completed'
  AND ua.resource_type = 'Review'
  AND ua.created_at > NOW() - INTERVAL '7 days'
GROUP BY p.id, p.name
ORDER BY review_count DESC;
```

## Notification Channels

### Slack Alerts

```yaml
# Alertmanager configuration
receivers:
- name: 'slack-critical'
  slack_configs:
  - api_url: 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
    channel: '#alerts-critical'
    title: '🚨 Critical Alert'
    text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

route:
  receiver: 'slack-critical'
  group_by: ['alertname', 'cluster']
  group_wait: 10s
  repeat_interval: 12h
```

### Email Alerts

```yaml
receivers:
- name: 'email'
  email_configs:
  - to: 'ops@example.com'
    from: 'alertmanager@example.com'
    smarthost: 'smtp.example.com:587'
    auth_username: 'alertmanager'
    auth_password: 'password'
```

## Best Practices

### 1. Monitor Everything

```java
// Add metrics to key operations
@Timed
@ExceptionCounter
public void execute() {
    // ...
}
```

### 2. Use Meaningful Labels

```java
Counter.builder("issues.found")
    .tag("severity", severity.toString())
    .tag("rule", rule)
    .tag("language", language)
    .register(registry);
```

### 3. Set Appropriate Thresholds

```yaml
# Avoid alert fatigue
- alert: HighErrorRate
  # 5% error rate sustained for 5 minutes
```

### 4. Correlate Metrics

```
Error rate up → Check latency
Latency up → Check database
Database slow → Check connection pool
```

## Troubleshooting

### High Memory Usage

```bash
# Check JVM memory
curl http://pullwise:8080/actuator/metrics/jvm.memory.used

# Check heap dump
kubectl exec -it pullwise-0 -n pullwise -- \
  jcmd 1 GC.heap_dump /tmp/heap.hprof

# Analyze with Eclipse MAT
```

### Slow Queries

```sql
-- Enable pg_stat_statements
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Find slow queries
SELECT query, calls, mean_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

### Connection Leaks

```bash
# Check HikariCP metrics
curl http://pullwise:8080/actuator/metrics/hikaricp.connections.active

# If high, check for leaks
jmap -dump:live,format=b,file=heap.hprof <pid>
```

## Next Steps

- [Backups](/docs/administration/maintenance/backups) - Backup procedures
- [Migrations](/docs/administration/maintenance/migrations) - Migration guide
- [Updates](/docs/administration/maintenance/updates) - Update procedures
