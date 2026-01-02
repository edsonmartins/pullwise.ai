# Firewall Configuration

Configure firewalls and network policies for Pullwise.

## Overview

Network security is critical for production deployments. This guide covers:

- Kubernetes Network Policies
- AWS Security Groups
- GCP Firewall Rules
- Azure Network Security Groups
- Docker networking

## Kubernetes Network Policies

### Default Deny All

```yaml
# Deny all ingress by default
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-ingress
  namespace: pullwise
spec:
  podSelector: {}
  policyTypes:
  - Ingress
---
# Deny all egress by default
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-egress
  namespace: pullwise
spec:
  podSelector: {}
  policyTypes:
  - Egress
```

### Allow Ingress from Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-from-ingress
  namespace: pullwise
spec:
  podSelector:
    matchLabels:
      app: pullwise
  policyTypes:
  - Ingress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 8080
```

### Allow Egress to Database

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-to-postgresql
  namespace: pullwise
spec:
  podSelector:
    matchLabels:
      app: pullwise
  policyTypes:
  - Egress
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: pullwise-postgresql
    ports:
    - protocol: TCP
      port: 5432
```

### Allow Egress to External APIs

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-external-apis
  namespace: pullwise
spec:
  podSelector:
    matchLabels:
      app: pullwise
  policyTypes:
  - Egress
  egress:
  - to:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 443  # HTTPS
  - to:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 80   # HTTP
```

### Full Policy Set

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: pullwise-policy
  namespace: pullwise
spec:
  podSelector:
    matchLabels:
      app: pullwise
  policyTypes:
  - Ingress
  - Egress
  ingress:
  # Allow from ingress controller
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 8080
  # Allow from monitoring
  - from:
    - namespaceSelector:
        matchLabels:
          name: monitoring
    ports:
    - protocol: TCP
      port: 8080
  egress:
  # DNS
  - to:
    - namespaceSelector:
        matchLabels:
          name: kube-system
    ports:
    - protocol: UDP
      port: 53
  # PostgreSQL
  - to:
    - podSelector:
        matchLabels:
          app: pullwise-postgresql
    ports:
    - protocol: TCP
      port: 5432
  # Redis
  - to:
    - podSelector:
        matchLabels:
          app: pullwise-redis
    ports:
    - protocol: TCP
      port: 6379
  # External APIs (HTTPS)
  - to:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 443
```

## AWS Security Groups

### Security Group for Pullwise

```bash
# Create security group
aws ec2 create-security-group \
  --group-name pullwise-sg \
  --description "Pullwise application security group" \
  --vpc-id vpc-xxxxxxxx

# Allow ingress from ALB/NLB
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxxxxx \
  --protocol tcp \
  --port 8080 \
  --source-group sg-alb-xxxxxxxx

# Allow egress to PostgreSQL
aws ec2 authorize-security-group-egress \
  --group-id sg-xxxxxxxx \
  --protocol tcp \
  --port 5432 \
  --destination sg-rds-xxxxxxxx

# Allow egress to internet (for APIs)
aws ec2 authorize-security-group-egress \
  --group-id sg-xxxxxxxx \
  --protocol tcp \
  --port 443 \
  --cidr 0.0.0.0/0
```

### Terraform Configuration

```hcl
# security-group.tf
resource "aws_security_group" "pullwise" {
  name        = "pullwise-sg"
  description = "Pullwise application security group"
  vpc_id      = var.vpc_id

  # HTTP from ALB
  ingress {
    description     = "HTTP from ALB"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  # HTTPS to external APIs
  egress {
    description = "HTTPS to external APIs"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # PostgreSQL
  egress {
    description     = "PostgreSQL access"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.rds.id]
  }

  tags = {
    Name = "pullwise-sg"
  }
}
```

## GCP Firewall Rules

### Create Firewall Rules

```bash
# Allow ingress from load balancer
gcloud compute firewall-rules create pullwise-allow-ingress \
  --network=vpc-network \
  --action=ALLOW \
  --direction=INGRESS \
  --source-ranges=10.0.0.0/8 \
  --target-tags=pullwise \
  --rules=tcp:8080

# Allow egress to PostgreSQL
gcloud compute firewall-rules create pullwise-allow-postgresql \
  --network=vpc-network \
  --action=ALLOW \
  --direction=EGRESS \
  --destination-ranges=10.1.0.0/16 \
  --rules=tcp:5432

# Allow egress to internet
gcloud compute firewall-rules create pullwise-allow-external \
  --network=vpc-network \
  --action=ALLOW \
  --direction=EGRESS \
  --destination-ranges=0.0.0.0/0 \
  --rules=tcp:443,tcp:80
```

### Terraform Configuration

```hcl
# firewall.tf
resource "google_compute_firewall" "pullwise_ingress" {
  name    = "pullwise-allow-ingress"
  network = var.vpc_name

  allow {
    protocol = "tcp"
    ports    = ["8080"]
  }

  source_ranges = ["10.0.0.0/8"]
  target_tags   = ["pullwise"]
}

resource "google_compute_firewall" "pullwise_egress" {
  name      = "pullwise-allow-egress"
  network   = var.vpc_name
  direction = "EGRESS"

  allow {
    protocol = "tcp"
    ports    = ["443", "80"]
  }

  destination_ranges = ["0.0.0.0/0"]
  target_tags        = ["pullwise"]
}

resource "google_compute_firewall" "pullwise_postgresql" {
  name      = "pullwise-allow-postgresql"
  network   = var.vpc_name
  direction = "EGRESS"

  allow {
    protocol = "tcp"
    ports    = ["5432"]
  }

  destination_ranges = ["10.1.0.0/16"]
  target_tags        = ["pullwise"]
}
```

## Azure Network Security Groups

### Create NSG Rules

```bash
# Create NSG
az network nsg create \
  --name pullwise-nsg \
  --resource-group myResourceGroup \
  --location eastus

# Allow ingress from load balancer
az network nsg rule create \
  --nsg-name pullwise-nsg \
  --name allow-lb-ingress \
  --resource-group myResourceGroup \
  --access Allow \
  --protocol Tcp \
  --direction Inbound \
  --source-address-prefixes LoadBalancer \
  --source-port-ranges '*' \
  --destination-address-prefixes '*' \
  --destination-port-ranges 8080

# Allow egress to PostgreSQL
az network nsg rule create \
  --nsg-name pullwise-nsg \
  --name allow-postgresql \
  --resource-group myResourceGroup \
  --access Allow \
  --protocol Tcp \
  --direction Outbound \
  --source-address-prefixes '*' \
  --source-port-ranges '*' \
  --destination-address-prefixes 10.1.0.0/16 \
  --destination-port-ranges 5432
```

### Terraform Configuration

```hcl
# nsg.tf
resource "azurerm_network_security_group" "pullwise" {
  name                = "pullwise-nsg"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name

  security_rule {
    name                       = "allow-http"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "8080"
    source_address_prefix      = "LoadBalancer"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "allow-postgresql"
    priority                   = 200
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "5432"
    source_address_prefix      = "*"
    destination_address_prefix = "10.1.0.0/16"
  }

  security_rule {
    name                       = "allow-https"
    priority                   = 300
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }
}
```

## Docker Networking

### User-Defined Bridge Network

```yaml
# docker-compose.yml
version: '3.8'

services:
  pullwise:
    image: pullwise/pullwise:1.0.0
    networks:
      - pullwise-net
    ports:
      - "8080:8080"

  postgresql:
    image: pgvector/pgvector:pg16
    networks:
      - pullwise-net
    environment:
      POSTGRES_DB: pullwise
      POSTGRES_USER: pullwise

  redis:
    image: redis:7-alpine
    networks:
      - pullwise-net

networks:
  pullwise-net:
    driver: bridge
    ipam:
      config:
        - subnet: 172.28.0.0/16
```

### Firewall with Docker

```bash
# Use UFW to restrict Docker
sudo ufw allow from 172.28.0.0/16 to any port 8080
sudo ufw allow 22    # SSH
sudo ufw allow 80    # HTTP
sudo ufw allow 443   # HTTPS
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw enable
```

## Service Mesh (Istio)

### Peer Authentication

```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: pullwise-mtls
  namespace: pullwise
spec:
  selector:
    matchLabels:
      app: pullwise
  mtls:
    mode: STRICT
```

### Authorization Policy

```yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: pullwise-authz
  namespace: pullwise
spec:
  selector:
    matchLabels:
      app: pullwise
  action: ALLOW
  rules:
  - from:
    - source:
        principals:
        - cluster.local/ns/ingress-nginx/sa/ingress-nginx
    to:
    - operation:
        methods: ["GET", "POST", "PUT", "DELETE"]
  - from:
    - source:
        namespaces:
        - monitoring
    to:
    - operation:
        ports: ["8080"]
```

## Best Practices

### 1. Least Privilege

```bash
# Good: Specific port
aws ec2 authorize-security-group-ingress \
  --port 8080

# Bad: All ports
aws ec2 authorize-security-group-ingress \
  --port 1-65535
```

### 2. Default Deny

```yaml
# Good: Deny all, allow specific
policyTypes:
- Ingress
- Egress
ingress: []

# Bad: Allow all
policyTypes: []
ingress:
- {}
```

### 3. Use Tags for Organization

```bash
# Tag resources
aws ec2 create-tags \
  --resources sg-xxxxxxxx \
  --tags Key=Environment,Value=Production \
         Key=Application,Value=Pullwise
```

### 4. Regular Audits

```bash
# List all security group rules
aws ec2 describe-security-groups \
  --query 'SecurityGroups[*].IpPermissions'

# Find overly permissive rules
aws ec2 describe-security-groups \
  --query 'SecurityGroups[*].IpPermissions[?contains(IpRanges[].CidrIp, `0.0.0.0/0`)]'
```

## Monitoring Network Policies

### Verify Policies

```bash
# Check network policies
kubectl get networkpolicies -n pullwise

# Describe policy
kubectl describe networkpolicy pullwise-policy -n pullwise

# Test connectivity
kubectl run test-pod --image=nicolaka/netshoot -it --rm --restart=Never -n pullwise
```

### Network Policy Editor

Use tools like:
- [Tufin](https://www.tufin.com/) - Visual policy editor
- [Cillium](https://cilium.io/) - Advanced networking
- [Calico](https://www.projectcalico.org/) - Network policies

## Next Steps

- [SSL/HTTPS](/docs/deployment/security/ssl-https) - TLS configuration
- [Secrets](/docs/deployment/security/secrets) - Secrets management
- [Monitoring](/docs/deployment/monitoring/) - Monitoring setup
