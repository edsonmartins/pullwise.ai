# Kubernetes Installation

Deploy Pullwise to Kubernetes using Helm charts or raw manifests.

## Prerequisites

- **Kubernetes** cluster (v1.24+)
- **kubectl** configured to access your cluster
- **Helm** 3.0+ (for Helm installation)

### Verify Cluster Access

```bash
kubectl cluster-info
kubectl get nodes
```

## Option 1: Helm Chart (Recommended)

### Add Pullwise Helm Repository

```bash
# Add the Pullwise Helm repository
helm repo add pullwise https://charts.pullwise.ai
helm repo update

# Search for available charts
helm search repo pullwise
```

### Install Pullwise

```bash
# Create namespace
kubectl create namespace pullwise

# Install with default values
helm install pullwise pullwise/pullwise-ce --namespace pullwise

# Install with custom values
helm install pullwise pullwise/pullwise-ce \
  --namespace pullwise \
  --set-file secrets.github.clientId=./github-client-id.txt \
  --set-file secrets.github.clientSecret=./github-client-secret.txt \
  --set secrets.jwtSecret="your-very-long-random-secret-key"
```

### Values Reference

```yaml title="values.yaml"
# Image configuration
image:
  backend:
    repository: pullwise/backend
    tag: "1.0.0"
    pullPolicy: IfNotPresent
  frontend:
    repository: pullwise/frontend
    tag: "1.0.0"
    pullPolicy: IfNotPresent

# Ingress configuration
ingress:
  enabled: true
  className: nginx
  hosts:
    - host: pullwise.example.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: pullwise-tls
      hosts:
        - pullwise.example.com

# Database configuration
postgresql:
  enabled: true
  auth:
    database: pullwise
    username: pullwise
    password: change-me
  primary:
    persistence:
      enabled: true
      size: 20Gi

# Redis configuration
redis:
  enabled: true
  auth:
    enabled: true
    password: change-me

# RabbitMQ configuration (optional)
rabbitmq:
  enabled: false
  auth:
    username: pullwise
    password: change-me

# Resource limits
resources:
  backend:
    requests:
      cpu: 500m
      memory: 1Gi
    limits:
      cpu: 2000m
      memory: 2Gi
  frontend:
    requests:
      cpu: 100m
      memory: 128Mi
    limits:
      cpu: 500m
      memory: 256Mi

# Autoscaling
autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

# Secrets
secrets:
  jwtSecret: ""
  github:
    clientId: ""
    clientSecret: ""
  openRouter:
    apiKey: ""

# Environment variables
env:
  llmModel: "anthropic/claude-3.5-sonnet"
  springProfilesActive: "prod"
```

### Upgrade Installation

```bash
# Upgrade with new values
helm upgrade pullwise pullwise/pullwise-ce \
  --namespace pullwise \
  -f values.yaml

# Rollback to previous version
helm rollback pullwise --namespace pullwise
```

### Uninstall

```bash
# Uninstall Pullwise
helm uninstall pullwise --namespace pullwise

# Delete namespace (optional)
kubectl delete namespace pullwise
```

## Option 2: Kubernetes Manifests

### Namespace and ConfigMaps

```yaml title="00-namespace.yaml"
apiVersion: v1
kind: Namespace
metadata:
  name: pullwise
```

```yaml title="01-configmap.yaml"
apiVersion: v1
kind: ConfigMap
metadata:
  name: pullwise-config
  namespace: pullwise
data:
  SPRING_PROFILES_ACTIVE: "prod"
  LLM_MODEL: "anthropic/claude-3.5-sonnet"
  VITE_API_URL: "https://pullwise.example.com/api"
  VITE_WS_URL: "wss://pullwise.example.com/ws"
```

### Secrets

```yaml title="02-secrets.yaml"
apiVersion: v1
kind: Secret
metadata:
  name: pullwise-secrets
  namespace: pullwise
type: Opaque
stringData:
  jwt-secret: "your-very-long-random-secret-key"
  github-client-id: "your-github-client-id"
  github-client-secret: "your-github-client-secret"
  openrouter-api-key: "sk-or-v1-your-key-here"
  postgres-password: "your-postgres-password"
  redis-password: "your-redis-password"
```

:::warning
Never commit secrets to version control. Use sealed secrets or external secret management.
:::

### PostgreSQL Deployment

```yaml title="10-postgres.yaml"
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: pullwise
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 20Gi
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: pullwise
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: pgvector/pgvector:pg16
        env:
        - name: POSTGRES_DB
          value: pullwise
        - name: POSTGRES_USER
          value: pullwise
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: pullwise-secrets
              key: postgres-password
        ports:
        - containerPort: 5432
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        livenessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - pullwise
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - pullwise
          initialDelaySeconds: 5
          periodSeconds: 5
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: pullwise
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
```

### Redis Deployment

```yaml title="20-redis.yaml"
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: pullwise
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        command:
        - redis-server
        - --appendonly
        - "yes"
        - --requirepass
        - $(REDIS_PASSWORD)
        env:
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: pullwise-secrets
              key: redis-password
        ports:
        - containerPort: 6379
        livenessProbe:
          exec:
            command:
            - redis-cli
            - ping
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - redis-cli
            - ping
          initialDelaySeconds: 5
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: pullwise
spec:
  selector:
    app: redis
  ports:
  - port: 6379
    targetPort: 6379
```

### Backend Deployment

```yaml title="30-backend.yaml"
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: pullwise
spec:
  replicas: 2
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      containers:
      - name: backend
        image: pullwise/backend:1.0.0
        envFrom:
        - configMapRef:
            name: pullwise-config
        env:
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://postgres:5432/pullwise"
        - name: SPRING_DATASOURCE_USERNAME
          value: pullwise
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: pullwise-secrets
              key: postgres-password
        - name: SPRING_REDIS_HOST
          value: redis
        - name: SPRING_REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: pullwise-secrets
              key: redis-password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: pullwise-secrets
              key: jwt-secret
        - name: GITHUB_CLIENT_ID
          valueFrom:
            secretKeyRef:
              name: pullwise-secrets
              key: github-client-id
        - name: GITHUB_CLIENT_SECRET
          valueFrom:
            secretKeyRef:
              name: pullwise-secrets
              key: github-client-secret
        - name: OPENROUTER_API_KEY
          valueFrom:
            secretKeyRef:
              name: pullwise-secrets
              key: openrouter-api-key
        ports:
        - containerPort: 8080
        resources:
          requests:
            cpu: 500m
            memory: 1Gi
          limits:
            cpu: 2000m
            memory: 2Gi
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: backend
  namespace: pullwise
spec:
  selector:
    app: backend
  ports:
  - port: 8080
    targetPort: 8080
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: backend-hpa
  namespace: pullwise
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### Frontend Deployment

```yaml title="40-frontend.yaml"
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
  namespace: pullwise
spec:
  replicas: 2
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
      - name: frontend
        image: pullwise/frontend:1.0.0
        envFrom:
        - configMapRef:
            name: pullwise-config
        ports:
        - containerPort: 80
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
          limits:
            cpu: 500m
            memory: 256Mi
        livenessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 5
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: frontend
  namespace: pullwise
spec:
  selector:
    app: frontend
  ports:
  - port: 80
    targetPort: 80
```

### Ingress

```yaml title="50-ingress.yaml"
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: pullwise-ingress
  namespace: pullwise
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - pullwise.example.com
    secretName: pullwise-tls
  rules:
  - host: pullwise.example.com
    http:
      paths:
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: backend
            port:
              number: 8080
      - path: /ws
        pathType: Prefix
        backend:
          service:
            name: backend
            port:
              number: 8080
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend
            port:
              number: 80
```

### Deploy Manifests

```bash
# Apply all manifests
kubectl apply -f k8s/

# Check deployment status
kubectl get all -n pullwise

# View logs
kubectl logs -f deployment/backend -n pullwise
```

## External Dependencies

### Using External PostgreSQL

```yaml
# Disable internal PostgreSQL
postgresql:
  enabled: false

# Configure external database
externalDatabase:
  host: external-postgres.example.com
  port: 5432
  user: pullwise
  password: your-password
  database: pullwise
```

### Using External Redis

```yaml
# Disable internal Redis
redis:
  enabled: false

# Configure external Redis
externalRedis:
  host: external-redis.example.com
  port: 6379
  password: your-password
```

## Monitoring

### Prometheus ServiceMonitor

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: backend
  namespace: pullwise
spec:
  selector:
    matchLabels:
      app: backend
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

## Troubleshooting

### Check Pod Status

```bash
# Get pod status
kubectl get pods -n pullwise

# Describe pod for events
kubectl describe pod <pod-name> -n pullwise

# View logs
kubectl logs <pod-name> -n pullwise
```

### Common Issues

**Pods in CrashLoopBackOff:**
```bash
# Check if pods can connect to database
kubectl exec -it <backend-pod> -n pullwise -- nc -zv postgres 5432
```

**Image Pull Errors:**
```bash
# Create image pull secret
kubectl create secret docker-registry regcred \
  --docker-server=your-registry \
  --docker-username=your-username \
  --docker-password=your-password \
  -n pullwise
```

## Next Steps

- [Scaling Configuration](/docs/deployment/kubernetes/scaling) - Configure HPA and VPA
- [Production Deployment](/docs/deployment/docker/production) - Production considerations
- [Monitoring Setup](/docs/deployment/monitoring/prometheus) - Prometheus and Grafana
