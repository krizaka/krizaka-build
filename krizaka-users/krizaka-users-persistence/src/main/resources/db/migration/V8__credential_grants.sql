-- Flyway V8 (GHSA-3pf7-rgf2-4x3m, krizaka-internal#2359 slice G): a provider key leaves krizaka-users only against a
-- short-lived, single-use grant bound to the user, the provider and the run. Only the SHA-256 of the grant is stored.
CREATE TABLE IF NOT EXISTS credential_grants (
    grant_hash CHAR(64) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider_name VARCHAR(255) NOT NULL,
    run_id VARCHAR(128) NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_credential_grants_expires ON credential_grants(expires_at);
