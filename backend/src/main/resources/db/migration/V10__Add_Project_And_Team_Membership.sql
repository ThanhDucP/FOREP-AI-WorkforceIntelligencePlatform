CREATE TABLE IF NOT EXISTS project (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    github_repository VARCHAR(255),
    jira_domain VARCHAR(255),
    jira_project_key VARCHAR(255),
    organization_id UUID NOT NULL REFERENCES organization(id),
    team_id UUID NOT NULL REFERENCES team(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_project_github_repository
    ON project (LOWER(github_repository))
    WHERE github_repository IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_project_jira_project
    ON project (LOWER(jira_domain), LOWER(jira_project_key))
    WHERE jira_domain IS NOT NULL AND jira_project_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS team_membership (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    employee_id UUID NOT NULL REFERENCES employee(id),
    team_id UUID NOT NULL REFERENCES team(id),
    approved_by_id UUID REFERENCES employee(id),
    status VARCHAR(64) NOT NULL DEFAULT 'PENDING_LEAD_APPROVAL',
    started_at TIMESTAMP,
    ended_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_team_membership_one_active_team
    ON team_membership (employee_id)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS ux_team_membership_one_pending_per_team
    ON team_membership (employee_id, team_id)
    WHERE status IN ('ACTIVE', 'PENDING_LEAD_APPROVAL');

ALTER TABLE organization ADD COLUMN IF NOT EXISTS github_organization VARCHAR(255);
ALTER TABLE task ADD COLUMN IF NOT EXISTS project_id UUID REFERENCES project(id);
ALTER TABLE task_integration_config ADD COLUMN IF NOT EXISTS project_id UUID REFERENCES project(id);
