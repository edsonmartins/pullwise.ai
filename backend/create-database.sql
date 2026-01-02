-- Script para criar o banco de dados Pullwise
-- Execute no PostgreSQL em 192.168.1.110:5444

-- Criar banco de dados
CREATE DATABASE pullwise_db
    WITH
    OWNER = admin
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0
    CONNECTION LIMIT = -1;

-- Conceder permissões
GRANT ALL PRIVILEGES ON DATABASE pullwise_db TO admin;

-- Habilitar extensão pgvector (necessário para RAG)
\c pullwise_db
CREATE EXTENSION IF NOT EXISTS vector;

-- Sair
\c postgres

-- Verificar
\l pullwise_db
