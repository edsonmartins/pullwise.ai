-- Habilita pgvector antes do Hibernate aplicar o schema (necessário para
-- entities que mapeiam columnDefinition = "vector(1536)").
CREATE EXTENSION IF NOT EXISTS vector;
