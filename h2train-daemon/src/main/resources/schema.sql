CREATE TABLE IF NOT EXISTS user_accounts (
    id VARCHAR(64) PRIMARY KEY,
    created_at VARCHAR(64) NOT NULL
);

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
