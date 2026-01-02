# Docker Installation

Complete Docker installation guide for Pullwise.

## Overview

Pullwise provides a production-ready Docker image with all dependencies bundled:

- **Backend** - Spring Boot application (Java 17)
- **Frontend** - React application (Node.js)
- **Database** - PostgreSQL with pgvector
- **Cache** - Redis
- **Queue** - RabbitMQ (optional, for async processing)

## Quick Install

```bash
# Pull the latest images
docker pull pullwise/backend:latest
docker pull pullwise/frontend:latest

# Download docker-compose.yml
curl -LO https://pullwise.ai/docker-compose.yml

# Start all services
docker-compose up -d
```

## Docker Compose Configuration

### Complete docker-compose.yml

```yaml
version: '3.8'

services:
  # PostgreSQL with pgvector extension
  postgres:
    image: pgvector/pgvector:pg16
    container_name: pullwise-postgres
    environment:
      POSTGRES_DB: pullwise
      POSTGRES_USER: pullwise
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-pullwise_dev_2024}
      POSTGRES_INITDB_ARGS: '-E UTF8'
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./db/migration:/docker-entrypoint-initdb.d
    healthcheck:
      test: ['CMD-SHELL', 'pg_isready -U pullwise']
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
    networks:
      - pullwise-network

  # Redis cache
  redis:
    image: redis:7-alpine
    container_name: pullwise-redis
    command: redis-server --appendonly yes
    ports:
      - "${REDIS_PORT:-6379}:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ['CMD', 'redis-cli', 'ping']
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
    networks:
      - pullwise-network

  # RabbitMQ message queue (optional)
  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: pullwise-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER:-pullwise}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:-pullwise_dev_2024}
    ports:
      - "${RABBITMQ_PORT:-5672}:5672"
      - "${RABBITMQ_MGMT_PORT:-15672}:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ['CMD', 'rabbitmq-diagnostics', '-q', 'ping']
      interval: 30s
      timeout: 10s
      retries: 5
    restart: unless-stopped
    networks:
      - pullwise-network
    profiles:
      - with-queue

  # Backend application
  backend:
    image: pullwise/backend:latest
    container_name: pullwise-backend
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      # Spring Profile
      SPRING_PROFILES_ACTIVE: docker

      # Database
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/pullwise
      SPRING_DATASOURCE_USERNAME: pullwise
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-pullwise_dev_2024}

      # Redis
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379

      # RabbitMQ (optional)
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USER:-pullwise}
      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD:-pullwise_dev_2024}

      # Security
      JWT_SECRET: ${JWT_SECRET}
      GITHUB_CLIENT_ID: ${GITHUB_CLIENT_ID}
      GITHUB_CLIENT_SECRET: ${GITHUB_CLIENT_SECRET}

      # LLM Provider
      OPENROUTER_API_KEY: ${OPENROUTER_API_KEY}
      LLM_MODEL: ${LLM_MODEL:-anthropic/claude-3.5-sonnet}
    ports:
      - "${BACKEND_PORT:-8080}:8080"
    volumes:
      - backend_logs:/app/logs
      - plugin_data:/opt/pullwise/plugins
    restart: unless-stopped
    networks:
      - pullwise-network

  # Frontend application
  frontend:
    image: pullwise/frontend:latest
    container_name: pullwise-frontend
    depends_on:
      - backend
    environment:
      VITE_API_URL: ${VITE_API_URL:-http://localhost:8080/api}
      VITE_WS_URL: ${VITE_WS_URL:-ws://localhost:8080/ws}
    ports:
      - "${FRONTEND_PORT:-3000:80"}
    restart: unless-stopped
    networks:
      - pullwise-network

networks:
  pullwise-network:
    driver: bridge

volumes:
  postgres_data:
  redis_data:
  rabbitmq_data:
  backend_logs:
  plugin_data:
```

## Environment Variables

Create a `.env` file in the same directory:

```bash title=".env"
# Database
POSTGRES_PASSWORD=your_secure_password_here
POSTGRES_PORT=5432

# Redis
REDIS_PORT=6379

# RabbitMQ (optional)
RABBITMQ_USER=pullwise
RABBITMQ_PASSWORD=your_rabbitmq_password
RABBITMQ_PORT=5672
RABBITMQ_MGMT_PORT=15672

# Backend
BACKEND_PORT=8080
JWT_SECRET=your_very_long_random_secret_key_min_256_bits

# GitHub OAuth
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret

# LLM Provider
OPENROUTER_API_KEY=sk-or-v1-your-key-here
LLM_MODEL=anthropic/claude-3.5-sonnet

# Frontend
FRONTEND_PORT=3000
VITE_API_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/ws
```

## Running with Docker Compose

### Start All Services

```bash
# Start with default profile (without RabbitMQ)
docker-compose up -d

# Start with message queue
docker-compose --profile with-queue up -d

# Start with monitoring (Prometheus, Grafana, Jaeger)
docker-compose --profile monitoring up -d
```

### Check Service Status

```bash
# List running containers
docker-compose ps

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f backend
docker-compose logs -f frontend
```

### Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## Building from Source

To build your own Docker images:

### Backend

```dockerfile title="backend/Dockerfile"
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Build backend image
cd backend
docker build -t pullwise/backend:latest .
```

### Frontend

```dockerfile title="frontend/Dockerfile"
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

```bash
# Build frontend image
cd frontend
docker build -t pullwise/frontend:latest .
```

## Production Considerations

### Resource Limits

```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2G
        reservations:
          cpus: '1.0'
          memory: 1G
```

### Health Checks

Health endpoints are available at:
- `http://localhost:8080/actuator/health` - Backend health
- `http://localhost:8080/api/v2/health` - API health

### Volume Management

```bash
# Backup volumes
docker run --rm -v pullwise_postgres_data:/data -v $(pwd):/backup alpine tar czf /backup/postgres-backup.tar.gz -C /data .

# Restore volumes
docker run --rm -v pullwise_postgres_data:/data -v $(pwd):/backup alpine tar xzf /backup/postgres-backup.tar.gz -C /data
```

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker-compose logs backend

# Enter container for debugging
docker-compose exec backend sh

# Check health status
docker-compose ps
```

### Database Connection Issues

```bash
# Verify PostgreSQL is ready
docker-compose exec postgres pg_isready -U pullwise

# Check database logs
docker-compose logs postgres
```

### Out of Memory

```bash
# Check container resource usage
docker stats

# Increase memory limits in docker-compose.yml
```

## Next Steps

- [Kubernetes Installation](/docs/getting-started/installation/kubernetes) - Deploy to K8s
- [Configuration](/docs/getting-started/configuration) - Customize your setup
- [Production Deployment](/docs/deployment/docker/production) - Production considerations
