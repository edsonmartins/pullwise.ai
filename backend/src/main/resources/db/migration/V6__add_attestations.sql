-- Review Attestation System
-- Cryptographic proof that code was reviewed for compliance/audit purposes

CREATE TABLE review_attestations (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    payload JSONB NOT NULL,
    payload_hash VARCHAR(128) NOT NULL,
    signature VARCHAR(512) NOT NULL,
    algorithm VARCHAR(50) NOT NULL DEFAULT 'HmacSHA256',
    key_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(review_id)
);

CREATE INDEX idx_attestation_review ON review_attestations(review_id);
CREATE INDEX idx_attestation_hash ON review_attestations(payload_hash);
