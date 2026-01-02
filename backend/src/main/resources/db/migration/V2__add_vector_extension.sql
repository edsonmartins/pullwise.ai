-- V2__add_vector_extension.sql
-- Adiciona suporte a documentos de conhecimento (RAG)
-- Nota: pgvector não está instalado, usando TEXT para embeddings

-- ============================================
-- Knowledge Documents (sem pgvector por enquanto)
-- ============================================
CREATE TABLE IF NOT EXISTS knowledge_documents (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES projects(id) ON DELETE CASCADE,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    metadata TEXT,
    embedding TEXT,
    chunk_index INTEGER,
    total_chunks INTEGER,
    source_type VARCHAR(50),
    source_path VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kd_project ON knowledge_documents(project_id);
CREATE INDEX IF NOT EXISTS idx_kd_org ON knowledge_documents(organization_id);
CREATE INDEX IF NOT EXISTS idx_kd_source ON knowledge_documents(source_type);
CREATE INDEX IF NOT EXISTS idx_kd_active ON knowledge_documents(is_active);

-- Trigger para updated_at
CREATE TRIGGER update_knowledge_documents_updated_at BEFORE UPDATE ON knowledge_documents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- Comentários
-- ============================================
-- embedding: Armazena o vetor como JSON (quando pgvector disponível, alterar para vector(1536))
-- source_type: README, CONTRIBUTING, CODE, DOC_URL, etc.
-- Para habilitar busca vetorial completa, instale pgvector e altere coluna embedding
