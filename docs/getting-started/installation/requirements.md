# System Requirements

Hardware and software requirements for running Pullwise.

## Minimum Requirements

These are the minimum requirements for running Pullwise Community Edition:

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| **CPU** | 2 cores | 4+ cores |
| **RAM** | 4 GB | 8 GB+ |
| **Disk** | 20 GB | 50 GB+ SSD |
| **Network** | 1 Gbps | 1 Gbps |

## Software Requirements

### Operating System

Pullwise supports the following operating systems:

- **Linux**: Ubuntu 20.04+, Debian 11+, RHEL 8+, CentOS 8+
- **macOS**: 12+ (Monterey or later)
- **Windows**: 10/11 with WSL2

### Container Runtime

If using Docker:

- **Docker**: 20.10+
- **Docker Compose**: 2.0+

### Java Runtime

Backend requires:

- **Java**: 17+ (OpenJDK, Eclipse Temurin, or Amazon Corretto)
- **JVM**: Compatible with Java 17

### Node.js

Frontend build requires:

- **Node.js**: 20+
- **npm**: 10+

### Database

Pullwise requires PostgreSQL:

- **PostgreSQL**: 16+ (with pgvector extension)
- **Compatible**: Amazon RDS PostgreSQL, Azure Database for PostgreSQL

### Cache

Pullwise requires Redis:

- **Redis**: 7+

### Message Queue (Optional)

For async processing:

- **RabbitMQ**: 3.12+

## Component Resource Usage

### Backend (Spring Boot)

| Metric | Minimum | Typical | Maximum |
|--------|---------|---------|---------|
| **Memory (Heap)** | 512 MB | 1-2 GB | 4 GB |
| **Memory (Total)** | 1 GB | 2-3 GB | 6 GB |
| **CPU** | 0.5 cores | 1-2 cores | 4 cores |
| **Disk** | 500 MB | 1 GB | 2 GB |

### Frontend (React)

| Metric | Build Time | Runtime |
|--------|------------|---------|
| **Memory** | 2 GB | 100 MB |
| **CPU** | 2 cores | < 0.1 cores |
| **Disk** | 500 MB | 50 MB |

### PostgreSQL

| Metric | Small | Medium | Large |
|--------|-------|--------|-------|
| **RAM** | 1 GB | 2-4 GB | 8-16 GB |
| **CPU** | 1 core | 2 cores | 4+ cores |
| **Disk** | 20 GB | 50 GB | 100+ GB |

### Redis

| Metric | Small | Medium | Large |
|--------|-------|--------|-------|
| **RAM** | 256 MB | 512 MB | 1+ GB |
| **CPU** | 0.5 cores | 1 core | 2+ cores |
| **Disk** | 100 MB | 500 MB | 1+ GB |

## Sizing Guide

### Small Team (1-10 developers)

- **Reviews/month**: 100-500
- **Concurrent reviews**: 1-3
- **Recommended specs**:
  - 2 CPU cores
  - 4 GB RAM
  - 50 GB disk

### Medium Team (10-50 developers)

- **Reviews/month**: 500-5,000
- **Concurrent reviews**: 3-10
- **Recommended specs**:
  - 4 CPU cores
  - 8 GB RAM
  - 100 GB disk

### Large Team (50+ developers)

- **Reviews/month**: 5,000+
- **Concurrent reviews**: 10+
- **Recommended specs**:
  - 8+ CPU cores
  - 16 GB RAM
  - 200+ GB disk
- Consider: Horizontal scaling with Kubernetes

## Network Requirements

### Bandwidth

| Usage | Bandwidth |
|-------|-----------|
| **Web UI** | 1 Mbps per user |
| **Webhooks** | 0.1 Mbps per event |
| **LLM API** | 1-5 Mbps per review |
| **Clone operations** | 10-100 Mbps burst |

### Ports

| Port | Protocol | Service | Direction |
|------|----------|---------|-----------|
| 80 | HTTP | Frontend | Inbound |
| 443 | HTTPS | Frontend | Inbound |
| 8080 | HTTP | Backend API | Inbound |
| 5432 | PostgreSQL | Database | Local only |
| 6379 | Redis | Cache | Local only |
| 5672 | AMQP | RabbitMQ | Local only |
| 15672 | HTTP | RabbitMQ UI | Local only |

### Firewall Rules

```bash
# Allow HTTP/HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Allow backend API (if external access needed)
sudo ufw allow 8080/tcp

# Block database and cache from external
sudo ufw deny 5432/tcp
sudo ufw deny 6379/tcp
sudo ufw deny 5672/tcp
```

## LLM Provider Requirements

Pullwise integrates with external LLM providers:

### OpenRouter

- **API Endpoint**: `https://openrouter.ai/api/v1`
- **Authentication**: API Key
- **Rate Limits**: Depends on plan
- **Network**: Internet access required

### Ollama (Local)

- **CPU**: 4+ cores recommended
- **RAM**: 8-16 GB per model
- **GPU**: Optional (NVIDIA with CUDA)
- **Storage**: 4-8 GB per model

### Model Sizes

| Model | Parameters | RAM Required |
|-------|-----------|--------------|
| Llama 3 8B | 8B | 6 GB |
| Llama 3 70B | 70B | 40 GB |
| Mistral 7B | 7B | 5 GB |
| Gemma 7B | 7B | 5 GB |

## Storage Requirements

### Database Growth

- **Per review**: ~100 KB average
- **Per issue**: ~5 KB average
- **With 1,000 reviews/month**: ~100 MB/month
- **Recommended growth**: 2x current size

### File Storage

Pullwise stores:

| Type | Location | Size |
|------|----------|------|
| **Database** | PostgreSQL | Variable |
| **Plugin JARs** | `/opt/pullwise/plugins` | 10-100 MB each |
| **Logs** | `/var/log/pullwise` | 10-100 MB/day |
| **Cache** | Redis | Configurable |

### Backup Storage

Plan for:

- **Database backups**: 2x current database size
- **Retention**: Keep at least 30 days of backups
- **Off-site**: Consider cloud storage for disaster recovery

## Browser Requirements

### Supported Browsers

| Browser | Minimum Version |
|---------|-----------------|
| Chrome | 120+ |
| Firefox | 121+ |
| Safari | 17+ |
| Edge | 120+ |

### Browser Features

- JavaScript enabled
- Cookies enabled
- WebSocket support
- TLS 1.2+

## High Availability Requirements

For production HA deployments:

### Load Balancer

- **Hardware**: 1 Gbps throughput
- **Software**: HAProxy, NGINX, or cloud LB

### Database

- **PostgreSQL**: Streaming replication
- **Redis**: Sentinel or Cluster
- **Minimum**: 2 replicas

### Application

- **Backend**: 2+ instances
- **Frontend**: 2+ instances
- **Health checks**: Configured

## Next Steps

- [Quick Start](/docs/getting-started/quick-start) - Get started with Docker
- [Installation](/docs/category/installation) - Detailed installation guides
- [Deployment](/docs/category/deployment) - Production deployment
