# Jaeger Tracing

Distributed tracing for Pullwise with Jaeger.

## Overview

Pullwise supports OpenTelemetry distributed tracing to track:
- Review request lifecycle
- Plugin execution
- LLM API calls
- Database queries
- External service calls

## Spring Boot Configuration

### Enable Tracing

```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 1.0  # Sample 100% of traces
  zipkin:
    tracing:
      endpoint: http://jaeger-collector:9411/api/v2/spans

spring:
  application:
    name: pullwise
  sleuth:
    zipkin:
      enabled: true
    reactor:
      instrumentation:
        enabled: true
```

### OpenTelemetry Configuration

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-zipkin</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

## Jaeger Deployment

### All-in-One (Development)

```yaml
# jaeger-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jaeger
  namespace: monitoring
spec:
  replicas: 1
  selector:
    matchLabels:
      app: jaeger
  template:
    metadata:
      labels:
        app: jaeger
    spec:
      containers:
      - name: jaeger
        image: jaegertracing/all-in-one:1.50
        ports:
        - containerPort: 5775  # accept zipkin.thrift over compact thrift protocol
        - containerPort: 6831  # accept zipkin.thrift over compact thrift protocol
        - containerPort: 6832  # accept zipkin.thrift over json
        - containerPort: 5778  # serve configs
        - containerPort: 16686  # serve frontend
        - containerPort: 14268  # accept zipkin.thrift directly
        - containerPort: 14250  # accept model.proto
        - containerPort: 9411   # Zipkin compatible endpoint
        - containerPort: 4317   # accept OTLP over grpc
        - containerPort: 4318   # accept OTLP over http
        env:
        - name: COLLECTOR_ZIPKIN_HOST_PORT
          value: ":9411"
        - name: SPAN_STORAGE_TYPE
          value: elasticsearch
        - name: ES_SERVER_URLS
          value: http://elasticsearch:9200
        options:
          sampling:
            initial-sampling-probability: "1.0"
---
apiVersion: v1
kind: Service
metadata:
  name: jaeger
  namespace: monitoring
spec:
  selector:
    app: jaeger
  ports:
  - name: ui
    port: 16686
    targetPort: 16686
  - name: collector
    port: 9411
    targetPort: 9411
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: jaeger
  namespace: monitoring
spec:
  ingressClassName: nginx
  rules:
  - host: jaeger.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: jaeger
            port:
              number: 16686
```

### Production with Elasticsearch

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: jaeger-config
  namespace: monitoring
data:
  span-storage-type: elasticsearch
  es-server-urls: http://elasticsearch:9200
  es.index-date-separator: "-"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jaeger-collector
  namespace: monitoring
spec:
  replicas: 2
  selector:
    matchLabels:
      app: jaeger-collector
  template:
    metadata:
      labels:
        app: jaeger-collector
    spec:
      containers:
      - name: jaeger-collector
        image: jaegertracing/jaeger-collector:1.50
        ports:
        - containerPort: 9411
        - containerPort: 14250
        args:
        - "--es.server-urls=http://elasticsearch:9200"
        - "--es.tags-as-fields.all=true"
        - "--sampling.initial-sampling-probability=1.0"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jaeger-query
  namespace: monitoring
spec:
  replicas: 2
  selector:
    matchLabels:
      app: jaeger-query
  template:
    metadata:
      labels:
        app: jaeger-query
    spec:
      containers:
      - name: jaeger-query
        image: jaegertracing/jaeger-query:1.50
        ports:
        - containerPort: 16686
        - containerPort: 16685
        args:
        - "--es.server-urls=http://elasticsearch:9200"
```

## Custom Spans

### Create Span

```java
@Service
public class ReviewService {

    private final Tracer tracer;

    public ReviewService(Tracer tracer) {
        this.tracer = tracer;
    }

    public Review executeReview(ReviewRequest request) {
        // Create span
        Span span = tracer.nextSpan()
            .name("executeReview")
            .tag("project.id", request.getProjectId())
            .tag("branch", request.getBranch())
            .start();

        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            // Business logic
            Review review = doReview(request);

            span.tag("review.id", review.getId());
            span.tag("issues.count", review.getIssues().size());

            return review;

        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### Tagging Spans

```java
public class PluginExecutor {

    public AnalysisResult execute(Plugin plugin, AnalysisRequest request) {
        Span span = tracer.nextSpan()
            .name("plugin.execute")
            .tag("plugin.id", plugin.getId())
            .tag("plugin.name", plugin.getName())
            .tag("plugin.type", plugin.getType().name())
            .tag("language", request.getLanguage().name())
            .start();

        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            AnalysisResult result = plugin.analyze(request);

            span.tag("result.issues", result.getIssues().size());
            span.tag("result.success", result.isSuccess());
            span.tag("result.duration_ms", result.getDuration());

            return result;

        } finally {
            span.end();
        }
    }
}
```

### Baggage Propagation

```java
// Set baggage
public void handleRequest(Request request) {
    Span span = tracer.currentSpan();
    span.setBaggageItem("user.id", request.getUserId());
    span.setBaggageItem("request.id", request.getId());

    // Baggage propagates to downstream calls
    processRequest(request);
}

// Read baggage
public void processRequest(Request request) {
    String userId = tracer.currentSpan()
        .getBaggageItem("user.id");
}
```

## Span Relationships

### Child Spans

```java
public ReviewPipeline execute(ReviewRequest request) {
    Span parentSpan = tracer.nextSpan()
        .name("review.pipeline")
        .start();

    try (Tracer.SpanInScope ws = tracer.withSpan(parentSpan)) {

        // Child span for SAST
        AnalysisResult sastResult = withChildSpan("sast.pass", () ->
            sastAnalyzer.analyze(request)
        );

        // Child span for LLM
        AnalysisResult llmResult = withChildSpan("llm.pass", () ->
            llmAnalyzer.analyze(request, sastResult)
        );

        // Child span for consolidation
        Review review = withChildSpan("consolidate", () ->
            consolidator.merge(sastResult, llmResult)
        );

        return review;

    } finally {
        parentSpan.end();
    }
}

private <T> T withChildSpan(String name, Supplier<T> action) {
    Span childSpan = tracer.nextSpan()
        .name(name)
        .start();

    try (Tracer.SpanInScope ws = tracer.withSpan(childSpan)) {
        return action.get();
    } finally {
        childSpan.end();
    }
}
```

## Tracing LLM Calls

### Trace External API

```java
@Component
public class LLMClient {

    private final WebClient webClient;
    private final Tracer tracer;

    public LLMResponse call(LLMRequest request) {
        Span span = tracer.nextSpan()
            .name("llm.request")
            .tag("llm.provider", request.getProvider())
            .tag("llm.model", request.getModel())
            .tag("llm.input_tokens", request.getTokens())
            .start();

        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            LLMResponse response = webClient.post()
                .uri(request.getEndpoint())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(LLMResponse.class)
                .block();

            span.tag("llm.output_tokens", response.getTokens());
            span.tag("llm.duration_ms", response.getDuration());

            return response;

        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

## Trace Queries

### Jaeger UI Queries

```
# Search traces
service: pullwise
operation: executeReview
tags: project.id="123"
duration: 5m-10m

# Find slow traces
service: pullwise
duration: 30s-

# Find errors
service: pullwise
tags: error=true
```

### API Queries

```bash
# Get traces for service
curl -X GET \
  "http://jaeger:16686/api/traces?service=pullwise&limit=20"

# Get trace by ID
curl -X GET \
  "http://jaeger:16686/api/traces/{trace-id}"

# Search traces
curl -X GET \
  "http://jaeger:16686/api/traces?service=pullwise&operation=executeReview&lookback=1h"
```

## Performance Analysis

### Identify Slow Operations

```java
@NewSpan("database.query")
public List<Review> findByProject(Long projectId) {
    return reviewRepository.findByProjectId(projectId);
}
```

### Trace Database Queries

```yaml
# Enable SQL tracing
spring:
  datasource:
    hikari:
      metric-tracker: com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTracker
```

## Sampling Strategy

### Configurable Sampling

```yaml
# Development: Sample all
management:
  tracing:
    sampling:
      probability: 1.0

# Production: Sample 10%
management:
  tracing:
    sampling:
      probability: 0.1
```

### Custom Sampler

```java
@Configuration
public class TracingConfig {

    @Bean
    Sampler defaultSampler() {
        // Sample based on operation
        return SpanId::randomLong; // Or implement custom logic
    }
}
```

## Best Practices

### 1. Use Meaningful Span Names

```java
// Good: Descriptive
tracer.nextSpan().name("plugin.execute.sast")

// Bad: Generic
tracer.nextSpan().name("execute")
```

### 2. Add Relevant Tags

```java
span.tag("plugin.id", plugin.getId());
span.tag("issues.count", result.getIssues().size());
span.tag("duration.ms", duration);
```

### 3. Always Close Spans

```java
try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
    // Do work
} finally {
    span.end();  // Always executed
}
```

### 4. Use Trace Context

```java
// Include trace ID in logs
String traceId = tracer.currentSpan().context().traceId();
log.info("Processing review, traceId={}", traceId);
```

## Troubleshooting

### Missing Spans

```bash
# Check Jaeger is receiving spans
curl http://jaeger:16686/api/services

# Check application configuration
curl http://pullwise:8080/actuator/tracing
```

### Sampling Issues

```yaml
# Increase sampling for debugging
management:
  tracing:
    sampling:
      probability: 1.0
```

### Performance Impact

```java
// Use async reporting
spring:
  sleuth:
    zipkin:
      sender:
        type: web
      messageTimeout: 1s
```

## Next Steps

- [Prometheus](/docs/deployment/monitoring/prometheus) - Prometheus setup
- [Grafana](/docs/deployment/monitoring/grafana) - Grafana dashboards
- [Production](/docs/deployment/docker/production) - Production setup
