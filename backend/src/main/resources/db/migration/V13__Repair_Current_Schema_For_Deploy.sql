-- Repair schema drift introduced by recent domain changes.
-- Keep this migration additive/idempotent so existing deployments do not need
-- old Flyway checksums to change.

ALTER TABLE project
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

ALTER TABLE team_membership
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

ALTER TABLE task
    ADD COLUMN IF NOT EXISTS project_id UUID REFERENCES project(id);

ALTER TABLE task_integration_config
    ADD COLUMN IF NOT EXISTS project_id UUID REFERENCES project(id),
    ADD COLUMN IF NOT EXISTS last_sync_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_sync_status VARCHAR(64),
    ADD COLUMN IF NOT EXISTS last_sync_error TEXT;

ALTER TABLE account
    ADD COLUMN IF NOT EXISTS jira_id VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS ux_account_jira_id
    ON account (jira_id)
    WHERE jira_id IS NOT NULL;

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
