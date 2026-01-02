# Helm Deployment

Deploy Pullwise using Helm charts.

## Overview

Helm simplifies Kubernetes deployment with:
- Versioned charts
- Configurable installs
- Easy upgrades and rollbacks
- Dependency management

## Prerequisites

```bash
# Install Helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Verify installation
helm version

# Add Pullwise Helm repo
helm repo add pullwise https://charts.pullwise.ai
helm repo update
```

## Quick Start

### Install Chart

```bash
# Install with default values
helm install pullwise pullwise/pullwise \
  --namespace pullwise \
  --create-namespace

# Install with custom values
helm install pullwise pullwise/pullwise \
  --namespace pullwise \
  --create-namespace \
  --values custom-values.yaml
```

### Custom Values

```yaml
# custom-values.yaml
image:
  repository: pullwise/pullwise
  tag: "1.0.0"
  pullPolicy: IfNotPresent

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

postgresql:
  enabled: true
  auth:
    password: "change-me"
  primary:
    persistence:
      enabled: true
      size: 20Gi

redis:
  enabled: true
  auth:
    enabled: false

env:
  SPRING_PROFILES_ACTIVE: "prod"
  SPRING_DATASOURCE_URL: "jdbc:postgresql://pullwise-postgresql:5432/pullwise"
  SPRING_REDIS_HOST: "pullwise-redis"
```

## Chart Values

### Image Configuration

```yaml
image:
  # Image repository
  repository: pullwise/pullwise

  # Image tag
  tag: "1.0.0"

  # Image pull policy
  pullPolicy: IfNotPresent

  # Image pull secrets
  pullSecrets: []

  # Deployment image (for multi-arch)
  platform: {}
```

### Replica Configuration

```yaml
replicaCount: 3

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80
```

### Resource Limits

```yaml
resources:
  limits:
    cpu: 2000m
    memory: 2Gi
  requests:
    cpu: 500m
    memory: 512Mi
```

### Environment Variables

```yaml
env:
  # Spring profile
  SPRING_PROFILES_ACTIVE: "prod"

  # Database
  SPRING_DATASOURCE_URL: "jdbc:postgresql://localhost:5432/pullwise"
  SPRING_DATASOURCE_USERNAME: "pullwise"
  SPRING_DATASOURCE_PASSWORD: "change-me"

  # Redis
  SPRING_REDIS_HOST: "localhost"
  SPRING_REDIS_PORT: "6379"

  # JWT
  JWT_SECRET: "change-me"

  # GitHub OAuth
  GITHUB_CLIENT_ID: "your-client-id"
  GITHUB_CLIENT_SECRET: "your-client-secret"

  # OpenRouter (for LLM)
  OPENROUTER_API_KEY: "your-api-key"

# Load from secret
envFromSecret: {}

# Load from config map
envFromConfigMap: {}
```

### Ingress Configuration

```yaml
ingress:
  enabled: true

  className: nginx

  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"

  hosts:
    - host: pullwise.example.com
      paths:
        - path: /
          pathType: Prefix

  tls:
    - secretName: pullwise-tls
      hosts:
        - pullwise.example.com
```

### Service Configuration

```yaml
service:
  type: ClusterIP

  ports:
    http: 8080
    https: 8443

  annotations: {}
```

### Database Configuration

```yaml
postgresql:
  enabled: true

  auth:
    username: pullwise
    password: "change-me"
    database: pullwise

  primary:
    resources:
      limits:
        cpu: 1000m
        memory: 1Gi
      requests:
        cpu: 500m
        memory: 512Mi

    persistence:
      enabled: true
      size: 20Gi
      storageClass: fast-ssd
```

### Redis Configuration

```yaml
redis:
  enabled: true

  auth:
    enabled: false

  master:
    resources:
      limits:
        cpu: 500m
        memory: 512Mi
      requests:
        cpu: 250m
        memory: 256Mi

    persistence:
      enabled: true
      size: 5Gi
```

## Advanced Configuration

### Multiple Environments

```bash
# Production
helm install pullwise pullwise/pullwise \
  --namespace production \
  --values values-prod.yaml

# Staging
helm install pullwise pullwise/pullwise \
  --namespace staging \
  --values values-staging.yaml

# Development
helm install pullwise pullwise/pullwise \
  --namespace development \
  --values values-dev.yaml
```

### External Database

```yaml
# Disable embedded PostgreSQL
postgresql:
  enabled: false

# Configure external database
env:
  SPRING_DATASOURCE_URL: "jdbc:postgresql://external-db.example.com:5432/pullwise"
  SPRING_DATASOURCE_USERNAME: "pullwise"
  SPRING_DATASOURCE_PASSWORD: "change-me"
```

### Pod Disruption Budget

```yaml
podDisruptionBudget:
  enabled: true
  minAvailable: 1
  # maxUnavailable: 1
```

### Pod Affinity

```yaml
affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchExpressions:
              - key: app.kubernetes.io/name
                operator: In
                values:
                  - pullwise
          topologyKey: kubernetes.io/hostname
```

### Tolerations

```yaml
tolerations:
  - key: "workload"
    operator: "Equal"
    value: "app"
    effect: "NoSchedule"
```

## Secrets Management

### Using Kubernetes Secrets

```bash
# Create secret for sensitive values
kubectl create secret generic pullwise-secrets \
  --from-literal=jwt-secret='your-jwt-secret' \
  --from-literal=github-client-secret='your-github-secret' \
  --from-literal=database-password='your-db-password' \
  -n pullwise
```

Reference in values:

```yaml
env:
  JWT_SECRET:
    secretKeyRef:
      name: pullwise-secrets
      key: jwt-secret

  GITHUB_CLIENT_SECRET:
    secretKeyRef:
      name: pullwise-secrets
      key: github-client-secret
```

### Using Sealed Secrets

```bash
# Install sealed-secrets
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml

# Create sealed secret
kubeseal -f pullwise-secret.yaml -w sealed-secret.yaml

# Apply sealed secret
kubectl apply -f sealed-secret.yaml
```

### Using External Secrets Operator

```yaml
# ExternalSecret for AWS Secrets Manager
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: pullwise-secrets
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: pullwise-secrets
  data:
    - secretKey: jwt-secret
      remoteRef:
        key: prod/pullwise/jwt-secret
```

## Upgrade Strategy

### Rolling Update

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1
    maxUnavailable: 0
```

### Upgrade Command

```bash
# Upgrade to new version
helm upgrade pullwise pullwise/pullwise \
  --namespace pullwise \
  --values custom-values.yaml \
  --set image.tag="1.1.0"

# Upgrade with reuse values
helm upgrade pullwise pullwise/pullwise \
  --namespace pullwise \
  --reuse-values \
  --set image.tag="1.1.0"
```

### Rollback

```bash
# List releases
helm history pullwise -n pullwise

# Rollback to previous version
helm rollback pullwise -n pullwise

# Rollback to specific revision
helm rollback pullwise 5 -n pullwise
```

## Monitoring

### PodMonitors for Prometheus

```yaml
podMonitor:
  enabled: true
  interval: 30s
  scrapeTimeout: 10s
  labels: {}
```

### ServiceMonitor

```yaml
serviceMonitor:
  enabled: true
  interval: 30s
  scrapeTimeout: 10s
  labels: {}
```

## Backup and Restore

### PostgreSQL Backup

```bash
# Create backup cron job
kubectl create -f - <<EOF
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgres-backup
  namespace: pullwise
spec:
  schedule: "0 2 * * *"
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: backup
            image: postgres:16
            command:
            - sh
            - -c
            - |
              pg_dump -h pullwise-postgresql -U pullwise pullwise | \
              gzip > /backup/pullwise-$(date +%Y%m%d).sql.gz
            volumeMounts:
            - name: backup
              mountPath: /backup
          volumes:
          - name: backup
            persistentVolumeClaim:
              claimName: backup-pvc
          restartPolicy: OnFailure
EOF
```

## Troubleshooting

### Check Pod Status

```bash
# List pods
kubectl get pods -n pullwise

# Describe pod
kubectl describe pod pullwise-xxxxx -n pullwise

# View logs
kubectl logs pullwise-xxxxx -n pullwise

# Follow logs
kubectl logs -f pullwise-xxxxx -n pullwise
```

### Check Services

```bash
# List services
kubectl get svc -n pullwise

# Describe service
kubectl describe svc pullwise -n pullwise
```

### Check Ingress

```bash
# List ingress
kubectl get ingress -n pullwise

# Describe ingress
kubectl describe ingress pullwise -n pullwise

# Test ingress
curl -H "Host: pullwise.example.com" http://ingress-ip/
```

## Best Practices

### 1. Use Values Files

```bash
# Good: Separate values files
helm install pullwise pullwise/pullwise \
  -f values.yaml \
  -f values-prod.yaml \
  --set image.tag=1.0.0

# Bad: Too many --set flags
helm install pullwise pullwise/pullwise \
  --set env.SPRING_PROFILES_ACTIVE=prod \
  --set env.SPRING_DATASOURCE_URL=... \
  --set env.GITHUB_CLIENT_ID=...
```

### 2. Version Pin Charts

```bash
# Pin chart version
helm install pullwise pullwise/pullwise \
  --version 1.2.3 \
  -f values.yaml
```

### 3. Use Helm Diff

```bash
# Preview changes
helm diff upgrade pullwise pullwise/pullwise \
  -f values.yaml

# Apply if good
helm upgrade pullwise pullwise/pullwise \
  -f values.yaml
```

### 4. Test Upgrades

```bash
# Test upgrade in staging first
helm upgrade pullwise-staging pullwise/pullwise \
  -n staging -f values-staging.yaml \
  --wait --timeout 5m

# Then production
helm upgrade pullwise pullwise/pullwise \
  -n production -f values-prod.yaml
```

## Next Steps

- [Manifests](/docs/deployment/kubernetes/manifests) - Raw manifests
- [Scaling](/docs/deployment/kubernetes/scaling) - HPA scaling
- [Production](/docs/deployment/docker/production) - Production setup
