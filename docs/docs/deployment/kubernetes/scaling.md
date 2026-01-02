# Kubernetes Scaling

Configure horizontal and vertical scaling for Pullwise.

## Overview

Pullwise can scale automatically based on load using Kubernetes HPA, VPA, or KEDA.

## Horizontal Pod Autoscaler (HPA)

### Basic HPA

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: pullwise-hpa
  namespace: pullwise
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: pullwise
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### Multi-Metric HPA

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: pullwise-hpa
  namespace: pullwise
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: pullwise
  minReplicas: 3
  maxReplicas: 20
  metrics:
  # CPU-based scaling
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  # Memory-based scaling
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  # Custom metric (requests per second)
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "100"
  # Custom metric (queue length)
  - type: External
    external:
      metric:
        name: rabbitmq_queue_messages
      target:
        type: AverageValue
        averageValue: "50"
```

### Scaling Behavior

```yaml
spec:
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
      - type: Pods
        value: 2
        periodSeconds: 60
      selectPolicy: Min
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 30
      - type: Pods
        value: 4
        periodSeconds: 60
      selectPolicy: Max
```

## Vertical Pod Autoscaler (VPA)

### VPA Configuration

```yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: pullwise-vpa
  namespace: pullwise
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: pullwise
  updatePolicy:
    updateMode: "Auto"
  resourcePolicy:
    containerPolicies:
    - containerName: pullwise
      minAllowed:
        cpu: 250m
        memory: 256Mi
      maxAllowed:
        cpu: 4000m
        memory: 4Gi
      controlledResources: ["cpu", "memory"]
      controlledValues: RequestsAndLimits
```

### VPA Update Modes

| Mode | Description |
|------|-------------|
| `Off` | VPA only recommends, doesn't apply |
| `Initial` | Sets resource on pod creation |
| `Recreate` | Recreates pods when resources change |
| `Auto` | Updates in-place if possible, otherwise recreates |

## KEDA (Event-Driven Scaling)

### RabbitMQ Queue Scaling

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: pullwise-rabbitmq-scaler
  namespace: pullwise
spec:
  scaleTargetRef:
    name: pullwise
  minReplicaCount: 3
  maxReplicaCount: 20
  triggers:
  - type: rabbitmq
    metadata:
      protocol: amqp
      queueName: reviews
      mode: QueueSize
      value: "10"
      hostFromEnv: RABBITMQ_HOST
      vhostName: /
    authenticationRef:
      name: rabbitmq-trigger-auth
```

### Redis Scaling

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: pullwise-redis-scaler
  namespace: pullwise
spec:
  scaleTargetRef:
    name: pullwise
  minReplicaCount: 2
  maxReplicaCount: 10
  triggers:
  - type: redis
    metadata:
      addressFromEnv: REDIS_ADDRESS
      listName: review:queue
      listLength: "5"
      enableTLS: "false"
```

### Cron-Based Scaling

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: pullwise-cron-scaler
  namespace: pullwise
spec:
  scaleTargetRef:
    name: pullwise
  minReplicaCount: 2
  maxReplicaCount: 10
  triggers:
  - type: cron
    metadata:
      timezone: America/New_York
      start: 0 9 * * *
      end: 0 17 * * *
      desiredReplicas: "10"
  - type: cron
    metadata:
      timezone: America/New_York
      start: 0 17 * * *
      end: 0 9 * * *
      desiredReplicas: "3"
```

## Cluster Autoscaler

### Node Auto Scaling

```yaml
# Enable cluster autoscaler on node pool
apiVersion: autoscaling.k8s.io/v1
kind: ClusterAutoscaler
metadata:
  name: cluster-autoscaler
  namespace: kube-system
spec:
  scaleDownEnabled: true
  scaleDownUnneededTime: 10m
  scaleDownUtilizationThreshold: 0.5
  maxNodesPerNamespace: 100
  skipNodesWithLocalStorage: true
  skipNodesWithSystemPods: true
```

### AWS ASG Labels

```yaml
# Node group with ASG labels
apiVersion: v1
kind: Node
metadata:
  labels:
    role: pullwise-worker
  annotations:
    cluster-autoscaler.kubernetes.io/safe-to-evict: "true"
spec:
  taints:
  - key: dedicated
    value: pullwise
    effect: NoSchedule
```

## Performance Tuning

### Resource Optimization

```yaml
# Optimize based on actual usage
resources:
  requests:
    cpu: 500m      # Based on average usage
    memory: 512Mi  # Based on average usage
  limits:
    cpu: 2000m     # Based on peak usage
    memory: 2Gi    # Based on peak usage
```

### JVM Tuning

```yaml
env:
  # Heap sizing
  - name: JAVA_OPTS
    value: "-Xms512m -Xmx1536m"

  # GC settings
  - name: JAVA_OPTS
    value: "-XX:+UseG1GC -XX:MaxGCPauseMillis=200"

  # Container awareness
  - name: JAVA_OPTS
    value: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
```

### Database Pooling

```yaml
env:
  # HikariCP settings
  - name: SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE
    value: "20"
  - name: SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE
    value: "5"
  - name: SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT
    value: "30000"
```

## Monitoring Scaling

### Metrics to Monitor

```bash
# HPA status
kubectl get hpa -n pullwise

# HPA description
kubectl describe hpa pullwise-hpa -n pullwise

# Current metrics
kubectl top pods -n pullwise
kubectl top nodes
```

### Prometheus Alerts

```yaml
# Alerting rules for scaling
groups:
- name: pullwise_scaling
  rules:
  - alert: PullwiseAtMaxReplicas
    expr: |
      kube_hpa_status_current_replicas{namespace="pullwise"}
      /
      kube_hpa_spec_max_replicas{namespace="pullwise"} >= 0.9
    for: 10m
    annotations:
      summary: "HPA at max replicas"
      description: "Pullwise HPA has been at max replicas for 10 minutes"

  - alert: PullwiseHighCPU
    expr: |
      rate(container_cpu_usage_seconds_total{namespace="pullwise"}[5m])
      > 0.8
    for: 5m
    annotations:
      summary: "High CPU usage"
      description: "Pullwise pods using >80% CPU for 5 minutes"
```

## Scaling Strategies

### Time-Based Scaling

Scale up during business hours:

```yaml
# HPA with cron-based min/max
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: pullwise-hpa-business-hours
  namespace: pullwise
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: pullwise
  minReplicas: 5  # Higher during day
  maxReplicas: 20
```

### Queue-Based Scaling

Scale based on RabbitMQ queue length:

```yaml
triggers:
- type: rabbitmq
  metadata:
    queueName: reviews
    mode: QueueSize
    value: "50"    # Scale up when 50+ messages
    activationValue: "10"  # Minimum for scaling
```

### Predictive Scaling

Use historical data to predict load:

```yaml
# Pre-scale before expected load
- type: cron
  metadata:
    timezone: America/New_York
    start: "0 8 * * 1-5"  # 8 AM weekdays
    desiredReplicas: "10"
    end: "0 18 * * 1-5"   # 6 PM weekdays
    desiredReplicas: "3"
```

## Best Practices

### 1. Set Appropriate Limits

```yaml
# Good: Balanced requests/limits
resources:
  requests:
    cpu: 500m
    memory: 512Mi
  limits:
    cpu: 2000m
    memory: 2Gi

# Bad: Requests equal limits (no bursting)
resources:
  requests:
    cpu: 2000m
    memory: 2Gi
  limits:
    cpu: 2000m
    memory: 2Gi
```

### 2. Use Liveness/Readiness Probes

```yaml
# Prevents killing healthy pods
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 30
  failureThreshold: 3

# Prevents routing to unready pods
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 3
```

### 3. Configure Pod Disruption Budget

```yaml
# Ensure availability during updates
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: pullwise-pdb
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: pullwise
```

### 4. Monitor Resource Usage

```bash
# Regular review of usage
kubectl top pods -n pullwise --sort-by=cpu
kubectl top pods -n pullwise --sort-by=memory

# Adjust based on actual usage
# Update requests/limits accordingly
```

## Next Steps

- [Helm](/docs/deployment/kubernetes/helm) - Helm deployment
- [Manifests](/docs/deployment/kubernetes/manifests) - Raw manifests
- [Monitoring](/docs/deployment/monitoring/prometheus) - Prometheus setup
