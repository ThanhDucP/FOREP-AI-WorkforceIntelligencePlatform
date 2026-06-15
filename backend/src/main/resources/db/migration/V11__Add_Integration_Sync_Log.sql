ALTER TABLE task_integration_config
    ADD COLUMN IF NOT EXISTS last_sync_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_sync_status VARCHAR(64),
    ADD COLUMN IF NOT EXISTS last_sync_error TEXT;

CREATE TABLE IF NOT EXISTS integration_sync_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    config_id UUID NOT NULL REFERENCES task_integration_config(id) ON DELETE CASCADE,
    provider VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    message TEXT
);

CREATE INDEX IF NOT EXISTS ix_integration_sync_log_config_started
    ON integration_sync_log (config_id, started_at DESC);
