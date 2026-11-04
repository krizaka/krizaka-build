-- Flyway V7 (krizaka-internal#2359, slice 1a): one key per user and provider, held by the database, so two
-- concurrent creations cannot both land (the service inserts and maps the violation to 409). Duplicates left by
-- the old check-then-write upsert keep their newest row. The key itself is sealed by the service (v2 envelope,
-- AES-256-GCM, bound to user and provider); rows still in the old AES-CBC form are re-sealed on first read.
DELETE FROM user_credentials older
    USING user_credentials newer
    WHERE older.user_id = newer.user_id
      AND older.provider_name = newer.provider_name
      AND older.id < newer.id;
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_credentials_user_provider ON user_credentials (user_id, provider_name);
ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
