-- V1__initial_schema.sql
-- Schema inicial do Pullwise
-- Cria todas as tabelas principais do sistema

-- ============================================
-- Organizations
-- ============================================
CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(50) UNIQUE,
    logo_url VARCHAR(500),
    plan_type VARCHAR(20) NOT NULL DEFAULT 'FREE',
    max_repositories INTEGER,
    max_reviews_per_month INTEGER,
    github_org_id VARCHAR(100),
    bitbucket_workspace_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Users
-- ============================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    avatar_url VARCHAR(500),
    github_id VARCHAR(100),
    github_login VARCHAR(100),
    bitbucket_uuid VARCHAR(100),
    bitbucket_username VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- ============================================
-- Organization Members
-- ============================================
CREATE TABLE organization_members (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    is_owner BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, user_id)
);

CREATE INDEX idx_org_members_org ON organization_members(organization_id);
CREATE INDEX idx_org_members_user ON organization_members(user_id);

-- ============================================
-- Projects
-- ============================================
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    platform VARCHAR(20) NOT NULL,
    repository_url VARCHAR(500) NOT NULL,
    repository_id VARCHAR(100),
    github_installation_id BIGINT,
    webhook_secret VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    auto_review_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, name)
);

CREATE INDEX idx_projects_org ON projects(organization_id);
CREATE INDEX idx_projects_repo_id ON projects(repository_id);

-- ============================================
-- Pull Requests
-- ============================================
CREATE TABLE pull_requests (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    platform VARCHAR(20) NOT NULL,
    pr_id BIGINT NOT NULL,
    pr_number INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    source_branch VARCHAR(100) NOT NULL,
    target_branch VARCHAR(100) NOT NULL,
    author_name VARCHAR(100),
    author_email VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP,
    merged_at TIMESTAMP,
    is_merged BOOLEAN DEFAULT FALSE,
    is_closed BOOLEAN DEFAULT FALSE,
    review_url VARCHAR(500)
);

CREATE INDEX idx_pr_project ON pull_requests(project_id);
CREATE INDEX idx_pr_platform_pr_id ON pull_requests(platform, pr_id);
CREATE INDEX idx_pr_number ON pull_requests(project_id, pr_number);

-- ============================================
-- Reviews
-- ============================================
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    pull_request_id BIGINT NOT NULL REFERENCES pull_requests(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration_ms BIGINT,
    files_analyzed INTEGER,
    lines_added_analyzed INTEGER,
    lines_removed_analyzed INTEGER,
    sast_enabled BOOLEAN DEFAULT TRUE,
    llm_enabled BOOLEAN DEFAULT TRUE,
    rag_enabled BOOLEAN DEFAULT FALSE,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    review_comment_id VARCHAR(100)
);

CREATE INDEX idx_reviews_pr ON reviews(pull_request_id);
CREATE INDEX idx_reviews_status ON reviews(status);

-- ============================================
-- Issues
-- ============================================
CREATE TABLE issues (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    severity VARCHAR(20) NOT NULL,
    type VARCHAR(30) NOT NULL,
    source VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    file_path VARCHAR(500),
    line_start INTEGER,
    line_end INTEGER,
    rule_id VARCHAR(100),
    suggestion TEXT,
    code_snippet TEXT,
    fixed_code TEXT,
    comment_id VARCHAR(100),
    is_false_positive BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_issues_review_id ON issues(review_id);
CREATE INDEX idx_issues_severity ON issues(severity);
CREATE INDEX idx_issues_source ON issues(source);

-- ============================================
-- Configurations
-- ============================================
CREATE TABLE configurations (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES projects(id) ON DELETE CASCADE,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    scope VARCHAR(20) NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    value_type VARCHAR(20) DEFAULT 'STRING',
    is_sensitive BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, scope, config_key)
);

CREATE INDEX idx_configs_org ON configurations(organization_id);
CREATE INDEX idx_configs_project ON configurations(project_id);

-- ============================================
-- Subscriptions
-- ============================================
CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL UNIQUE REFERENCES organizations(id) ON DELETE CASCADE,
    plan_type VARCHAR(20) NOT NULL,
    stripe_subscription_id VARCHAR(100),
    stripe_customer_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    current_period_start DATE,
    current_period_end DATE,
    cancel_at_period_end BOOLEAN DEFAULT FALSE,
    trial_start DATE,
    trial_end DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subscriptions_org ON subscriptions(organization_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);

-- ============================================
-- Usage Records
-- ============================================
CREATE TABLE usage_records (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    project_id BIGINT REFERENCES projects(id) ON DELETE SET NULL,
    usage_date DATE NOT NULL,
    period VARCHAR(7) NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    metric_value BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_usage_org_period ON usage_records(organization_id, period);
CREATE INDEX idx_usage_date ON usage_records(usage_date);
CREATE INDEX idx_usage_project ON usage_records(project_id);

-- ============================================
-- Triggers para updated_at
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_organizations_updated_at BEFORE UPDATE ON organizations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_projects_updated_at BEFORE UPDATE ON projects
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pull_requests_updated_at BEFORE UPDATE ON pull_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reviews_updated_at BEFORE UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_configurations_updated_at BEFORE UPDATE ON configurations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_subscriptions_updated_at BEFORE UPDATE ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- Dados iniciais
-- ============================================
INSERT INTO organizations (name, slug, plan_type, max_repositories, max_reviews_per_month) VALUES
('Pullwise Demo', 'pullwise-demo', 'FREE', 3, 50);
