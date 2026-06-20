CREATE TABLE IF NOT EXISTS jira_project_snapshot (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    config_id UUID NOT NULL REFERENCES task_integration_config(id) ON DELETE CASCADE,
    project_id UUID REFERENCES project(id),
    team_id UUID NOT NULL REFERENCES team(id),
    jira_domain VARCHAR(255) NOT NULL,
    project_key VARCHAR(255) NOT NULL,
    provider_project_id VARCHAR(255),
    name VARCHAR(255),
    project_type_key VARCHAR(255),
    lead_account_id VARCHAR(255),
    lead_display_name VARCHAR(255),
    self_url VARCHAR(1000)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_jira_project_snapshot_config_domain_key
    ON jira_project_snapshot (config_id, LOWER(jira_domain), LOWER(project_key));

CREATE TABLE IF NOT EXISTS jira_sprint_snapshot (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    config_id UUID NOT NULL REFERENCES task_integration_config(id) ON DELETE CASCADE,
    project_id UUID REFERENCES project(id),
    team_id UUID NOT NULL REFERENCES team(id),
    jira_domain VARCHAR(255) NOT NULL,
    project_key VARCHAR(255) NOT NULL,
    board_id INTEGER,
    sprint_id INTEGER NOT NULL,
    name VARCHAR(255),
    state VARCHAR(64),
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    complete_date TIMESTAMP,
    self_url VARCHAR(1000)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_jira_sprint_snapshot_config_sprint
    ON jira_sprint_snapshot (config_id, sprint_id);

CREATE INDEX IF NOT EXISTS ix_jira_sprint_snapshot_team_state
    ON jira_sprint_snapshot (team_id, state);

CREATE TABLE IF NOT EXISTS jira_issue_snapshot (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    config_id UUID NOT NULL REFERENCES task_integration_config(id) ON DELETE CASCADE,
    project_id UUID REFERENCES project(id),
    team_id UUID NOT NULL REFERENCES team(id),
    assignee_id UUID REFERENCES employee(id),
    jira_domain VARCHAR(255) NOT NULL,
    project_key VARCHAR(255) NOT NULL,
    issue_key VARCHAR(255) NOT NULL,
    provider_issue_id VARCHAR(255),
    summary VARCHAR(1000),
    status_name VARCHAR(255),
    issue_type VARCHAR(255),
    priority_name VARCHAR(255),
    external_url VARCHAR(1000),
    assignee_account_id VARCHAR(255),
    assignee_email VARCHAR(255),
    story_points INTEGER,
    original_estimate_seconds INTEGER,
    remaining_estimate_seconds INTEGER,
    sprint_id INTEGER,
    sprint_name VARCHAR(255),
    due_date DATE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_jira_issue_snapshot_config_issue
    ON jira_issue_snapshot (config_id, LOWER(issue_key));

CREATE INDEX IF NOT EXISTS ix_jira_issue_snapshot_team_status
    ON jira_issue_snapshot (team_id, status_name);

CREATE INDEX IF NOT EXISTS ix_jira_issue_snapshot_sprint
    ON jira_issue_snapshot (sprint_id);
