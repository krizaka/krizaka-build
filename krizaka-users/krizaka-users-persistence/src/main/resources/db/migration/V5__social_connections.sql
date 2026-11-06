-- Flyway V5 (ADR-016 slice 1): the identity provider connections, their client secret sealed under AUTH_VAULT_KEY.
-- sealed_client_secret is the envelope "v2.<key_id>.<wrapped data key>.<sealed secret>"; it is never the clear value.
CREATE TABLE IF NOT EXISTS social_connection (
    provider VARCHAR(60) PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL,
    sealed_client_secret TEXT,
    key_id VARCHAR(32),
    source VARCHAR(20) NOT NULL DEFAULT 'env',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT social_connection_sealed CHECK (sealed_client_secret IS NULL OR sealed_client_secret LIKE 'v2.%')
);
