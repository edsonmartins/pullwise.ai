# Quick Start

Get Pullwise running in **5 minutes** with Docker.

## Prerequisites

Before you begin, ensure you have:

- **Docker** 20.10+ and **Docker Compose** 2.0+ installed
- **2 GB RAM** minimum (4 GB recommended)
- **10 GB** disk space
- **Linux**, **macOS**, or **Windows** with WSL2

### Verify Docker Installation

```bash
docker --version
docker-compose version
```

## 5-Minute Setup

### Step 1: Download docker-compose.yml

```bash
# Create a directory for Pullwise
mkdir pullwise && cd pullwise

# Download the docker-compose file
curl -LO https://pullwise.ai/docker-compose.yml
```

Or create `docker-compose.yml` manually:

```yaml title="docker-compose.yml"
version: '3.8'

services:
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: pullwise
      POSTGRES_USER: pullwise
      POSTGRES_PASSWORD: pullwise_dev_2024
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  backend:
    image: pullwise/backend:latest
    depends_on:
      - postgres
      - redis
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/pullwise
      SPRING_DATASOURCE_USERNAME: pullwise
      SPRING_DATASOURCE_PASSWORD: pullwise_dev_2024
      SPRING_REDIS_HOST: redis
    ports:
      - "8080:8080"

  frontend:
    image: pullwise/frontend:latest
    environment:
      VITE_API_URL: http://localhost:8080/api
    ports:
      - "3000:80"

volumes:
  postgres_data:
```

### Step 2: Start All Services

```bash
docker-compose up -d
```

This will start:
- PostgreSQL database with pgvector extension
- Redis cache
- Pullwise Backend (Spring Boot)
- Pullwise Frontend (React)

### Step 3: Verify Installation

```bash
# Check if services are running
docker-compose ps

# View logs
docker-compose logs -f backend
```

### Step 4: Access Pullwise

Open your browser and navigate to:

**Frontend:** `http://localhost:3000`
**Backend API:** `http://localhost:8080/api`

## First Login

Pullwise supports GitHub OAuth2 for authentication. To configure:

### Option 1: GitHub OAuth (Recommended)

1. Go to GitHub Settings → Developer settings → OAuth Apps
2. Click "New OAuth App"
3. Configure:
   - **Application name**: Pullwise
   - **Homepage URL**: `http://localhost:3000`
   - **Authorization callback URL**: `http://localhost:8080/api/auth/callback/github`
4. Copy the **Client ID** and generate a **Client Secret**
5. Add to your environment:

```bash
# Add to docker-compose.yml under backend.environment
GITHUB_CLIENT_ID: your_client_id_here
GITHUB_CLIENT_SECRET: your_client_secret_here
```

6. Restart backend:

```bash
docker-compose restart backend
```

### Option 2: Development Mode (No OAuth)

For development without OAuth, you can use the built-in demo user:

- **Email**: `demo@pullwise.ai`
- **Password**: `demo123`

:::note
The demo user is only available in development mode. Do not use in production.
:::

## Run Your First Review

### Step 1: Create a Project

1. Navigate to `http://localhost:3000`
2. Click **"New Project"**
3. Enter project details:
   - **Name**: `my-first-project`
   - **Repository URL**: `https://github.com/your-org/your-repo`
4. Click **"Create"**

### Step 2: Configure Webhook

1. Go to your GitHub repository
2. Navigate to **Settings → Webhooks**
3. Click **"Add webhook"**
4. Configure:
   - **Payload URL**: `http://your-server-ip:8080/webhooks/github`
   - **Content type**: `application/json`
   - **Secret**: (optional) generate a random secret
   - **Events**: Select "Pull requests"
5. Click **"Add webhook"**

:::tip
For localhost testing, use a tool like [ngrok](https://ngrok.com) to expose your local server:
```bash
ngrok http 8080
```
:::

### Step 3: Create a Pull Request

1. Make a change in your repository
2. Create a pull request
3. Pullwise will automatically start reviewing
4. Check the review progress in the Pullwise dashboard

### Step 4: View Results

Once the review is complete:

1. Navigate to **Reviews** in the dashboard
2. Click on your review
3. View issues found, ranked by severity:
   - 🔴 **Critical** - Security vulnerabilities, data leaks
   - 🟠 **High** - Bugs, major issues
   - 🟡 **Medium** - Code quality, maintainability
   - 🔵 **Low** - Style, minor improvements

## Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (deletes data)
docker-compose down -v
```

## Viewing Logs

```bash
# View all logs
docker-compose logs

# Follow logs for specific service
docker-compose logs -f backend
docker-compose logs -f frontend

# View last 100 lines
docker-compose logs --tail=100 backend
```

## Troubleshooting

### Services Won't Start

```bash
# Check port conflicts
netstat -tuln | grep -E ':(3000|8080|5432|6379)'

# Free up ports if needed
docker-compose down
docker-compose up -d
```

### Database Connection Issues

```bash
# Wait for PostgreSQL to be ready
docker-compose logs postgres

# Restart backend after database is up
docker-compose restart backend
```

### Review Not Triggering

1. Check webhook is configured correctly
2. Verify Pullwise server is accessible from GitHub
3. Check backend logs: `docker-compose logs -f backend`

## Next Steps

- [Installation Guide](/docs/category/installation) - Detailed setup options
- [Configuration](/docs/getting-started/configuration) - Customize your setup
- [First Review](/docs/getting-started/first-review) - Deep dive on reviews
- [Troubleshooting](/docs/getting-started/troubleshooting) - Common issues

## Production Deployment

For production deployment, see:

- [Docker Production](/docs/deployment/docker/production)
- [Kubernetes Deployment](/docs/deployment/kubernetes/helm)
- [Security Configuration](/docs/deployment/security/ssl-https)
