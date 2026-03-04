-- Review Coverage Tracking
-- Tracks what percentage of changed lines have been reviewed per file

CREATE TABLE review_coverage (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    file_path VARCHAR(500) NOT NULL,
    total_lines_changed INTEGER NOT NULL DEFAULT 0,
    lines_reviewed INTEGER NOT NULL DEFAULT 0,
    coverage_percentage DECIMAL(5,2) DEFAULT 0.0,
    first_reviewed_at TIMESTAMP,
    last_reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(review_id, file_path)
);

CREATE INDEX idx_review_coverage_review ON review_coverage(review_id);

-- Add coverage percentage to reviews table
ALTER TABLE reviews ADD COLUMN coverage_percentage DECIMAL(5,2);
