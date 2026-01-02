-- V4__enable_pgvector.sql
-- Habilita extensão pgvector para busca vetorial (RAG)
-- Requer: EXTENSION pgvector instalado no PostgreSQL

-- Habilitar extensão pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- Alterar tabela knowledge_documents para usar vector type
ALTER TABLE knowledge_documents
    ALTER COLUMN embedding TYPE vector(1536) USING embedding::vector(1536);

-- Criar índice ivfflat para busca vetorial eficiente
CREATE INDEX IF NOT EXISTS idx_kd_embedding
    ON knowledge_documents
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- Tabela para RAG decisions (cache de decisões do LLM Router)
CREATE TABLE IF NOT EXISTS llm_routing_decisions (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT REFERENCES reviews(id) ON DELETE CASCADE,
    task_type VARCHAR(50) NOT NULL,
    selected_model VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    input_tokens INTEGER,
    output_tokens INTEGER,
    cost_usd DECIMAL(10, 4),
    latency_ms INTEGER,
    reasoning TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_lrd_review ON llm_routing_decisions(review_id);
CREATE INDEX IF NOT EXISTS idx_lrd_task ON llm_routing_decisions(task_type);
CREATE INDEX IF NOT EXISTS idx_lrd_model ON llm_routing_decisions(selected_model);

-- Tabela para cache de embeddings
CREATE TABLE IF NOT EXISTS embedding_cache (
    id BIGSERIAL PRIMARY KEY,
    content_hash VARCHAR(64) UNIQUE NOT NULL,
    embedding vector(1536) NOT NULL,
    model VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ec_hash ON embedding_cache(content_hash);
CREATE INDEX IF NOT EXISTS idx_ec_accessed ON embedding_cache(accessed_at);

-- Tabela para plugin configurations
CREATE TABLE IF NOT EXISTS plugin_configurations (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    plugin_id VARCHAR(100) NOT NULL,
    plugin_type VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    config JSONB,
    version VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, plugin_id)
);

CREATE INDEX IF NOT EXISTS idx_pc_org ON plugin_configurations(organization_id);
CREATE INDEX IF NOT EXISTS idx_pc_type ON plugin_configurations(plugin_type);
CREATE INDEX IF NOT EXISTS idx_pc_enabled ON plugin_configurations(enabled);

-- Tabela para fix suggestions (auto-fix)
CREATE TABLE IF NOT EXISTS fix_suggestions (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT REFERENCES reviews(id) ON DELETE CASCADE,
    issue_id BIGINT REFERENCES issues(id) ON DELETE CASCADE,
    file_path VARCHAR(500) NOT NULL,
    line_start INTEGER NOT NULL,
    line_end INTEGER NOT NULL,
    original_code TEXT NOT NULL,
    suggested_code TEXT NOT NULL,
    description TEXT,
    confidence VARCHAR(20),  -- HIGH, MEDIUM, LOW
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, APPLIED, REJECTED
    applied_by BIGINT REFERENCES users(id),
    applied_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fs_review ON fix_suggestions(review_id);
CREATE INDEX IF NOT EXISTS idx_fs_issue ON fix_suggestions(issue_id);
CREATE INDEX IF NOT EXISTS idx_fs_status ON fix_suggestions(status);

-- Comentários
COMMENT ON TABLE llm_routing_decisions IS 'Registro de decisões do router de LLM para analytics';
COMMENT ON TABLE embedding_cache IS 'Cache de embeddings para evitar reprocessamento';
COMMENT ON TABLE plugin_configurations IS 'Configurações de plugins por organização';
COMMENT ON TABLE fix_suggestions IS 'Sugestões de correção automática (auto-fix)';
