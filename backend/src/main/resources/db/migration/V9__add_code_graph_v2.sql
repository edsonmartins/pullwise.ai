-- V9__add_code_graph_v2.sql
-- Code Graph v2: persistência do grafo de dependências para Blast-Radius
--
-- Substitui o grafo puramente in-memory por nós/arestas em Postgres
-- para suportar BFS forward via CTE recursiva, confidence tiers (EXTRACTED/
-- INFERRED/AMBIGUOUS) e risk scoring no pipeline 4-pass.

-- ============================================
-- Code Graph Nodes
-- ============================================
CREATE TABLE code_graph_nodes (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    qualified_name VARCHAR(512) NOT NULL,
    simple_name VARCHAR(255) NOT NULL,
    kind VARCHAR(20) NOT NULL,           -- FILE, CLASS, INTERFACE, ENUM, METHOD
    file_path VARCHAR(512) NOT NULL,
    language VARCHAR(20) NOT NULL,       -- JAVA, KOTLIN, TYPESCRIPT, ...
    line_start INTEGER,
    line_end INTEGER,
    is_test BOOLEAN NOT NULL DEFAULT FALSE,
    hotspot_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    has_test_coverage BOOLEAN NOT NULL DEFAULT FALSE,
    security_keywords TEXT,            -- CSV de keywords match
    extra TEXT,                         -- JSON serializado
    file_hash VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_code_graph_nodes_project_qn UNIQUE (project_id, qualified_name)
);

CREATE INDEX idx_cgn_project_file ON code_graph_nodes(project_id, file_path);
CREATE INDEX idx_cgn_project_kind ON code_graph_nodes(project_id, kind);
CREATE INDEX idx_cgn_simple_name ON code_graph_nodes(project_id, simple_name);

-- ============================================
-- Code Graph Edges
-- ============================================
CREATE TABLE code_graph_edges (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    source_qualified VARCHAR(512) NOT NULL,
    target_qualified VARCHAR(512) NOT NULL,
    kind VARCHAR(20) NOT NULL,           -- CALLS, IMPORTS_FROM, INHERITS, IMPLEMENTS, REFERENCES, CONTAINS
    confidence_tier VARCHAR(15) NOT NULL DEFAULT 'EXTRACTED',
    confidence_weight DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    source_file_path VARCHAR(512),
    source_line INTEGER,
    extra TEXT,                         -- JSON serializado
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_code_graph_edges_unique UNIQUE (project_id, source_qualified, target_qualified, kind)
);

-- Índice composto crítico para CTE recursiva (BFS forward)
CREATE INDEX idx_cge_source_kind ON code_graph_edges(project_id, source_qualified, kind);
-- Índice para BFS reverso (callers de um nó)
CREATE INDEX idx_cge_target_kind ON code_graph_edges(project_id, target_qualified, kind);
