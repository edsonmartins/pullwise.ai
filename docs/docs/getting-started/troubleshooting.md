# Troubleshooting

Common issues and solutions when running Pullwise.

## Installation Issues

### Docker Compose Won't Start

**Problem**: Services fail to start or immediately exit

```bash
$ docker-compose up -d
ERROR: for backend  Cannot start service backend: driver failed programming
```

**Solutions**:

1. Check port conflicts:

```bash
# Check what's using the ports
netstat -tuln | grep -E ':(3000|8080|5432|6379)'

# Or use lsof
lsof -i :5432
```

2. Free up ports or change configuration:

```yaml
# In docker-compose.yml
services:
  postgres:
    ports:
      - "5433:5432"  # Use different host port
```

3. Check Docker resources:

```bash
# Check Docker is running
docker info

# Check disk space
df -h

# Check memory
free -h
```

### Out of Memory

**Problem**: Containers getting OOM killed

```bash
$ docker-compose ps
pullwise-backend  Exited (137)
```

**Solutions**:

1. Increase Docker memory limit (Docker Desktop: Settings → Resources)
2. Add memory limits to docker-compose.yml:

```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          memory: 2G
        reservations:
          memory: 1G
```

3. Reduce JVM heap size:

```yaml
services:
  backend:
    environment:
      JAVA_OPTS: "-Xmx1g -Xms512m"
```

### Database Connection Issues

**Problem**: Backend can't connect to PostgreSQL

```bash
ERROR: Could not open connection to database
```

**Solutions**:

1. Wait for PostgreSQL to be ready:

```bash
# Check PostgreSQL logs
docker-compose logs postgres

# Should see: "database system is ready to accept connections"
```

2. Add health check dependency:

```yaml
services:
  backend:
    depends_on:
      postgres:
        condition: service_healthy
```

3. Verify connection from backend container:

```bash
docker-compose exec backend \
  psql -h postgres -U pullwise -d pullwise -c "SELECT 1"
```

## Review Issues

### Review Not Triggering

**Problem**: Creating a PR doesn't trigger a review

**Solutions**:

1. Verify webhook is configured:

```bash
# Check GitHub webhook settings
# Repository → Settings → Webhooks → Pullwise webhook

# Check recent deliveries
# Click the webhook → "Recent Deliveries" tab
```

2. Test webhook manually:

```bash
# Use ngrok for local testing
ngrok http 8080

# Update webhook payload URL to ngrok URL
# Test with sample payload
curl -X POST https://your-ngrok-url/webhooks/github \
  -H "Content-Type: application/json" \
  -d @test-payload.json
```

3. Check backend logs:

```bash
docker-compose logs -f backend | grep webhook
```

### Review Stuck in "Analyzing"

**Problem**: Review status doesn't change from "Analyzing"

**Solutions**:

1. Check LLM provider status:

```bash
# Check OpenRouter status
curl https://openrouter.ai/api/v1/status

# Check API key is valid
curl -H "Authorization: Bearer $OPENROUTER_API_KEY" \
  https://openrouter.ai/api/v1/models
```

2. Increase timeout:

```yaml
# In application.yml
pullwise:
  llm:
    timeout: 600000  # 10 minutes
```

3. Cancel and retry:

```bash
# Cancel stuck review
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/reviews/{id}/cancel
```

### No Issues Found

**Problem**: Review completes but finds 0 issues

**Possible causes**:

1. **Empty diff** - PR doesn't have code changes
2. **Filtered files** - Changed files are excluded
3. **Small PR** - Changes are too minor

**Verification**:

```bash
# Check review details
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/reviews/{id}

# Check which files were analyzed
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/reviews/{id}/issues
```

## Authentication Issues

### GitHub OAuth Not Working

**Problem**: OAuth login redirects to error page

**Solutions**:

1. Verify OAuth configuration:

```bash
# Check environment variables
docker-compose exec backend env | grep GITHUB

# Should show GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET
```

2. Verify callback URL in GitHub:

```
Homepage URL: http://localhost:3000 (or your domain)
Authorization callback URL: http://localhost:8080/api/auth/callback/github
```

3. Check backend logs for OAuth errors:

```bash
docker-compose logs -f backend | grep -i oauth
```

### JWT Token Expired

**Problem**: API calls return 401 Unauthorized

```json
{
  "error": "Unauthorized",
  "message": "JWT token expired"
}
```

**Solutions**:

1. Refresh token:

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  http://localhost:8080/api/auth/refresh \
  -d '{"refreshToken": "your-refresh-token"}'
```

2. Re-authenticate:

```bash
# Get new token via OAuth or demo user
curl -X POST \
  -H "Content-Type: application/json" \
  http://localhost:8080/api/auth/login \
  -d '{"email":"demo@pullwise.ai","password":"demo123"}'
```

3. Adjust token expiration:

```yaml
# In application.yml
pullwise:
  security:
    jwt:
      expiration: 604800000  # 7 days instead of 1 day
```

## Performance Issues

### Slow Reviews

**Problem**: Reviews take too long to complete

**Solutions**:

1. **Enable parallel SAST**:

```yaml
pullwise:
  review:
    parallel-sast: true
```

2. **Use faster LLM**:

```yaml
# Use cheaper, faster model
LLM_MODEL=openai/gpt-3.5-turbo
```

3. **Cache analysis results**:

```yaml
spring:
  redis:
    # Increase Redis memory
  cache:
    type: redis
    cache-names: sast-cache, llm-cache
    redis:
      time-to-live: 3600000  # 1 hour
```

4. **Exclude unnecessary files**:

```yaml
pullwise:
  review:
    excluded-paths:
      - node_modules/**
      - vendor/**
      - "**/*.min.js"
      - "**/*.min.css"
```

### High Memory Usage

**Problem**: Backend consuming too much memory

**Solutions**:

1. Tune JVM:

```yaml
JAVA_OPTS: "-Xmx2g -Xms512m -XX:MaxMetaspaceSize=512m"
```

2. Tune connection pools:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
  redis:
    lettuce:
      pool:
        max-active: 10
        max-idle: 5
```

## Logging Issues

### Too Many Logs

**Problem**: Logs growing too fast

**Solutions**:

1. Adjust log level:

```yaml
logging:
  level:
    com.pullwise: INFO
    org.springframework: WARN
    org.hibernate.SQL: WARN
```

2. Configure log rotation:

```xml
<!-- logback-spring.xml -->
<appender name="ROLLING" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>/var/log/pullwise/application.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>/var/log/pullwise/application-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>30</maxHistory>
        <totalSizeCap>10GB</totalSizeCap>
    </rollingPolicy>
</appender>
```

### Can't Find Logs

**Problem**: Not sure where logs are

**Solutions**:

```bash
# Docker logs
docker-compose logs backend

# Container logs
docker-compose exec backend cat /app/logs/application.log

# Systemd logs (if using systemd)
journalctl -u pullwise-backend -f
```

## Plugin Issues

### Plugin Not Loading

**Problem**: Custom plugin not appearing

**Solutions**:

1. Verify plugin location:

```bash
# Check plugins directory
ls -la /opt/pullwise/plugins/

# Should show your plugin JAR
```

2. Check plugin manifest:

```bash
# Verify SPI file exists
unzip -l your-plugin.jar | grep META-INF/services/
```

3. Check plugin logs:

```bash
docker-compose logs backend | grep -i plugin
```

### Plugin Timeout

**Problem**: Plugin analysis timing out

**Solutions**:

```yaml
# Increase plugin timeout
pullwise:
  plugins:
    timeout: 120000  # 2 minutes
```

## Getting Help

If you're still having issues:

1. **Check logs**:

```bash
docker-compose logs --tail=100 backend
```

2. **Health check**:

```bash
curl http://localhost:8080/actuator/health
```

3. **Enable debug logging**:

```yaml
logging:
  level:
    com.pullwise: DEBUG
```

4. **Community resources**:
   - [GitHub Issues](https://github.com/integralltech/pullwise-ai/issues)
   - [Discord Community](https://discord.gg/pullwise)
   - [Documentation](https://docs.pullwise.ai)

### Report an Issue

When reporting an issue, include:

- **Pullwise version**: `curl http://localhost:8080/actuator/info`
- **Logs**: Relevant error messages
- **Configuration**: Sanitized configuration
- **Steps to reproduce**: Clear reproduction steps

## Next Steps

- [Configuration](/docs/getting-started/configuration) - Adjust settings
- [First Review](/docs/getting-started/first-review) - Test your setup
- [User Guide](/docs/category/user-guide) - Learn features
