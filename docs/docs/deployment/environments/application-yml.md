# PostgreSQL Configuration

Configure and optimize PostgreSQL for Pullwise.

## Overview

Pullwise uses PostgreSQL 16 with the pgvector extension for:

- Primary data storage
- Vector embeddings for RAG
- Full-text search
- JSON data storage

## Configuration

### postgresql.conf

Key settings for Pullwise:

```ini
# Connection settings
max_connections = 200
shared_buffers = 256MB
effective_cache_size = 2GB
maintenance_work_mem = 64MB
work_mem = 16MB

# WAL settings
wal_level = replica
wal_buffers = 16MB
checkpoint_completion_target = 0.9
max_wal_size = 2GB

# Query planner
random_page_cost = 1.1
effective_io_concurrency = 200

# Logging
log_min_duration_statement = 1000  # Log slow queries
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '
log_checkpoints = on
log_connections = on
log_disconnections = on
log_lock_waits = on

# pgvector
shared_preload_libraries = 'vector'
```

## pgvector Extension

### Enable pgvector

```sql
-- Enable extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Verify
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
```

### Vector Operations

```sql
-- Create vector column
ALTER TABLE embeddings
ADD COLUMN embedding vector(1536);

-- Create vector index
CREATE INDEX ON embeddings
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);
```

## Connection Pooling

### HikariCP Configuration

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 30000
      connection-timeout: 30000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

## Backup Strategy

### pg_dump Backup

```bash
# Full backup
pg_dump -U pullwise -h localhost -F c -f pullwise_backup.sql

# Custom format backup
pg_dump -U pullwise -h localhost -F d -f pullwise_backup.dump

# Schema only
pg_dump -U pullwise -h localhost -s -f schema.sql
```

### Automated Backup

```bash
# Backup script
cat > /usr/local/bin/backup-pullwise.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/backups/postgres"
DATE=$(date +%Y%m%d_%H%M%S)

pg_dump -U pullwise -h localhost -F c \
  -f $BACKUP_DIR/pullwise_$DATE.sql

# Keep last 30 days
find $BACKUP_DIR -mtime +30 -delete
EOF

chmod +x /usr/local/bin/backup-pullwise.sh

# Add to crontab
0 2 * * * /usr/local/bin/backup-pullwise.sh
```

### Restore Backup

```bash
# Restore from SQL file
psql -U pullwise -h localhost -f pullwise_backup.sql

# Restore from dump
pg_restore -U pullwise -h localhost -d pullwise -f pullwise_backup.dump
```

## Replication

### Master-Slave Replication

```sql
-- On master
CREATE ROLE replication_user WITH REPLICATION LOGIN PASSWORD 'replica_pass';
GRANT REPLICATION ON DATABASE pullwise TO replication_user;

-- Get replication status
SELECT * FROM pg_stat_replication;
```

### Standby Configuration

```ini
# postgresql.conf on standby
hot_standby = on
standby_mode = on
primary_conninfo = 'host=primary port=5432 user=replication_user password=replica_pass'
restore_command = 'cp /var/lib/postgresql/archive/%f %p'
archive_cleanup_command = 'pg_archivecleanup /var/lib/postgresql/archive %r'
```

## Monitoring

### Queries

```sql
-- Active connections
SELECT count(*) FROM pg_stat_activity;

-- Database size
SELECT pg_size_pretty(pg_database_size('pullwise'));

-- Table sizes
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Slow queries
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;

-- Index usage
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC;
```

## Performance Tuning

### VACUUM Settings

```sql
-- Enable autovacuum
ALTER DATABASE pullwise SET autovacuum = ON;
ALTER DATABASE pullwise SET autovacuum_vacuum_scale_factor = 0.1;
ALTER DATABASE pullwise SET autovacuum_analyze_scale_factor = 0.05;
```

### Table Statistics

```sql
-- Update statistics
ANALYZE reviews;
ANALYZE issues;
ANALYZE plugins;
```

### Index Optimization

```sql
-- Create partial index
CREATE INDEX CONCURRENTLY idx_reviews_status
ON reviews(status) WHERE status != 'COMPLETED';

-- Create expression index
CREATE INDEX CONCURRENTLY idx_issues_severity_weight
ON issues (
  CASE severity
    WHEN 'CRITICAL' THEN 4
    WHEN 'HIGH' THEN 3
    WHEN 'MEDIUM' THEN 2
    WHEN 'LOW' THEN 1
  END
);
```

## Row-Level Security

### Enable RLS

```sql
-- Enable RLS on reviews table
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;

-- Policy for organizations
CREATE POLICY reviews_org_policy ON reviews
FOR ALL
TO pullwise_app
USING (
  organization_id = current_setting('app.current_org')::BIGINT
);

-- Set organization context
SET app.current_org = '123';
```

## Troubleshooting

### Connection Issues

```bash
# Check PostgreSQL is running
pg_isready -U pullwise

# Check connections
psql -U pullwise -c "SELECT count(*) FROM pg_stat_activity;"
```

### Performance Issues

```sql
-- Check for locks
SELECT * FROM pg_stat_activity WHERE wait_event_type = 'Lock';

-- Check for blocking queries
SELECT pid, query, state, wait_event_type
FROM pg_stat_activity
WHERE wait_event_type IS NOT NULL;
```

## Next Steps

- [Database](/docs/developer-guide/architecture/database) - Schema details
- [Production](/docs/deployment/docker/production) - Production setup
- [Monitoring](/docs/deployment/monitoring/prometheus) - Metrics
