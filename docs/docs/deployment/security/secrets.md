# Secrets Management

Securely manage Pullwise secrets and sensitive data.

## Overview

Pullwise requires several secrets:
- Database passwords
- JWT signing keys
- OAuth client secrets
- API keys (LLM providers)
- Webhook signing secrets

## Kubernetes Secrets

### Creating Secrets

```bash
# Create generic secret
kubectl create secret generic pullwise-secrets \
  --from-literal=database-password='your-db-password' \
  --from-literal=jwt-secret='your-jwt-secret' \
  --from-literal=github-client-secret='your-github-secret' \
  -n pullwise

# Create from file
kubectl create secret generic pullwise-credentials \
  --from-file=database-password=./db-password.txt \
  --from-file=jwt-secret=./jwt-secret.txt \
  -n pullwise

# Create TLS secret
kubectl create secret tls pullwise-tls \
  --cert=tls.crt \
  --key=tls.key \
  -n pullwise
```

### Using Secrets

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pullwise
spec:
  containers:
  - name: pullwise
    image: pullwise/pullwise:1.0.0
    env:
    - name: SPRING_DATASOURCE_PASSWORD
      valueFrom:
        secretKeyRef:
          name: pullwise-secrets
          key: database-password
    - name: JWT_SECRET
      valueFrom:
        secretKeyRef:
          name: pullwise-secrets
          key: jwt-secret
    volumeMounts:
    - name: secrets
      mountPath: /etc/secrets
      readOnly: true
  volumes:
  - name: secrets
    secret:
      secretName: pullwise-secrets
```

### Secret as Environment Variables

```yaml
env:
- name: SPRING_DATASOURCE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: pullwise-secrets
      key: database-password
      optional: false  # Set to true if secret is optional
```

### Secret as Volume

```yaml
volumeMounts:
- name: secret-volume
  mountPath: /etc/secrets
  readOnly: true
volumes:
- name: secret-volume
  secret:
    secretName: pullwise-secrets
    items:
    - key: database-password
      path: db-password
    - key: jwt-secret
      path: jwt-secret
```

## Sealed Secrets

### Install Sealed Secrets

```bash
# Add Helm repo
helm repo add sealed-secrets https://bitnami-labs.github.io/sealed-secrets
helm repo update

# Install controller
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml

# Verify installation
kubectl get pods -n sealed-secrets
```

### Create Sealed Secret

```yaml
# secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: pullwise-secrets
  namespace: pullwise
type: Opaque
stringData:
  database-password: "your-db-password"
  jwt-secret: "your-jwt-secret"
```

```bash
# Seal the secret
kubeseal -f secret.yaml -w sealed-secret.yaml

# Apply sealed secret (can be committed to git)
kubectl apply -f sealed-secret.yaml
```

### Sealed Secret Format

```yaml
# sealed-secret.yaml
apiVersion: bitnami.com/v1alpha1
kind: SealedSecret
metadata:
  name: pullwise-secrets
  namespace: pullwise
spec:
  encryptedData:
    database-password: AgBy3i4OJWK...base64...
    jwt-secret: AgBy3i4OJWK...base64...
  template:
    metadata:
      name: pullwise-secrets
      namespace: pullwise
    type: Opaque
```

## External Secrets Operator

### Install External Secrets

```bash
# Install via Helm
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets \
  external-secrets/external-secrets \
  -n external-secrets \
  --create-namespace
```

### AWS Secrets Manager

```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-east-1
      auth:
        jwt:
          serviceAccountRef:
            name: external-secrets-sa
---
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
    creationPolicy: Owner
  data:
  - secretKey: database-password
    remoteRef:
      key: prod/pullwise/database-password
  - secretKey: jwt-secret
    remoteRef:
      key: prod/pullwise/jwt-secret
```

### Azure Key Vault

```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: azure-keyvault
spec:
  provider:
    azurekv:
      tenantId: "your-tenant-id"
      vaultUrl: "https://your-vault.vault.azure.net"
      auth:
        managedIdentity:
          clientId: "your-client-id"
---
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: pullwise-secrets
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: azure-keyvault
    kind: SecretStore
  target:
    name: pullwise-secrets
    creationPolicy: Owner
  data:
  - secretKey: database-password
    remoteRef:
      key: database-password
  - secretKey: jwt-secret
    remoteRef:
      key: jwt-secret
```

### HashiCorp Vault

```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: hashicorp-vault
spec:
  provider:
    vault:
      server: "https://vault.example.com"
      path: "secret"
      version: "v2"
      auth:
        kubernetes:
          mountPath: "kubernetes"
          role: "pullwise"
---
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: pullwise-secrets
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: hashicorp-vault
    kind: SecretStore
  target:
    name: pullwise-secrets
    creationPolicy: Owner
  data:
  - secretKey: database-password
    remoteRef:
      key: pullwise/database-password
  - secretKey: jwt-secret
    remoteRef:
      key: pullwise/jwt-secret
```

## Docker Secrets

### Docker Swarm Secrets

```bash
# Create secret
echo "your-db-password" | docker secret create db_password -

# Create secret from file
docker secret create jwt_secret ./jwt-secret.txt

# List secrets
docker secret ls
```

### Use in Docker Compose

```yaml
version: '3.8'

services:
  pullwise:
    image: pullwise/pullwise:1.0.0
    secrets:
      - db_password
      - jwt_secret
    environment:
      SPRING_DATASOURCE_PASSWORD_FILE: /run/secrets/db_password
      JWT_SECRET_FILE: /run/secrets/jwt_secret

secrets:
  db_password:
    external: true
  jwt_secret:
    external: true
```

## Environment-Specific Secrets

### Development Secrets

```yaml
# secrets-dev.yaml
apiVersion: v1
kind: Secret
metadata:
  name: pullwise-secrets
  namespace: pullwise-dev
stringData:
  database-password: "dev-password-123"
  jwt-secret: "dev-jwt-secret"
  github-client-secret: "dev-github-secret"
```

### Production Secrets

```yaml
# secrets-prod.yaml
apiVersion: v1
kind: Secret
metadata:
  name: pullwise-secrets
  namespace: pullwise-prod
stringData:
  # Use strong random values
  database-password: "<random-32-char>"
  jwt-secret: "<random-64-char>"
  github-client-secret: "<from-github>"
```

## Secret Rotation

### Manual Rotation

```bash
# 1. Generate new secret
openssl rand -base64 32 > new-jwt-secret.txt

# 2. Update secret
kubectl create secret generic pullwise-secrets-new \
  --from-file=jwt-secret=new-jwt-secret.txt \
  -n pullwise

# 3. Update deployment to use new secret
kubectl set env deployment/pullwise \
  --from=secret/pullwise-secrets-new/jwt-secret \
  -n pullwise

# 4. Verify application works

# 5. Delete old secret
kubectl delete secret pullwise-secrets-old -n pullwise
```

### Automated Rotation

```yaml
# External secrets with refresh
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: pullwise-secrets
spec:
  refreshInterval: 1h  # Check every hour
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: pullwise-secrets
```

## Best Practices

### 1. Never Commit Secrets

```bash
# Add to .gitignore
*.pem
*.key
*.crt
secrets/
.env.local
```

### 2. Use Namespace Isolation

```bash
# Separate secrets per environment
kubectl get secrets -n pullwise-dev
kubectl get secrets -n pullwise-staging
kubectl get secrets -n pullwise-prod
```

### 3. Limit Secret Access

```yaml
# RBAC for secrets
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: secret-reader
rules:
- apiGroups: [""]
  resources: ["secrets"]
  verbs: ["get"]
  resourceNames: ["pullwise-secrets"]
```

### 4. Use Immutable Secrets

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: pullwise-secrets
immutable: true  # Secret cannot be updated
type: Opaque
stringData:
  database-password: "your-password"
```

### 5. Audit Secret Usage

```bash
# Find pods using secrets
kubectl get pods -n pullwise -o json | \
  jq -r '.items[] | select(.spec.containers[].env[]?.valueFrom.secretKeyRef.name == "pullwise-secrets") | .metadata.name'
```

## Troubleshooting

### Secret Not Found

```bash
# List secrets
kubectl get secrets -n pullwise

# Describe secret
kubectl describe secret pullwise-secrets -n pullwise

# Check if pod references secret
kubectl describe pod pullwise-xxxxx -n pullwise
```

### Secret Mount Fails

```bash
# Check volume mounts
kubectl get pod pullwise-xxxxx -n pullwise -o json | \
  jq '.spec.volumes[] | select(.secret.secretName == "pullwise-secrets")'

# Check mount paths
kubectl exec -it pullwise-xxxxx -n pullwise -- ls -la /etc/secrets
```

### External Secret Not Syncing

```bash
# Check external secret
kubectl get externalsecret -n pullwise

# Describe external secret
kubectl describe externalsecret pullwise-secrets -n pullwise

# Check operator logs
kubectl logs -n external-secrets -l app.kubernetes.io/name=external-secrets
```

## Next Steps

- [SSL/HTTPS](/docs/deployment/security/ssl-https) - TLS configuration
- [Firewall](/docs/deployment/security/firewall) - Firewall setup
- [Monitoring](/docs/deployment/monitoring/) - Monitoring setup
