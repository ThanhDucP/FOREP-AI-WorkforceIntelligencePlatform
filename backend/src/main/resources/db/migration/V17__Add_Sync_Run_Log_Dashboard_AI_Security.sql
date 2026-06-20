CREATE TABLE IF NOT EXISTS sync_run_log (
    id UUID PRIMARY KEY,
    config_id UUID NOT NULL REFERENCES task_integration_config(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    error_message TEXT,
    total_fetched INTEGER DEFAULT 0,
    total_created INTEGER DEFAULT 0,
    total_updated INTEGER DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

INSERT INTO sync_run_log (
    id, config_id, provider, status, started_at, finished_at, error_message,
    total_fetched, total_created, total_updated, created_at, updated_at, created_by, updated_by
)
SELECT
    id,
    config_id,
    provider,
    CASE WHEN status = 'STARTED' THEN 'RUNNING' ELSE status END,
    started_at,
    finished_at,
    message,
    0,
    0,
    0,
    created_at,
    updated_at,
    created_by,
    updated_by
FROM integration_sync_log
WHERE EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_name = 'integration_sync_log'
)
ON CONFLICT (id) DO NOTHING;

CREATE INDEX IF NOT EXISTS ix_sync_run_log_config_started
    ON sync_run_log (config_id, started_at DESC);
CREATE INDEX IF NOT EXISTS ix_sync_run_log_provider_status_started
    ON sync_run_log (provider, status, started_at DESC);

ALTER TABLE task ADD COLUMN IF NOT EXISTS external_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE ai_insight ADD COLUMN IF NOT EXISTS project_id UUID REFERENCES project(id);
CREATE INDEX IF NOT EXISTS ix_ai_insight_project_created ON ai_insight(project_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_task_external_deleted ON task(source_provider, external_deleted);