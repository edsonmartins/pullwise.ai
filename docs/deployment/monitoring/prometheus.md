# Prometheus Metrics

Configure Prometheus monitoring for Pullwise.

## Overview

Pullwise exposes metrics at `/actuator/prometheus` compatible with Prometheus.

## Metrics Endpoints

### Available Endpoints

```
/actuator/health          - Health status
/actuator/health/liveness - Liveness probe
/actuator/health/readiness - Readiness probe
/actuator/prometheus       - Prometheus metrics
/actuator/metrics         - Micrometer metrics
```

### Key Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `jvm_memory_used_bytes` | Gauge | JVM memory usage |
| `jvm_gc_pause_seconds` | Summary | GC pause times |
| `http_server_requests` | Timer | HTTP request metrics |
| `database_connections` | Gauge | Database pool metrics |
| `review_total` | Counter | Total reviews |
| `review_duration_seconds` | Summary | Review duration |
| `llm_requests_total` | Counter | LLM API calls |
| `plugin_executions_total` | Counter | Plugin runs |

## Spring Boot Configuration

### Enable Prometheus

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,metrics
      base-path: /actuator
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: pullwise
      environment: ${SPRING_PROFILES_ACTIVE}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5,0.95,0.99
```

### Custom Metrics

```java
@Component
public class ReviewMetrics {

    private final Counter reviewCounter;
    private final Counter issueCounter;
    private final Timer reviewTimer;

    public ReviewMetrics(MeterRegistry registry) {
        this.reviewCounter = Counter.builder("review_total")
            .description("Total number of reviews")
            .tag("status", "completed")
            .register(registry);

        this.issueCounter = Counter.builder("issues_total")
            .description("Total issues found")
            .tag("severity", "all")
            .register(registry);

        this.reviewTimer = Timer.builder("review_duration_seconds")
            .description("Review duration")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }

    public void recordReviewCompleted() {
        reviewCounter.increment();
    }

    public void recordIssueFound(Severity severity) {
        Counter.builder("issues_total")
            .tag("severity", severity.toString())
            .register(Metrics.globalRegistry)
            .increment();
    }

    public Timer.Sample startReview() {
        return Timer.start();
    }

    public void stopReview(Timer.Sample sample) {
        sample.stop(reviewTimer);
    }
}
```

## Prometheus Configuration

### ServiceMonitor

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: pullwise
  namespace: pullwise
  labels:
    app: pullwise
spec:
  selector:
    matchLabels:
      app: pullwise
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
    scrapeTimeout: 10s
```

### Prometheus Config

```yaml
# prometheus.yml
global:
  scrape_interval: 30s
  evaluation_interval: 30s

scrape_configs:
  - job_name: 'pullwise'
    kubernetes_sd_configs:
    - role: pod
      namespaces:
        names:
        - pullwise
    relabel_configs:
    - source_labels: [__meta_kubernetes_pod_label_app]
      action: keep
      regex: pullwise
    - source_labels: [__meta_kubernetes_pod_ip]
      target_label: __address__
      replacement: $1:8080
    - source_labels: [__meta_kubernetes_namespace]
      target_label: namespace
```

## Grafana Dashboards

### Import Dashboard

```bash
# Pullwise provides pre-built dashboards
# Import via Grafana UI: Dashboards → Import → Upload JSON
# Dashboard ID: 12345 (example)
```

### Example Dashboard JSON

```json
{
  "dashboard": {
    "title": "Pullwise Metrics",
    "tags": ["pullwise", "code-review"],
    "timezone": "browser",
    "panels": [
      {
        "title": "Reviews per Minute",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(review_total[1m])",
            "legendFormat": "Reviews/sec"
          }
        ]
      },
      {
        "title": "Review Duration",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, review_duration_seconds_bucket)",
            "legendFormat": "p95"
          },
          {
            "expr": "review_duration_seconds_avg",
            "legendFormat": "average"
          }
        ]
      },
      {
        "title": "Issues by Severity",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(issues_total{severity=\"CRITICAL\"}[5m])",
            "legendFormat": "Critical"
          },
          {
            "expr": "rate(issues_total{severity=\"HIGH\"}[5m])",
            "legendFormat": "High"
          },
          {
            "expr": "rate(issues_total{severity=\"MEDIUM\"}[5m])",
            "legendFormat": "Medium"
          }
        ]
      },
      {
        "title": "Database Pool Usage",
        "type": "gauge",
        "targets": [
          {
            "expr": "hikaricp_connections_active / hikaricp_connections_max * 100",
            "legendFormat": "Pool Usage %"
          }
        ]
      },
      {
        "title": "JVM Memory",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\"}",
            "legendFormat": "Heap Used"
          },
          {
            "expr": "jvm_memory_max_bytes{area=\"heap\"}",
            "legendFormat": "Heap Max"
          }
        ]
      },
      {
        "title": "LLM Requests",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(llm_requests_total[1m])",
            "legendFormat": "Requests/sec"
          },
          {
            "expr": "rate(llm_errors_total[1m])",
            "legendFormat": "Errors/sec"
          }
        ]
      }
    ]
  }
}
```

## Alerting Rules

### Prometheus Alerts

```yaml
# alerts.yml
groups:
- name: pullwise
  interval: 30s
  rules:
  - alert: PullwiseHighErrorRate
    expr: |
      rate(http_server_requests_seconds_count{status=~"5.."}[5m])
      /
      rate(http_server_requests_seconds_count[5m])
      > 0.05
    for: 5m
    annotations:
      summary: "High error rate"
      description: "Error rate is {{ $value | humanizePercentage }}"
    labels:
      severity: warning

  - alert: PullwiseSlowReviews
    expr: |
      histogram_quantile(0.95, review_duration_seconds_bucket) > 300
    for: 10m
    annotations:
      summary: "Slow reviews"
      description: "p95 review duration is {{ $value }}s"
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
      severity: critical

  - alert: PullwiseHighGCPause
    expr: |
      rate(jvm_gc_pause_seconds_sum[5m]) > 0.1
    for: 5m
    annotations:
      summary: "High GC pause time"
      description: "GC pause rate is {{ $value }}s/s"
    labels:
      severity: warning

  - alert: PullwiseLLMErrorRate
    expr: |
      rate(llm_requests_total{status="error"}[5m])
      /
      rate(llm_requests_total[5m])
      > 0.1
    for: 5m
    annotations:
      summary: "High LLM error rate"
      description: "LLM error rate is {{ $value | humanizePercentage }}"
    labels:
      severity: critical
```

## Recording Rules

### Aggregate Rules

```yaml
# recording-rules.yml
groups:
- name: pullwise_aggregates
  interval: 30s
  rules:
  # Review rates
  - record: job:pullwise:reviews:rate5m
    expr: sum(rate(review_total[5m])) by (job)

  # Issue counts by severity
  - record: job:pullwise:issues:critical
    expr: sum(issues_total{severity="CRITICAL"}) by (job)

  - record: job:pullwise:issues:high
    expr: sum(issues_total{severity="HIGH"}) by (job)

  # Request rates
  - record: job:pullwise:http_requests:rate5m
    expr: sum(rate(http_server_requests_seconds_count[5m])) by (job, status)

  # Duration quantiles
  - record: job:pullwise:review_duration:p95
    expr: histogram_quantile(0.95, sum(rate(review_duration_seconds_bucket[5m])) by (le))

  # Database metrics
  - record: job:pullwise:db:pool_utilization
    expr: hikaricp_connections_active / hikaricp_connections_max * 100
```

## Custom Metrics

### Business Metrics

```java
@Service
public class MetricsService {

    private final MeterRegistry registry;

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
        registerGauges();
    }

    private void registerGauges() {
        // Active reviews gauge
        Gauge.builder("reviews.active", this, MetricsService::getActiveReviews)
            .description("Currently active reviews")
            .register(registry);

        // Queue size gauge
        Gauge.builder("reviews.queue_size", this, MetricsService::getQueueSize)
            .description("Reviews in queue")
            .register(registry);
    }

    public void recordReviewCompleted(String projectId, long duration) {
        Timer.builder("review.duration")
            .tag("project", projectId)
            .register(registry)
            .record(duration, TimeUnit.MILLISECONDS);
    }

    public void recordIssueFound(String rule, Severity severity) {
        Counter.builder("issues.found")
            .tag("rule", rule)
            .tag("severity", severity.name())
            .register(registry)
            .increment();
    }
}
```

### LLM Metrics

```java
@Component
public class LLMMetrics {

    private final Counter requestCounter;
    private final Counter errorCounter;
    private final Counter tokenCounter;
    private final Timer responseTimer;

    public LLMMetrics(MeterRegistry registry) {
        this.requestCounter = Counter.builder("llm.requests")
            .description("LLM API requests")
            .register(registry);

        this.errorCounter = Counter.builder("llm.errors")
            .description("LLM API errors")
            .register(registry);

        this.tokenCounter = Counter.builder("llm.tokens")
            .description("LLM tokens processed")
            .register(registry);

        this.responseTimer = Timer.builder("llm.response_time")
            .description("LLM response time")
            .register(registry);
    }

    public void recordRequest(String provider, String model) {
        Counter.builder("llm.requests")
            .tag("provider", provider)
            .tag("model", model)
            .register(Metrics.globalRegistry)
            .increment();
    }

    public void recordTokens(int inputTokens, int outputTokens) {
        Counter.builder("llm.tokens")
            .tag("type", "input")
            .register(Metrics.globalRegistry)
            .increment(inputTokens);

        Counter.builder("llm.tokens")
            .tag("type", "output")
            .register(Metrics.globalRegistry)
            .increment(outputTokens);
    }
}
```

## Monitoring Setup

### Helm Chart

```yaml
# values.yaml for prometheus operator
prometheus:
  prometheusSpec:
    retention: 15d
    retentionSize: 50GB
    resources:
      requests:
        cpu: 500m
        memory: 2Gi
      limits:
        cpu: 1000m
        memory: 4Gi
    storageSpec:
      volumeClaimTemplate:
        spec:
          storageClassName: fast-ssd
          accessModes: ["ReadWriteOnce"]
          resources:
            requests:
              storage: 100Gi

grafana:
  persistence:
    enabled: true
    size: 10Gi
  adminPassword: "change-me"
```

### Install via Helm

```bash
# Install kube-prometheus-stack
helm repo add prometheus-community \
  https://prometheus-community.github.io/helm-charts

helm install kube-prometheus prometheus-community/kube-prometheus-stack \
  -n monitoring --create-namespace \
  --values prometheus-values.yaml
```

## Best Practices

### 1. Label Everything

```java
// Good: Rich labels
Counter.builder("reviews")
    .tag("project", projectId)
    .tag("language", language)
    .tag("status", status)
    .register(registry);

// Bad: No labels
Counter.builder("reviews").register(registry);
```

### 2. Use Histograms for Durations

```java
// Good: Histogram
Timer.builder("review.duration")
    .publishPercentiles(0.5, 0.95, 0.99)
    .register(registry);

// Bad: Single gauge
gauge.set(duration);
```

### 3. Set Appropriate Buckets

```java
// Custom buckets for review duration
Timer.builder("review.duration")
    .serviceLevelObjectives(
        Duration.ofSeconds(10),
        Duration.ofSeconds(30),
        Duration.ofMinutes(1),
        Duration.ofMinutes(5)
    )
    .register(registry);
```

### 4. Monitor Cardinality

```bash
# Check metric cardinality
curl http://localhost:8080/actuator/prometheus | wc -l

# Alert on high cardinality
- alert: HighCardinality
  expr: count({__name__=~".+"}) by (__name__) > 10000
```

## Next Steps

- [Grafana](/docs/deployment/monitoring/grafana) - Grafana dashboards
- [Jaeger](/docs/deployment/monitoring/jaeger) - Distributed tracing
- [Production](/docs/deployment/docker/production) - Production setup
