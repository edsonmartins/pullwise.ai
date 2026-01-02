# Manual Installation

Install Pullwise components manually on your server.

## Prerequisites

### System Requirements

- **Operating System**: Linux (Ubuntu 20.04+, Debian 11+, RHEL 8+), macOS 12+, or Windows with WSL2
- **CPU**: 2 cores minimum, 4+ recommended
- **RAM**: 4 GB minimum, 8 GB+ recommended
- **Disk**: 20 GB free space

### Required Software

| Component | Version | Description |
|-----------|---------|-------------|
| **Java** | 17+ | Backend runtime (JDK) |
| **Node.js** | 20+ | Frontend build |
| **PostgreSQL** | 16+ | Database with pgvector |
| **Redis** | 7+ | Cache and session store |
| **RabbitMQ** | 3.12+ | Message queue (optional) |

## Step 1: Install Java 17

### Ubuntu/Debian

```bash
# Install OpenJDK 17
sudo apt update
sudo apt install -y openjdk-17-jdk

# Verify installation
java -version

# Set JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
source ~/.bashrc
```

### macOS

```bash
# Using Homebrew
brew install openjdk@17

# Set JAVA_HOME
echo 'export JAVA_HOME=$(/usr/local/opt/openjdk@17/bin/java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | awk '{print $3}')' >> ~/.zshrc
source ~/.zshrc
```

## Step 2: Install Node.js 20

### Ubuntu/Debian

```bash
# Using NodeSource repository
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# Verify installation
node --version
npm --version
```

### macOS

```bash
# Using Homebrew
brew install node@20

# Verify installation
node --version
npm --version
```

## Step 3: Install PostgreSQL 16 with pgvector

### Ubuntu/Debian

```bash
# Add PostgreSQL repository
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
echo "deb http://apt.postgresql.org/pub/repos/apt/ $(lsb_release -cs)-pgdg main" | sudo tee /etc/apt/sources.list.d/pgdg.list

# Install PostgreSQL
sudo apt update
sudo apt install -y postgresql-16 postgresql-contrib-16

# Install build dependencies for pgvector
sudo apt install -y postgresql-16-pgvector build-essential git

# Verify installation
psql --version
```

### macOS

```bash
# Using Homebrew
brew install postgresql@16 pgvector

# Start PostgreSQL
brew services start postgresql@16
```

### Enable pgvector Extension

```bash
# Switch to postgres user
sudo -u postgres psql

# Inside psql:
CREATE DATABASE pullwise;
CREATE USER pullwise WITH PASSWORD 'pullwise_dev_2024';
GRANT ALL PRIVILEGES ON DATABASE pullwise TO pullwise;
\c pullwise
CREATE EXTENSION vector;
\q
```

## Step 4: Install Redis

### Ubuntu/Debian

```bash
# Install Redis
sudo apt install -y redis-server

# Configure Redis
sudo nano /etc/redis/redis.conf
# Set: supervised systemd
# Set: requirepass your_redis_password

# Restart Redis
sudo systemctl restart redis-server
sudo systemctl enable redis-server

# Verify installation
redis-cli ping
```

### macOS

```bash
# Using Homebrew
brew install redis

# Start Redis
brew services start redis

# Verify installation
redis-cli ping
```

## Step 5: Install RabbitMQ (Optional)

### Ubuntu/Debian

```bash
# Add RabbitMQ repository
curl -s https://packagecloud.io/install/repositories/rabbitmq/rabbitmq-server/script.deb.sh | sudo bash
curl -s https://packagecloud.io/install/repositories/rabbitmq/erlang/script.deb.sh | sudo bash

# Install RabbitMQ
sudo apt install -y rabbitmq-server

# Enable management plugin
sudo rabbitmq-plugins enable rabbitmq_management

# Create admin user
sudo rabbitmqctl add_user pullwise pullwise_password
sudo rabbitmqctl set_user_tags pullwise administrator
sudo rabbitmqctl set_permissions -p / pullwise ".*" ".*" ".*"

# Restart RabbitMQ
sudo systemctl restart rabbitmq-server
sudo systemctl enable rabbitmq-server

# Verify installation
sudo rabbitmqctl status
```

### macOS

```bash
# Using Homebrew
brew install rabbitmq

# Start RabbitMQ
brew services start rabbitmq

# Enable management plugin
rabbitmq-plugins enable rabbitmq_management
```

## Step 6: Install Backend

```bash
# Clone repository
git clone https://github.com/integralltech/pullwise-ai.git
cd pullwise-ai/backend

# Configure application
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml

# Edit configuration
nano src/main/resources/application-dev.yml
```

### application-dev.yml

```yaml
spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:postgresql://localhost:5432/pullwise
    username: pullwise
    password: pullwise_dev_2024
  redis:
    host: localhost
    port: 6379
    password: your_redis_password
  rabbitmq:
    host: localhost
    port: 5672
    username: pullwise
    password: pullwise_password

pullwise:
  security:
    jwt:
      secret: your-very-long-random-secret-key-min-256-bits
  github:
    client-id: your_github_client_id
    client-secret: your_github_client_secret
  llm:
    openrouter:
      api-key: sk-or-v1-your-key-here
    default-model: anthropic/claude-3.5-sonnet
```

### Build and Run Backend

```bash
# Build with Maven
./mvnw clean package -DskipTests

# Run backend
java -jar target/pullwise-backend-*.jar --spring.profiles.active=dev

# Or run with Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Create Systemd Service (Linux)

```bash
# Create service file
sudo nano /etc/systemd/system/pullwise-backend.service
```

```ini
[Unit]
Description=Pullwise Backend Service
After=network.target postgresql.service redis.service

[Service]
Type=simple
User=pullwise
WorkingDirectory=/opt/pullwise/backend
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="JAVA_OPTS=-Xmx2g -Xms1g"
ExecStart=/usr/bin/java -jar /opt/pullwise/backend/target/pullwise-backend-*.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# Create pullwise user
sudo useradd -r -s /bin/false pullwise

# Copy files
sudo mkdir -p /opt/pullwise
sudo cp -r backend /opt/pullwise/
sudo chown -R pullwise:pullwise /opt/pullwise

# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable pullwise-backend
sudo systemctl start pullwise-backend
sudo systemctl status pullwise-backend
```

## Step 7: Install Frontend

```bash
# Navigate to frontend
cd ../frontend

# Install dependencies
npm install

# Configure environment
cat > .env.production << EOF
VITE_API_URL=https://pullwise.example.com/api
VITE_WS_URL=wss://pullwise.example.com/ws
EOF

# Build for production
npm run build

# Output is in 'dist' directory
```

### Serve with Nginx

```bash
# Install Nginx
sudo apt install -y nginx

# Copy build files
sudo cp -r dist/* /var/www/pullwise/

# Create Nginx configuration
sudo nano /etc/nginx/sites-available/pullwise
```

```nginx
server {
    listen 80;
    server_name pullwise.example.com;

    root /var/www/pullwise;
    index index.html;

    # SPA routing
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Proxy API requests
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Proxy WebSocket
    location /ws {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
}
```

```bash
# Enable site
sudo ln -s /etc/nginx/sites-available/pullwise /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

## Step 8: Configure SSL/TLS

```bash
# Install Certbot
sudo apt install -y certbot python3-certbot-nginx

# Obtain certificate
sudo certbot --nginx -d pullwise.example.com

# Auto-renewal is configured automatically
sudo certbot renew --dry-run
```

## Verification

```bash
# Check backend health
curl http://localhost:8080/actuator/health

# Check frontend
curl https://pullwise.example.com

# Check logs
sudo journalctl -u pullwise-backend -f
```

## Upgrading

### Backend

```bash
cd /opt/pullwise/backend
sudo systemctl stop pullwise-backend

# Pull latest code
git pull origin main

# Rebuild
./mvnw clean package -DskipTests

# Restart
sudo systemctl start pullwise-backend
```

### Frontend

```bash
cd /opt/pullwise/frontend

# Pull latest code
git pull origin main

# Rebuild
npm install
npm run build

# Copy files
sudo cp -r dist/* /var/www/pullwise/
sudo systemctl reload nginx
```

## Troubleshooting

### Backend Won't Start

```bash
# Check logs
sudo journalctl -u pullwise-backend -n 100

# Check if port is available
netstat -tuln | grep 8080

# Check database connection
psql -U pullwise -h localhost -d pullwise -c "SELECT 1"
```

### Frontend Not Loading

```bash
# Check Nginx logs
sudo tail -f /var/log/nginx/error.log

# Check file permissions
ls -la /var/www/pullwise/
```

### Database Issues

```bash
# Check PostgreSQL status
sudo systemctl status postgresql

# Check connection
psql -U pullwise -h localhost -d pullwise
```

## Next Steps

- [Configuration](/docs/getting-started/configuration) - Customize your setup
- [Docker Installation](/docs/getting-started/installation/docker) - Containerized deployment
- [Production Deployment](/docs/deployment/docker/production) - Production considerations
