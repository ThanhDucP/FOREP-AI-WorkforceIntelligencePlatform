CREATE TABLE IF NOT EXISTS project_source_link (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id),
    team_id UUID REFERENCES team(id),
    project_id UUID REFERENCES project(id),
    jira_project_snapshot_id UUID NOT NULL REFERENCES jira_project_snapshot(id),
    github_repository_snapshot_id UUID NOT NULL REFERENCES github_repository_snapshot(id),
    note TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT uk_project_source_link_jira_github UNIQUE (jira_project_snapshot_id, github_repository_snapshot_id)
);

CREATE INDEX IF NOT EXISTS ix_project_source_link_org ON project_source_link(organization_id);
CREATE INDEX IF NOT EXISTS ix_project_source_link_team ON project_source_link(team_id);
CREATE INDEX IF NOT EXISTS ix_project_source_link_project ON project_source_link(project_id);
CREATE INDEX IF NOT EXISTS ix_project_source_link_jira ON project_source_link(jira_project_snapshot_id);
CREATE INDEX IF NOT EXISTS ix_project_source_link_github ON project_source_link(github_repository_snapshot_id);