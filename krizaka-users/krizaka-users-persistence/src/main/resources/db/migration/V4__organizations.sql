-- orazaka#372: organizations that own accounts. Flyway V4, run on deploy by the service role.
-- Idempotent. New installs get the tables from 10-identity.sql.
CREATE TABLE IF NOT EXISTS organizations (
    id VARCHAR(255) PRIMARY KEY,
    slug VARCHAR(63) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS organization_members (
    organization_id VARCHAR(255) NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(16) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'SERVICE')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (organization_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_organization_members_user ON organization_members(user_id);

-- The Krizaka organization, and the service accounts the products already use (svc_akinaza, svc_yomizaka, svc_yasakia...).
-- Its owner is added by an administrator through the API; nothing here guesses a person.
INSERT INTO organizations (id, slug, name) VALUES ('org-krizaka', 'krizaka', 'Krizaka') ON CONFLICT (slug) DO NOTHING;

INSERT INTO organization_members (organization_id, user_id, role)
SELECT 'org-krizaka', u.id, 'SERVICE' FROM users u WHERE u.username LIKE 'svc\_%'
ON CONFLICT (organization_id, user_id) DO NOTHING;
