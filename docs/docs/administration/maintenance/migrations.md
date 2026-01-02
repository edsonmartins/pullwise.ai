# Database Migrations

Manage database schema migrations with Flyway.

## Overview

Pullwise uses Flyway for database migrations:

- Versioned migration scripts
- Automatic schema updates
- Rollback support
- Multi-environment support

## Migration Files

### Naming Convention

```
SQL Migrations: V{version}__{description}.sql
Java Migrations: V{version}__{description}.java
Repeatable: R__{description}.sql
```

Examples:
```
V1__init.sql
V2__add_projects.sql
V3__add_reviews.sql
R__load_data.sql
```

### Migration Structure

```
backend/src/main/resources/db/migration/
├── migration/
│   ├── V1__init.sql
│   ├── V2__add_organizations.sql
│   ├── V3__add_projects.sql
│   ├── V4__add_reviews.sql
│   ├── V5__add_issues.sql
│   ├── V6__add_plugins.sql
│   └── V7__add_analytics.sql
└── ...
```

## Configuration

### Flyway Configuration

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    out-of-order: false
    validate-on-migrate: true
```

### Per-Environment Configuration

```yaml
# application-dev.yml
spring:
  flyway:
    enabled: true
    clean-disabled: false  # Allow clean in dev

# application-prod.yml
spring:
  flyway:
    enabled: true
    clean-disabled: true   # Never clean in prod
```

## Writing Migrations

### Initial Schema

```sql
-- V1__init.sql

-- Organizations
CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Projects
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    repository_url VARCHAR(500),
    default_branch VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, name)
);

-- Reviews
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    branch VARCHAR(255) NOT NULL,
    commit_sha VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Issues
CREATE TABLE issues (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    severity VARCHAR(20) NOT NULL,
    type VARCHAR(50) NOT NULL,
    rule VARCHAR(100) NOT NULL,
    file_path TEXT NOT NULL,
    start_line INTEGER,
    end_line INTEGER,
    message TEXT NOT NULL,
    suggestion TEXT,
    code_snippet TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Adding Indexes

```sql
-- V8__add_indexes.sql

CREATE INDEX idx_reviews_project_id ON reviews(project_id);
CREATE INDEX idx_reviews_status ON reviews(status);
CREATE INDEX idx_reviews_created_at ON reviews(created_at DESC);

CREATE INDEX idx_issues_review_id ON issues(review_id);
CREATE INDEX idx_issues_severity ON issues(severity);
CREATE INDEX idx_issues_type ON issues(type);

CREATE INDEX idx_reviews_status_created ON reviews(status, created_at DESC);
```

### Adding Columns

```sql
-- V9__add_review_health_score.sql

ALTER TABLE reviews ADD COLUMN health_score INTEGER;
ALTER TABLE reviews ADD COLUMN issues_count INTEGER DEFAULT 0;
ALTER TABLE reviews ADD COLUMN critical_count INTEGER DEFAULT 0;

UPDATE reviews SET health_score = 100 WHERE health_score IS NULL;

ALTER TABLE reviews ALTER COLUMN health_score SET NOT NULL;
ALTER TABLE reviews ALTER COLUMN health_score SET DEFAULT 100;
```

### Adding Foreign Keys

```sql
-- V10__add_users.sql

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    avatar_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE organization_members (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, user_id)
);
```

## Running Migrations

### Automatic on Startup

```bash
# Migrations run automatically on Spring Boot startup
./mvnw spring-boot:run
```

### Manual with Maven

```bash
# Run migrations
./mvnw flyway:migrate

# Validate migrations
./mvnw flyway:validate

# Info
./mvnw flyway:info
```

### Docker Compose

```yaml
services:
  pullwise:
    image: pullwise/pullwise:1.0.0
    environment:
      SPRING_FLYWAY_ENABLED: "true"
    depends_on:
      postgres:
        condition: service_healthy
```

## Migration Best Practices

### 1. Use Transactions

```sql
-- Good: All-or-nothing
BEGIN;
ALTER TABLE reviews ADD COLUMN health_score INTEGER;
UPDATE reviews SET health_score = 100;
ALTER TABLE reviews ALTER COLUMN health_score SET NOT NULL;
COMMIT;
```

### 2. Add Defaults First

```sql
-- Good: Add column with default
ALTER TABLE reviews ADD COLUMN status VARCHAR(50) DEFAULT 'PENDING';
ALTER TABLE reviews ALTER COLUMN status SET NOT NULL;

-- Bad: Add NOT NULL without default
ALTER TABLE reviews ADD COLUMN status VARCHAR(50) NOT NULL; -- ERROR
```

### 3. Backward Compatible

```sql
-- Add new column (nullable)
ALTER TABLE reviews ADD COLUMN tags JSONB;

-- Update data
UPDATE reviews SET tags = '[]'::jsonb WHERE tags IS NULL;

-- Make NOT NULL
ALTER TABLE reviews ALTER COLUMN tags SET NOT NULL;
```

### 4. Index Creation Concurrently

```sql
-- Good: Concurrent index creation
CREATE INDEX CONCURRENTLY idx_reviews_status ON reviews(status);

-- Bad: Blocks writes
CREATE INDEX idx_reviews_status ON reviews(status);
```

## Rollback

### Flyway Undo

```yaml
# application.yml
spring:
  flyway:
    undo-enabled: true
```

### Undo Script

```sql
-- U1__init.sql

DROP INDEX IF EXISTS idx_issues_severity;
DROP INDEX IF EXISTS idx_issues_review_id;
DROP TABLE IF EXISTS issues;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS organizations;
```

### Manual Rollback

```sql
-- Rollback specific migration
DELETE FROM flyway_schema_history WHERE version = '8';
DROP INDEX IF EXISTS idx_reviews_status;
```

## Troubleshooting

### Check Migration Status

```sql
-- View migration history
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- Check pending migrations
./mvnw flyway:info
```

### Failed Migration

```bash
# View error
./mvnw flyway:migrate

# Manual fix
psql -U pullwise -d pullwise -c "DELETE FROM flyway_schema_history WHERE version = '8';"

# Re-run
./mvnw flyway:migrate
```

### Out of Order

```yaml
# Allow out-of-order migrations
spring:
  flyway:
    out-of-order: true
```

### Baseline Existing Database

```bash
# Baseline existing database
./mvnw flyway:baseline -Dflyway.baselineVersion=1
```

## Production Migrations

### Pre-Migration Checklist

- [ ] Test migration in staging
- [ ] Backup database
- [ ] Estimate migration time
- [ ] Plan rollback
- [ ] Notify users of downtime

### Zero-Downtime Migration

```sql
-- Step 1: Add column (nullable)
ALTER TABLE reviews ADD COLUMN new_field VARCHAR(255);

-- Deploy code that writes to both fields

-- Step 2: Backfill data
UPDATE reviews SET new_field = old_field;

-- Deploy code that reads from new field

-- Step 3: Make NOT NULL
ALTER TABLE reviews ALTER COLUMN new_field SET NOT NULL;

-- Step 4: Remove old field
ALTER TABLE reviews DROP COLUMN old_field;
```

### Large Table Operations

```sql
-- Batch updates
DO $$
DECLARE
    batch_size INT := 1000;
    updated INT := 1;
BEGIN
    WHILE updated > 0 LOOP
        UPDATE reviews
        SET new_field = default_value
        WHERE new_field IS NULL
        LIMIT batch_size;

        GET DIAGNOSTICS updated = ROW_COUNT;
        COMMIT;
        RAISE NOTICE 'Updated % records', updated;
    END LOOP;
END $$;
```

## Next Steps

- [Backups](/docs/administration/maintenance/backups) - Backup procedures
- [Updates](/docs/administration/maintenance/updates) - Update procedures
- [Monitoring](/docs/administration/maintenance/monitoring) - System monitoring
