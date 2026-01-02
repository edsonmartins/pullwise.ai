# Backend Setup

Set up your development environment for Pullwise backend development.

## Prerequisites

### Required Software

| Software | Version | Install |
|----------|---------|---------|
| **Java** | 17+ | [Adoptium](https://adoptium.net/) |
| **Maven** | 3.9+ | Included in project |
| **PostgreSQL** | 16+ | [PostgreSQL.org](https://www.postgresql.org/) |
| **Redis** | 7+ | [Redis.io](https://redis.io/) |
| **Docker** | 20.10+ | [Docker.com](https://www.docker.com/) |
| **Git** | Latest | [Git-scm.com](https://git-scm.com/) |

### Recommended Tools

| Tool | Purpose |
|------|---------|
| **IntelliJ IDEA** | IDE (recommended) |
| **Postman** | API testing |
| **pgAdmin** | Database management |
| **RedisInsight** | Redis GUI |

## Installation

### macOS

```bash
# Install dependencies
brew install openjdk@17
brew install maven
brew install postgresql@16
brew install redis
brew install docker

# Set JAVA_HOME
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
source ~/.zshrc
```

### Ubuntu/Debian

```bash
# Install Java 17
sudo apt update
sudo apt install -y openjdk-17-jdk

# Install Maven
sudo apt install -y maven

# Install PostgreSQL
sudo apt install -y postgresql-16 postgresql-contrib-16

# Install Redis
sudo apt install -y redis-server

# Verify installations
java -version
mvn -version
psql --version
redis-cli --version
```

### Windows (WSL2)

```bash
# In WSL2 Ubuntu
sudo apt update
sudo apt install -y openjdk-17-jdk maven postgresql-16 redis-server

# Or use Windows installers
# Java: https://adoptium.net/
# Maven: https://maven.apache.org/
# PostgreSQL: https://www.postgresql.org/download/windows/
```

## Clone Repository

```bash
# Clone the repository
git clone https://github.com/integralltech/pullwise-ai.git
cd pullwise-ai/backend

# Verify directory structure
ls -la
```

## Database Setup

### Start PostgreSQL

```bash
# macOS
brew services start postgresql@16

# Linux
sudo systemctl start postgresql

# Docker
docker run -d \
  --name postgres \
  -e POSTGRES_PASSWORD=pullwise_dev_2024 \
  -e POSTGRES_DB=pullwise \
  -e POSTGRES_USER=pullwise \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

### Create Database

```bash
# Connect to PostgreSQL
psql -U postgres

# Create database and user
CREATE DATABASE pullwise;
CREATE USER pullwise WITH PASSWORD 'pullwise_dev_2024';
GRANT ALL PRIVILEGES ON DATABASE pullwise TO pullwise;
\q
```

### Run Migrations

```bash
# Flyway migrations run automatically on startup
# Or manually:
./mvnw flyway:migrate
```

## Redis Setup

### Start Redis

```bash
# macOS
brew services start redis

# Linux
sudo systemctl start redis

# Docker
docker run -d \
  --name redis \
  -p 6379:6379 \
  redis:7-alpine
```

### Verify Redis

```bash
redis-cli ping
# Should return: PONG
```

## IDE Configuration

### IntelliJ IDEA

1. **Open Project**: File → Open → Select `backend/` directory
2. **Import as Maven**: Click "Open as Maven Project"
3. **SDK**: Configure JDK 17 in File → Project Structure → SDKs
4. **Code Style**: Import Google Java Style
5. **Save Actions**: Enable optimize imports and reformat

### Recommended Plugins

- **SonarLint** - Code quality
- **CheckStyle-IDEA** - Style checking
- **Save Actions** - Auto-format on save
- **Rainbow Brackets** - Colorized brackets
- **Presentation Assistant** - Better syntax highlighting

## Environment Configuration

### Application Configuration

Create `application-local.yml`:

```yaml
# Local development overrides
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pullwise
    username: pullwise
    password: pullwise_dev_2024

  redis:
    host: localhost
    port: 6379

logging:
  level:
    com.pullwise: DEBUG
    org.springframework.security: DEBUG
```

### Environment Variables

Create `.env` file (not in git):

```bash
# Database
POSTGRES_PASSWORD=pullwise_dev_2024

# Security
JWT_SECRET=dev-secret-change-in-production

# GitHub OAuth (optional)
GITHUB_CLIENT_ID=your_client_id
GITHUB_CLIENT_SECRET=your_client_secret

# LLM Provider
OPENROUTER_API_KEY=your_api_key
```

## Build Project

### Clean Build

```bash
# Clean and compile
./mvnw clean compile

# Full build with tests
./mvnw clean install

# Skip tests (faster)
./mvnw clean install -DskipTests
```

### Maven Wrapper

Use the included Maven wrapper:

```bash
./mvnw <command>
```

## Run Application

### Development Mode

```bash
# Run with dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# With custom port
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dserver.port=9090
```

### With Debugger

```bash
# Run with debug port
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5005"
```

### Verify Startup

```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Should return:
# {"status":"UP"}
```

## Running Tests

### Unit Tests

```bash
# Run all tests
./mvnw test

# Run specific class
./mvnw test -Dtest=ReviewServiceTest

# Run with coverage
./mvnw test jacoco:report
```

### Integration Tests

```bash
# Run integration tests
./mvnw verify -P integration-test

# Requires testcontainers
```

## Common Issues

### Port Already in Use

```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>
```

### Database Connection Failed

```bash
# Verify PostgreSQL is running
pg_isready -U pullwise

# Check connection
psql -U pullwise -h localhost -d pullwise
```

### Out of Memory

```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx2g -Xms1g"
```

## Next Steps

- [Frontend Setup](/docs/developer-guide/setup/frontend) - Frontend environment
- [IDE Configuration](/docs/developer-guide/setup/ide) - IDE tips
- [Architecture](/docs/developer-guide/architecture/backend-architecture) - Backend architecture
