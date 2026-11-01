-- ============================================================================
-- ORAZAKA — Local DB bootstrap · 10 — IDENTITY CONTEXT
-- ----------------------------------------------------------------------------
-- Owner: Identity service (krizaka-users-core + persistence-identity).
-- Everything "who is the actor and what may they do": users, credentials,
-- authorities, tokens, profiles, rate-limit tiers, per-user model prefs.
-- Other contexts reference the user ONLY by an opaque ActorId — no inbound or
-- outbound cross-context FK. Phase 2 cutover: these tables live in their OWN
-- database (krizaka_users_db) under the service's own role, created here.
-- ============================================================================

-- The password is NOT set here. psql 15 cannot read the environment (\getenv is 16+)
-- and ERR-125 bans a shell script, so `orazaka start` applies ALTER ROLE from
-- IDENTITY_DB_PASSWORD once the container is healthy. A role created without a
-- password cannot authenticate, so a skipped step fails closed rather than leaving a
-- guessable one — which is what the committed literal was (ADR-035, audit #5).
CREATE ROLE krizaka_users LOGIN;
CREATE DATABASE krizaka_users_db OWNER krizaka_users;
\c krizaka_users_db
SET ROLE krizaka_users;

CREATE TABLE rate_limit_tiers (
    id VARCHAR(50) PRIMARY KEY,
    capacity INT NOT NULL,
    refill_tokens INT NOT NULL,
    refill_seconds INT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    -- Canonical user preferences (settings / GraphQL `me` / updatePreferences write here).
    -- NOTE: user_profiles.raw_preferences is a parallel context-injection copy
    -- (merged by ContextMapper). Consolidating the two is a tracked backend follow-up — it
    -- touches the non-null UserProfile domain record + ContextMapper + identity tests.
    preferences TEXT,
    provider VARCHAR(50) DEFAULT 'local' NOT NULL,
    provider_id VARCHAR(255) DEFAULT NULL,
    rate_limit_tier VARCHAR(50) REFERENCES rate_limit_tiers(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    password_changed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT unique_provider_user UNIQUE (provider, provider_id)
);

CREATE TABLE authorities (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) REFERENCES users(id) ON DELETE CASCADE,
    authority_name VARCHAR(100) NOT NULL,
    CONSTRAINT unique_user_authority UNIQUE (user_id, authority_name)
);
CREATE INDEX idx_authorities_user ON authorities(user_id);

CREATE TABLE verification_tokens (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) REFERENCES users(id) ON DELETE CASCADE,
    token_type VARCHAR(100) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expiry_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_interceptions (
    user_id VARCHAR(255) REFERENCES users(id) ON DELETE CASCADE,
    interception_type VARCHAR(100) NOT NULL,
    schema_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, interception_type)
);

CREATE TABLE user_profiles (
    user_id VARCHAR(255) PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    theme VARCHAR(50) DEFAULT 'emerald',
    -- Application-defined profile attributes (JSON): typically the answers of the application's
    -- onboarding form. The users service stores them and never interprets a key.
    raw_preferences TEXT
);

CREATE TABLE user_credentials (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider_name VARCHAR(255) NOT NULL,
    api_key VARCHAR(1024) NOT NULL
);

-- Inbound Personal Access Tokens: authenticate a user's programmatic calls to the
-- Orazaka API (distinct from user_credentials, which are outbound BYOK provider keys).
-- Only the SHA-256 hash is stored; the plaintext secret is shown once at creation.
CREATE TABLE api_keys (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    key_prefix VARCHAR(20) NOT NULL,
    key_hash VARCHAR(255) NOT NULL,
    -- Comma-separated area:action scopes the edge enforces (e.g. ai:embed); '' = the owner's full
    -- rights (every key created before scoping existed).
    scopes VARCHAR(255) NOT NULL DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP WITH TIME ZONE
);
CREATE UNIQUE INDEX idx_api_keys_hash ON api_keys(key_hash);
CREATE INDEX idx_api_keys_user ON api_keys(user_id);

-- An organization owns accounts (orazaka#372): the company (or an SME) is one party above its members and its
-- products' service accounts (svc_*). A member keeps one role; a service account is a user with the SERVICE role,
-- whose API keys the organization sees and can revoke. Removing a user removes their memberships.
CREATE TABLE organizations (
    id VARCHAR(255) PRIMARY KEY,
    slug VARCHAR(63) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE organization_members (
    organization_id VARCHAR(255) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(16) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'SERVICE')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (organization_id, user_id)
);
CREATE INDEX idx_organization_members_user ON organization_members(user_id);

CREATE TABLE password_resets (
    id         VARCHAR(255) PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_password_resets_token_hash ON password_resets(token_hash);
CREATE INDEX idx_password_resets_email ON password_resets(email);

-- Rate-limit tier configs read by identity's RateLimitProvider (the default tier
-- selection is data, not yaml — exactly one row carries is_default = TRUE).
CREATE TABLE rate_limits (
    tier_key VARCHAR(50) PRIMARY KEY,
    requests_per_minute INT NOT NULL,
    concurrent_jobs INT NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX idx_rate_limits_default
    ON rate_limits (is_default) WHERE is_default;

-- Identity transactional outbox (AGENTS.md §6): identity domain events
-- (evt.user.*, evt.password.*) are appended in the business transaction and
-- published by the identity relay with message_id as the AMQP messageId.
-- The administration's append-only audit trail (who did what to which account). Written by the identity
-- service's admin endpoints; read back at /api/v1/admin/users/audit. No secret, no message: ids and a few words.
CREATE TABLE admin_audit (
    id BIGSERIAL PRIMARY KEY,
    at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_id VARCHAR(255),
    actor_label VARCHAR(255) NOT NULL,
    action VARCHAR(60) NOT NULL,
    target_id VARCHAR(255),
    detail TEXT,
    request_id VARCHAR(64),
    outcome VARCHAR(12) NOT NULL DEFAULT 'ok'
);
CREATE INDEX idx_admin_audit_at ON admin_audit(at DESC);
CREATE INDEX idx_admin_audit_target ON admin_audit(target_id);

CREATE TABLE identity_outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    exchange VARCHAR(100) NOT NULL,
    routing_key VARCHAR(255) NOT NULL,
    message_id UUID NOT NULL UNIQUE,
    payload JSONB NOT NULL,
    -- AMQP headers published with the message: the event envelope (kz-type, kz-version,
    -- kz-producer, kz-correlation-id, kz-occurred-at) of a row written by an EventPublisher.
    headers JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_identity_outbox_pending ON identity_outbox(next_attempt_at) WHERE published_at IS NULL;

-- ============================================================================
-- IDENTITY SEED DATA
-- ============================================================================

INSERT INTO rate_limit_tiers (id, capacity, refill_tokens, refill_seconds) VALUES
('free', 60, 60, 60),
('premium', 200, 200, 60),
('admin', 1000, 1000, 60),
('service', 1000, 1000, 60)
ON CONFLICT (id) DO NOTHING;



INSERT INTO rate_limits (tier_key, requests_per_minute, concurrent_jobs, is_default) VALUES
('free', 60, 1, TRUE),
('premium', 200, 5, FALSE),
('admin', 1000, 20, FALSE),
('enterprise', 1000, 20, FALSE),
('service', 1000, 20, FALSE)
ON CONFLICT (tier_key) DO NOTHING;

