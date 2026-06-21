-- V19: Jira sprint is optional. Issue-level fields are the required analytics base.

ALTER TABLE jira_issue_snapshot ADD COLUMN IF NOT EXISTS assignee_display_name VARCHAR(255);
ALTER TABLE jira_issue_snapshot ADD COLUMN IF NOT EXISTS reporter_account_id VARCHAR(255);
ALTER TABLE jira_issue_snapshot ADD COLUMN IF NOT EXISTS reporter_email VARCHAR(255);
ALTER TABLE jira_issue_snapshot ADD COLUMN IF NOT EXISTS reporter_display_name VARCHAR(255);
ALTER TABLE jira_issue_snapshot ADD COLUMN IF NOT EXISTS labels TEXT;
ALTER TABLE jira_issue_snapshot ADD COLUMN IF NOT EXISTS epic_key VARCHAR(255);
ALTER TABLE jira_issue_snapshot ADD COLUMN IF NOT EXISTS fix_versions TEXT;
ALTER TABLE jira_issue_snapshot ADD COLUMN IF NOT EXISTS components TEXT;
ALTER TABLE jira_issue_snapshot ADD COLUMN IF NOT EXISTS provider_created_at DATE;
ALTER TABLE jira_issue_snapshot ADD COLUMN IF NOT EXISTS provider_updated_at DATE;

ALTER TABLE jira_project_snapshot ADD COLUMN IF NOT EXISTS sprint_data_available BOOLEAN DEFAULT FALSE;
ALTER TABLE jira_project_snapshot ADD COLUMN IF NOT EXISTS story_points_available BOOLEAN DEFAULT FALSE;
ALTER TABLE jira_project_snapshot ADD COLUMN IF NOT EXISTS epic_data_available BOOLEAN DEFAULT FALSE;
ALTER TABLE jira_project_snapshot ADD COLUMN IF NOT EXISTS version_data_available BOOLEAN DEFAULT FALSE;
ALTER TABLE jira_project_snapshot ADD COLUMN IF NOT EXISTS component_data_available BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS ix_jira_issue_snapshot_status ON jira_issue_snapshot(status_name);
CREATE INDEX IF NOT EXISTS ix_jira_issue_snapshot_priority ON jira_issue_snapshot(priority_name);
CREATE INDEX IF NOT EXISTS ix_jira_issue_snapshot_issue_type ON jira_issue_snapshot(issue_type);
CREATE INDEX IF NOT EXISTS ix_jira_issue_snapshot_due_date ON jira_issue_snapshot(due_date);
CREATE INDEX IF NOT EXISTS ix_jira_issue_snapshot_provider_updated ON jira_issue_snapshot(provider_updated_at);
CREATE INDEX IF NOT EXISTS ix_jira_issue_snapshot_reporter ON jira_issue_snapshot(reporter_account_id);
CREATE INDEX IF NOT EXISTS ix_jira_issue_snapshot_epic ON jira_issue_snapshot(epic_key);