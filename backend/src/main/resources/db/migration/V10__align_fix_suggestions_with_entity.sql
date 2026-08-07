-- Alinha a tabela fix_suggestions com a entidade FixSuggestion.
--
-- Drift histórico: a entidade evoluiu (renomeações e novos campos de
-- rastreamento de auto-fix) sem migrations correspondentes, o que foi
-- mascarado por ddl-auto=create-drop nos testes. Esta migration corrige
-- o schema; dados existentes são preservados nas renomeações.

-- Renomeações (preserva dados)
ALTER TABLE fix_suggestions RENAME COLUMN suggested_code TO fixed_code;
ALTER TABLE fix_suggestions RENAME COLUMN description TO explanation;
ALTER TABLE fix_suggestions RENAME COLUMN line_start TO start_line;
ALTER TABLE fix_suggestions RENAME COLUMN line_end TO end_line;

-- Novas colunas (rastreamento de aplicação/revisão e métricas de LLM)
ALTER TABLE fix_suggestions ADD COLUMN branch_name VARCHAR(255);
ALTER TABLE fix_suggestions ADD COLUMN applied_commit_hash VARCHAR(100);
ALTER TABLE fix_suggestions ADD COLUMN reviewed_by VARCHAR(255);
ALTER TABLE fix_suggestions ADD COLUMN reviewed_at TIMESTAMP;
ALTER TABLE fix_suggestions ADD COLUMN error_message TEXT;
ALTER TABLE fix_suggestions ADD COLUMN model_used VARCHAR(100);
ALTER TABLE fix_suggestions ADD COLUMN input_tokens INTEGER;
ALTER TABLE fix_suggestions ADD COLUMN output_tokens INTEGER;
ALTER TABLE fix_suggestions ADD COLUMN estimated_cost DOUBLE PRECISION;
ALTER TABLE fix_suggestions ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Relaxar NOT NULL herdados que a entidade declara nullable
ALTER TABLE fix_suggestions ALTER COLUMN file_path DROP NOT NULL;
ALTER TABLE fix_suggestions ALTER COLUMN original_code DROP NOT NULL;
ALTER TABLE fix_suggestions ALTER COLUMN fixed_code DROP NOT NULL;
ALTER TABLE fix_suggestions ALTER COLUMN start_line DROP NOT NULL;
ALTER TABLE fix_suggestions ALTER COLUMN end_line DROP NOT NULL;

-- Defaults alinhados aos @Builder.Default da entidade
ALTER TABLE fix_suggestions ALTER COLUMN status SET DEFAULT 'PENDING';

-- Índices declarados na entidade (@Table indexes)
CREATE INDEX IF NOT EXISTS idx_fix_review_id ON fix_suggestions(review_id);
CREATE INDEX IF NOT EXISTS idx_fix_issue_id ON fix_suggestions(issue_id);
CREATE INDEX IF NOT EXISTS idx_fix_status ON fix_suggestions(status);
CREATE INDEX IF NOT EXISTS idx_fix_confidence ON fix_suggestions(confidence);
