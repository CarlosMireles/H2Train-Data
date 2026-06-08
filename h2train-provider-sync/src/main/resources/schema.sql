CREATE TABLE IF NOT EXISTS user_accounts (
    id VARCHAR(64) PRIMARY KEY,
    email VARCHAR(320),
    username VARCHAR(128),
    password_hash VARCHAR(1024),
    provider_ids VARCHAR(1024),
    created_at VARCHAR(64) NOT NULL
);

ALTER TABLE user_accounts ADD COLUMN IF NOT EXISTS email VARCHAR(320);
ALTER TABLE user_accounts ADD COLUMN IF NOT EXISTS username VARCHAR(128);
ALTER TABLE user_accounts ADD COLUMN IF NOT EXISTS password_hash VARCHAR(1024);
ALTER TABLE user_accounts ADD COLUMN IF NOT EXISTS provider_ids VARCHAR(1024);

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_accounts_email
    ON user_accounts (email);

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_accounts_username
    ON user_accounts (username);

CREATE TABLE IF NOT EXISTS provider_connections (
    provider_id VARCHAR(32) NOT NULL,
    athlete_id VARCHAR(128) NOT NULL,
    athlete_username VARCHAR(255),
    access_token VARCHAR(4096),
    refresh_token VARCHAR(4096),
    expires_at VARCHAR(64),
    sync_enabled BOOLEAN NOT NULL,
    sync_interval VARCHAR(32) NOT NULL,
    last_sync_cursor VARCHAR(1024),
    last_synced_at VARCHAR(64),
    user_id VARCHAR(64),
    updated_at VARCHAR(64) NOT NULL,
    PRIMARY KEY (provider_id, athlete_id)
);

CREATE INDEX IF NOT EXISTS idx_provider_connections_user_id
    ON provider_connections (user_id);

CREATE TABLE IF NOT EXISTS sync_states (
    provider_id VARCHAR(32) NOT NULL,
    athlete_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    last_cursor VARCHAR(1024),
    last_synced_at VARCHAR(64),
    updated_at VARCHAR(64) NOT NULL,
    PRIMARY KEY (provider_id, athlete_id, event_type)
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    token_hash VARCHAR(128) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    email VARCHAR(320) NOT NULL,
    expires_at VARCHAR(64) NOT NULL,
    created_at VARCHAR(64) NOT NULL,
    used_at VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_id
    ON password_reset_tokens (user_id);

CREATE TABLE IF NOT EXISTS remember_me_tokens (
    token_hash VARCHAR(128) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    expires_at VARCHAR(64) NOT NULL,
    created_at VARCHAR(64) NOT NULL,
    last_used_at VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_remember_me_tokens_user_id
    ON remember_me_tokens (user_id);
