-- Flyway V2 (runs on deploy, as the service role). Idempotent. New installs get it from 10-identity.sql.
CREATE TABLE IF NOT EXISTS admin_audit (
    id BIGSERIAL PRIMARY KEY,
    at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_id VARCHAR(255),
    actor_label VARCHAR(255) NOT NULL,
    action VARCHAR(60) NOT NULL,
    target_id VARCHAR(255),
    detail TEXT
);
CREATE INDEX IF NOT EXISTS idx_admin_audit_at ON admin_audit(at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_audit_target ON admin_audit(target_id);
