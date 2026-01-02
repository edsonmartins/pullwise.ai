# SSL/HTTPS Configuration

Configure SSL/TLS for secure connections.

## Overview

Pullwise supports HTTPS through:
- Ingress TLS termination
- End-to-end TLS
- Certificate management with cert-manager

## cert-manager Setup

### Install cert-manager

```bash
# Add Helm repo
helm repo add jetstack https://charts.jetstack.io
helm repo update

# Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# Verify installation
kubectl get pods -n cert-manager
```

### Create Cluster Issuer

```yaml
# LetsEncrypt production
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
```

### Create Staging Issuer

```yaml
# LetsEncrypt staging (for testing)
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-staging
spec:
  acme:
    server: https://acme-staging-v02.api.letsencrypt.org/directory
    email: admin@example.com
    privateKeySecretRef:
      name: letsencrypt-staging
    solvers:
    - http01:
        ingress:
          class: nginx
```

## Ingress TLS

### Certificate Annotation

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: pullwise
  namespace: pullwise
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
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

### Verify Certificate

```bash
# Check certificate
kubectl get certificate -n pullwise

# Describe certificate
kubectl describe certificate pullwise-tls -n pullwise

# View secret
kubectl get secret pullwise-tls -n pullwise -o yaml
```

## Manual Certificates

### Create TLS Secret

```bash
# Create secret from existing certificates
kubectl create secret tls pullwise-tls \
  --cert=tls.crt \
  --key=tls.key \
  -n pullwise
```

### Ingress with Manual Cert

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: pullwise
  namespace: pullwise
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - pullwise.example.com
    secretName: pullwise-tls  # Manual secret
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

## Wildcard Certificates

### DNS Challenge

```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-dns
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@example.com
    privateKeySecretRef:
      name: letsencrypt-dns
    solvers:
    - dns01:
        cloudflare:
          email: admin@example.com
          apiTokenSecretRef:
            name: cloudflare-api-token
            key: api-token
```

### Wildcard Certificate

```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: pullwise-wildcard
  namespace: pullwise
spec:
  secretName: pullwise-wildcard-tls
  issuerRef:
    name: letsencrypt-dns
    kind: ClusterIssuer
  commonName: "*.example.com"
  dnsNames:
  - "*.example.com"
  - "example.com"
```

## Docker Compose TLS

### Generate Self-Signed Certificate

```bash
# Generate certificate
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout tls.key \
  -out tls.crt \
  -subj "/CN=pullwise.example.com"

# Create Docker secret
docker secret create pullwise-tls tls.crt tls.key
```

### Docker Compose Configuration

```yaml
version: '3.8'

services:
  nginx:
    image: nginx:alpine
    ports:
      - "443:443"
      - "80:80"
    secrets:
      - pullwise-tls
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./tls.crt:/etc/nginx/tls.crt:ro
      - ./tls.key:/etc/nginx/tls.key:ro
    depends_on:
      - pullwise

secrets:
  pullwise-tls:
    external: true
```

### nginx.conf

```nginx
events {
    worker_connections 1024;
}

http {
    upstream pullwise {
        server pullwise:8080;
    }

    # Redirect HTTP to HTTPS
    server {
        listen 80;
        server_name pullwise.example.com;
        return 301 https://$server_name$request_uri;
    }

    # HTTPS server
    server {
        listen 443 ssl http2;
        server_name pullwise.example.com;

        ssl_certificate /etc/nginx/tls.crt;
        ssl_certificate_key /etc/nginx/tls.key;

        # SSL configuration
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers HIGH:!aNULL:!MD5;
        ssl_prefer_server_ciphers on;

        # Proxy settings
        location / {
            proxy_pass http://pullwise;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
```

## Application TLS

### Spring Boot Configuration

```yaml
# application.yml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: pullwise
    protocol: TLSv1.3
    ciphers: HIGH:!aNULL:!MD5
```

### Generate Keystore

```bash
# Generate PKCS12 keystore
openssl pkcs12 -export \
  -in tls.crt \
  -inkey tls.key \
  -out keystore.p12 \
  -name pullwise \
  -password pass:change-me

# Or using keytool
keytool -importkeystore \
  -srckeystore tls.crt \
  -destkeystore keystore.p12 \
  -deststoretype PKCS12
```

## TLS Best Practices

### 1. Use Strong Protocols

```nginx
# Good: TLS 1.2 and 1.3 only
ssl_protocols TLSv1.2 TLSv1.3;

# Bad: TLS 1.0 and 1.1
ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
```

### 2. Use Strong Ciphers

```nginx
# Good: Strong ciphers
ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;

# Bad: Weak ciphers
ssl_ciphers ALL:!aNULL:!eNULL;
```

### 3. Enable HSTS

```nginx
# Enable HSTS
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
```

### 4. Test Configuration

```bash
# Test SSL configuration
openssl s_client -connect pullwise.example.com:443 -tls1_3

# Check certificate expiry
echo | openssl s_client -servername pullwise.example.com \
  -connect pullwise.example.com:443 2>/dev/null | \
  openssl x509 -noout -dates

# SSL Labs test
# https://www.ssllabs.com/ssltest/
```

## Certificate Renewal

### Automatic Renewal

cert-manager automatically renews certificates before expiry:

```yaml
# Certificate with renewal
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: pullwise-tls
  namespace: pullwise
spec:
  secretName: pullwise-tls
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  commonName: pullwise.example.com
  dnsNames:
  - pullwise.example.com
  renewBefore: 240h  # Renew 10 days before expiry
```

### Manual Renewal

```bash
# Force renewal by deleting secret
kubectl delete secret pullwise-tls -n pullwise

# Or restart cert-manager pods
kubectl rollout restart deployment/cert-manager -n cert-manager
```

## Troubleshooting

### Certificate Pending

```bash
# Check certificate status
kubectl describe certificate pullwise-tls -n pullwise

# Check cert-manager logs
kubectl logs -n cert-manager -l app=cert-manager

# Common issues:
# - Port 80 not open (HTTP-01 challenge)
# - DNS not propagated (DNS-01 challenge)
# - Rate limiting by LetsEncrypt
```

### SSL Handshake Errors

```bash
# Test SSL connection
openssl s_client -connect pullwise.example.com:443

# Check certificate chain
openssl s_client -connect pullwise.example.com:443 -showcerts

# Verify certificate
openssl verify -CAfile /etc/ssl/certs/ca-bundle.crt tls.crt
```

### Mixed Content Issues

```yaml
# Ensure all resources use HTTPS
spring:
  web:
    resources:
      static-locations: classpath:/static/
  security:
    require-ssl: true
```

## Next Steps

- [Secrets](/docs/deployment/security/secrets) - Secrets management
- [Firewall](/docs/deployment/security/firewall) - Firewall configuration
- [Monitoring](/docs/deployment/monitoring/) - Monitoring setup
