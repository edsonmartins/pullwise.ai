# Updates and Upgrades

Guidelines for updating Pullwise.

## Overview

This guide covers:
- Version compatibility
- Update procedures
- Rollback steps
- Database migrations
- Breaking changes

## Update Process

### Pre-Update Checklist

- [ ] Review release notes
- [ ] Check breaking changes
- [ ] Test in staging
- [ ] Backup database
- [ ] Plan rollback
- [ ] Schedule maintenance window

### Release Notes

Always review release notes before updating:

```
# Check release notes
curl -s https://api.github.com/repos/integralltech/pullwise-ai/releases/latest | \
  jq -r '.body'

# Or visit
# https://github.com/integralltech/pullwise-ai/releases
```

## Docker Updates

### Update Image

```bash
# Pull latest image
docker pull pullwise/pullwise:latest

# Stop current container
docker stop pullwise
docker rm pullwise

# Start new container
docker run -d \
  --name pullwise \
  -p 8080:8080 \
  -v pullwise-data:/var/lib/pullwise \
  pullwise/pullwise:latest
```

### Docker Compose Update

```yaml
# docker-compose.yml
services:
  pullwise:
    image: pullwise/pullwise:1.1.0  # Update tag
    restart: always
```

```bash
# Pull new images
docker-compose pull

# Recreate containers
docker-compose up -d

# Remove old images
docker image prune -a
```

## Kubernetes Updates

### Rolling Update

```bash
# Update image
kubectl set image deployment/pullwise \
  pullwise=pullwise/pullwise:1.1.0 \
  -n pullwise

# Watch rollout
kubectl rollout status deployment/pullwise -n pullwise

# Check pods
kubectl get pods -n pullwise
```

### Helm Update

```bash
# Update Helm chart
helm repo update
helm upgrade pullwise pullwise/pullwise \
  -n pullwise \
  --set image.tag=1.1.0

# Rollback if needed
helm rollback pullwise -n pullwise
```

### Blue-Green Deployment

```yaml
# Create new deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pullwise-green
spec:
  replicas: 3
  selector:
    matchLabels:
      app: pullwise
      version: "1.1.0"
  template:
    metadata:
      labels:
        app: pullwise
        version: "1.1.0"
    spec:
      containers:
      - name: pullwise
        image: pullwise/pullwise:1.1.0
---
# Update service selector
apiVersion: v1
kind: Service
metadata:
  name: pullwise
spec:
  selector:
    app: pullwise
    version: "1.1.0"  # Switch when ready
```

## Database Migrations

### Automatic Migrations

Pullwise runs migrations automatically on startup:

```bash
# 2024-01-15T10:00:00.000Z INFO Flyway Community Edition 9.0.0
# 2024-01-15T10:00:01.000Z INFO Database: jdbc:postgresql://localhost:5432/pullwise
# 2024-01-15T10:00:02.000Z INFO Successfully applied 5 migrations
```

### Manual Migrations

```bash
# Run migrations manually
./mvnw flyway:migrate -Dflyway.configFiles=flyway.prod.properties

# Preview migrations
./mvnw flyway:info -Dflyway.configFiles=flyway.prod.properties
```

### Migration Troubleshooting

```bash
# If migration fails:
# 1. Check error logs
kubectl logs deployment/pullwise -n pullwise

# 2. Identify failed migration
psql -U pullwise -d pullwise -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# 3. Fix and retry (if safe)
psql -U pullwise -d pullwise -c "DELETE FROM flyway_schema_history WHERE version = 'V8';"

# 4. Restart deployment
kubectl rollout restart deployment/pullwise -n pullwise
```

## Breaking Changes

### Version Compatibility

| Pullwise | Java | PostgreSQL | Notes |
|----------|------|------------|-------|
| 1.0.x | 17 | 14+ | Initial release |
| 1.1.x | 17 | 14+ | Added LLM routing |
| 1.2.x | 17 | 16+ | Added pgvector |

### 1.x to 2.0 Migration

```bash
# Major version update requires manual steps

# 1. Backup
kubectl exec -it pullwise-postgresql-0 -n pullwise -- \
  pg_dump -U pullwise pullwise > backup.sql

# 2. Update PostgreSQL to v16
# (use your cloud provider's migration tool)

# 3. Run data migration script
kubectl exec -it pullwise-0 -n pullwise -- \
  java -jar /app/pullwise.jar migrate-data

# 4. Update application
kubectl set image deployment/pullwise \
  pullwise=pullwise/pullwise:2.0.0
```

## Rollback

### Docker Rollback

```bash
# Stop current
docker stop pullwise

# Start previous version
docker run -d \
  --name pullwise \
  -p 8080:8080 \
  pullwise/pullwise:1.0.0  # Previous version
```

### Kubernetes Rollback

```bash
# View rollout history
kubectl rollout history deployment/pullwise -n pullwise

# Rollback to previous
kubectl rollout undo deployment/pullwise -n pullwise

# Rollback to specific revision
kubectl rollout undo deployment/pullwise --to-revision=2 -n pullwise
```

### Database Rollback

```bash
# If migration caused issues:
# 1. Stop application
kubectl scale deployment/pullwise -n pullwise --replicas=0

# 2. Restore database
kubectl exec -it pullwise-postgresql-0 -n pullwise -- \
  psql -U pullwise -d pullwise < backup.sql

# 3. Rollback migration version
psql -U pullwise -d pullwise -c \
  "DELETE FROM flyway_schema_history WHERE version = 'V8';"

# 4. Restart application
kubectl scale deployment pullwise -n pullwise --replicas=3
```

## Zero-Downtime Updates

### Health Checks

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 30
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
```

### Update Strategy

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1        # Create 1 extra pod
      maxUnavailable: 0  # Never have less pods
```

## Testing Updates

### Staging Environment

```bash
# 1. Update staging first
kubectl set image deployment/pullwise-staging \
  pullwise=pullwise/pullwise:1.1.0 \
  -n staging

# 2. Verify
kubectl port-forward -n staging svc/pullwise-staging 8080:8080
curl http://localhost:8080/actuator/health

# 3. Run smoke tests
./tests/smoke.sh

# 4. If good, update production
kubectl set image deployment/pullwise \
  pullwise=pullwise/pullwise:1.1.0 \
  -n production
```

### Smoke Tests

```bash
#!/bin/bash
# smoke-test.sh

HEALTH_URL="http://pullwise.example.com/actuator/health"
API_URL="http://pullwise.example.com/api"

# Health check
curl -f $HEALTH_URL || exit 1

# Create test review
REVIEW_ID=$(curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  $API_URL/reviews \
  -d '{"projectId": 1, "branch": "test"}' \
  | jq -r '.id')

# Verify review
curl -f $API_URL/reviews/$REVIEW_ID || exit 1

echo "Smoke tests passed"
```

## Monitoring Updates

### Watch Deployment

```bash
# Watch pods
kubectl get pods -n pullwise -w

# Watch logs
kubectl logs -f -n pullwise deployment/pullwise --all-containers=true

# Watch rollout
kubectl rollout status deployment/pullwise -n pullwise -w
```

### Check Metrics

```bash
# Check error rate after update
curl -s http://prometheus:9090/api/v1/query?query=\
'rate(http_server_requests_seconds_count{status=~"5.."}[5m])' \
  | jq '.data.result[0].value[1]'
```

## Best Practices

### 1. Test Before Deploying

```bash
# Always test in staging first
# Run full test suite
# Verify critical paths
```

### 2. Use Tags, Not Latest

```yaml
# Good: Specific version
image: pullwise/pullwise:1.1.0

# Bad: Always latest
image: pullwise/pullwise:latest
```

### 3. Keep Backups

```bash
# Backup before major updates
./backup-script.sh

# Tag backup with version
./backup-script.sh --tag=v1.1.0
```

### 4. Document Changes

```
Update Log:
==========
Date: 2024-01-15
Version: 1.1.0
Changes:
- Added LLM routing
- Improved error handling
Breaking Changes:
- None
Rollback Version: 1.0.5
```

## Troubleshooting

### Pod Not Starting

```bash
# Describe pod
kubectl describe pod pullwise-xxxxx -n pullwise

# Check logs
kubectl logs pullwise-xxxxx -n pullwise

# Common issues:
# - Image pull error
# - ConfigMap missing
# - Secret missing
# - Resource limits
```

### Migration Failures

```bash
# Check flyway history
kubectl exec -it pullwise-0 -n pullwise -- \
  psql -U pullwise -d pullwise -c "SELECT * FROM flyway_schema_history;"

# Manual fix
kubectl exec -it pullwise-0 -n pullwise -- \
  psql -U pullwise -d pullwise -c "SQL HERE;"
```

### Performance Issues

```bash
# Compare metrics before/after
# Look for memory leaks
# Check GC pauses
# Review query performance
```

## Next Steps

- [Backups](/docs/administration/maintenance/backups) - Backup procedures
- [Migrations](/docs/administration/maintenance/migrations) - Migration guide
- [Monitoring](/docs/administration/maintenance/monitoring) - System monitoring
