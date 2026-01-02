# Kubernetes Manifests

Raw Kubernetes YAML manifests for deployment.

## Overview

These manifests can be used directly without Helm. Customize values as needed for your environment.

## Namespace

```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: pullwise
  labels:
    name: pullwise
```

## ConfigMap

```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: pullwise-config
  namespace: pullwise
data:
  SPRING_PROFILES_ACTIVE: "prod"
  SPRING_DATASOURCE_URL: "jdbc:postgresql://pullwise-postgresql:5432/pullwise"
  SPRING_DATASOURCE_USERNAME: "pullwise"
  SPRING_REDIS_HOST: "pullwise-redis"
  SPRING_REDIS_PORT: "6379"
```

## Secret

```yaml
# secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: pullwise-secret
  namespace: pullwise
type: Opaque
stringData:
  SPRING_DATASOURCE_PASSWORD: "change-me"
  JWT_SECRET: "change-me-jwt-secret"
  GITHUB_CLIENT_ID: "your-github-client-id"
  GITHUB_CLIENT_SECRET: "your-github-client-secret"
  OPENROUTER_API_KEY: "your-openrouter-api-key"
```

## Deployment

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pullwise
  namespace: pullwise
  labels:
    app: pullwise
spec:
  replicas: 3
  selector:
    matchLabels:
      app: pullwise
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: pullwise
        version: "1.0.0"
    spec:
      containers:
      - name: pullwise
        image: pullwise/pullwise:1.0.0
        imagePullPolicy: IfNotPresent
        ports:
        - name: http
          containerPort: 8080
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          valueFrom:
            configMapKeyRef:
              name: pullwise-config
              key: SPRING_PROFILES_ACTIVE
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            configMapKeyRef:
              name: pullwise-config
              key: SPRING_DATASOURCE_URL
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            configMapKeyRef:
              name: pullwise-config
              key: SPRING_DATASOURCE_USERNAME
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: pullwise-secret
              key: SPRING_DATASOURCE_PASSWORD
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: pullwise-secret
              key: JWT_SECRET
        - name: GITHUB_CLIENT_ID
          valueFrom:
            secretKeyRef:
              name: pullwise-secret
              key: GITHUB_CLIENT_ID
        - name: GITHUB_CLIENT_SECRET
          valueFrom:
            secretKeyRef:
              name: pullwise-secret
              key: GITHUB_CLIENT_SECRET
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: http
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: http
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        resources:
          limits:
            cpu: "2000m"
            memory: "2Gi"
          requests:
            cpu: "500m"
            memory: "512Mi"
```

## Service

```yaml
# service.yaml
apiVersion: v1
kind: Service
metadata:
  name: pullwise
  namespace: pullwise
  labels:
    app: pullwise
spec:
  type: ClusterIP
  ports:
  - port: 80
    targetPort: http
    protocol: TCP
    name: http
  selector:
    app: pullwise
```

## Ingress

```yaml
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: pullwise
  namespace: pullwise
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "300"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "300"
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
      - path: /
        pathType: Prefix
        backend:
          service:
            name: pullwise
            port:
              number: 80
```

## HorizontalPodAutoscaler

```yaml
# hpa.yaml
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
  - type: Resource
    resource:
      name: memory
        target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 30
      - type: Pods
        value: 2
        periodSeconds: 60
      selectPolicy: Max
```

## PodDisruptionBudget

```yaml
# pdb.yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: pullwise-pdb
  namespace: pullwise
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: pullwise
```

## PostgreSQL StatefulSet

```yaml
# postgresql.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: pullwise-postgresql
  namespace: pullwise
spec:
  serviceName: pullwise-postgresql
  replicas: 1
  selector:
    matchLabels:
      app: pullwise-postgresql
  template:
    metadata:
      labels:
        app: pullwise-postgresql
    spec:
      containers:
      - name: postgresql
        image: pgvector/pgvector:pg16
        ports:
        - containerPort: 5432
          name: postgresql
        env:
        - name: POSTGRES_DB
          value: pullwise
        - name: POSTGRES_USER
          value: pullwise
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: pullwise-secret
              key: SPRING_DATASOURCE_PASSWORD
        - name: PGDATA
          value: /var/lib/postgresql/data/pgdata
        volumeMounts:
        - name: data
          mountPath: /var/lib/postgresql/data
        resources:
          requests:
            cpu: 500m
            memory: 512Mi
          limits:
            cpu: 1000m
            memory: 1Gi
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
  volumeClaimTemplates:
  - metadata:
      name: data
    spec:
      accessModes:
      - "ReadWriteOnce"
      resources:
        requests:
          storage: 20Gi
---
apiVersion: v1
kind: Service
metadata:
  name: pullwise-postgresql
  namespace: pullwise
spec:
  type: ClusterIP
  ports:
  - port: 5432
    targetPort: 5432
  selector:
    app: pullwise-postgresql
```

## Redis Deployment

```yaml
# redis.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pullwise-redis
  namespace: pullwise
spec:
  replicas: 1
  selector:
    matchLabels:
      app: pullwise-redis
  template:
    metadata:
      labels:
        app: pullwise-redis
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - containerPort: 6379
          name: redis
        command:
        - redis-server
        - --appendonly
        - "yes"
        volumeMounts:
        - name: data
          mountPath: /data
        resources:
          requests:
            cpu: 250m
            memory: 256Mi
          limits:
            cpu: 500m
            memory: 512Mi
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
      volumes:
      - name: data
        persistentVolumeClaim:
          claimName: pullwise-redis-pvc
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: pullwise-redis-pvc
  namespace: pullwise
spec:
  accessModes:
  - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
---
apiVersion: v1
kind: Service
metadata:
  name: pullwise-redis
  namespace: pullwise
spec:
  type: ClusterIP
  ports:
  - port: 6379
    targetPort: 6379
  selector:
    app: pullwise-redis
```

## ServiceMonitor (Prometheus)

```yaml
# servicemonitor.yaml
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

## Deployment Steps

### 1. Create Namespace

```bash
kubectl apply -f namespace.yaml
```

### 2. Apply Configuration

```bash
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml
```

### 3. Deploy Database

```bash
kubectl apply -f postgresql.yaml
kubectl apply -f redis.yaml
```

### 4. Deploy Application

```bash
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
```

### 5. Configure Ingress

```bash
kubectl apply -f ingress.yaml
```

### 6. Configure HPA

```bash
kubectl apply -f hpa.yaml
kubectl apply -f pdb.yaml
```

## Verify Deployment

```bash
# Check pods
kubectl get pods -n pullwise

# Check services
kubectl get svc -n pullwise

# Check ingress
kubectl get ingress -n pullwise

# Check HPA
kubectl get hpa -n pullwise

# View logs
kubectl logs -f deployment/pullwise -n pullwise
```

## Update Strategy

### Rolling Update

```bash
# Update image
kubectl set image deployment/pullwise \
  pullwise=pullwise/pullwise:1.1.0 \
  -n pullwise

# Watch rollout
kubectl rollout status deployment/pullwise -n pullwise
```

### Rollback

```bash
# View rollout history
kubectl rollout history deployment/pullwise -n pullwise

# Rollback to previous
kubectl rollout undo deployment/pullwise -n pullwise

# Rollback to specific revision
kubectl rollout undo deployment/pullwise --to-revision=2 -n pullwise
```

## Next Steps

- [Helm](/docs/deployment/kubernetes/helm) - Helm deployment
- [Scaling](/docs/deployment/kubernetes/scaling) - HPA scaling
- [Production](/docs/deployment/docker/production) - Production setup
