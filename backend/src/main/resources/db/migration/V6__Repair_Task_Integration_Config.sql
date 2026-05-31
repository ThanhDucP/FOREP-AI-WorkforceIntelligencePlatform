-- V6: Recreate task_integration_config if it was dropped or missed in an existing deployment

CREATE TABLE IF NOT EXISTS task_integration_config (
    id UUID PRIMARY KEY,
    team_id UUID NOT NULL REFERENCES team(id),
    provider VARCHAR(255) NOT NULL,
    webhook_secret VARCHAR(255) NOT NULL,
    access_token VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    project_key VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);