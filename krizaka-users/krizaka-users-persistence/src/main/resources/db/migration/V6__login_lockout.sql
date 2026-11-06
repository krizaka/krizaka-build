-- Flyway V6 (ADR-016 slice 2): the lockout of failed sign-ins is durable, shared by every instance, and
-- its thresholds live here, changed by an administrator (audited). subject_hash is a SHA-256: no mailbox.
CREATE TABLE IF NOT EXISTS login_lockout (
    scope VARCHAR(8) NOT NULL,
    subject_hash CHAR(64) NOT NULL,
    failures INTEGER NOT NULL,
    last_failure_at TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_until TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (scope, subject_hash),
    CONSTRAINT login_lockout_scope CHECK (scope IN ('PAIR', 'ACCOUNT'))
);
CREATE INDEX IF NOT EXISTS idx_login_lockout_last_failure ON login_lockout(last_failure_at);

CREATE TABLE IF NOT EXISTS lockout_settings (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    threshold INTEGER NOT NULL CHECK (threshold >= 1),
    account_threshold INTEGER NOT NULL,
    base_delay_seconds BIGINT NOT NULL CHECK (base_delay_seconds > 0),
    max_delay_seconds BIGINT NOT NULL CHECK (max_delay_seconds >= base_delay_seconds),
    forget_after_seconds BIGINT NOT NULL CHECK (forget_after_seconds > 0),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    CONSTRAINT lockout_settings_account CHECK (account_threshold >= threshold)
);
INSERT INTO lockout_settings (id, threshold, account_threshold, base_delay_seconds, max_delay_seconds, forget_after_seconds)
VALUES (1, 5, 25, 60, 900, 1800)
ON CONFLICT (id) DO NOTHING;
