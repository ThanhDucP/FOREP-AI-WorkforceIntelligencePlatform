INSERT INTO role (id, name, description, created_at)
VALUES ('00000000-0000-0000-0000-000000000101', 'SYSTEM_ADMIN', 'Full platform administration across all organizations', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO role (id, name, description, created_at)
VALUES ('00000000-0000-0000-0000-000000000102', 'DIRECTOR', 'Organization-level administration and manager provisioning', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY,
    action_type VARCHAR(80) NOT NULL,
    actor_employee_id UUID REFERENCES employee(id),
    organization_id UUID REFERENCES organization(id),
    target_type VARCHAR(120),
    target_id UUID,
    provider VARCHAR(50),
    occurred_at TIMESTAMP,
    metadata TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS ix_audit_log_action_occurred ON audit_log(action_type, occurred_at DESC);
CREATE INDEX IF NOT EXISTS ix_audit_log_org_occurred ON audit_log(organization_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS ix_audit_log_actor_occurred ON audit_log(actor_employee_id, occurred_at DESC);

CREATE TABLE IF NOT EXISTS external_identity (
    id UUID PRIMARY KEY,
    provider VARCHAR(40) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    username VARCHAR(255),
    display_name VARCHAR(255),
    email VARCHAR(255),
    avatar_url VARCHAR(1024),
    organization_id UUID REFERENCES organization(id),
    team_id UUID REFERENCES team(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT uk_external_identity_provider_external_id UNIQUE(provider, external_id)
);

CREATE INDEX IF NOT EXISTS ix_external_identity_org ON external_identity(organization_id);
CREATE INDEX IF NOT EXISTS ix_external_identity_team ON external_identity(team_id);
CREATE INDEX IF NOT EXISTS ix_external_identity_email ON external_identity(email);

CREATE TABLE IF NOT EXISTS identity_mapping (
    id UUID PRIMARY KEY,
    organization_id UUID REFERENCES organization(id),
    employee_id UUID REFERENCES employee(id),
    jira_identity_id UUID REFERENCES external_identity(id),
    github_identity_id UUID REFERENCES external_identity(id),
    status VARCHAR(40) NOT NULL DEFAULT 'UNMATCHED',
    confidence_score DOUBLE PRECISION,
    evidence_summary TEXT,
    confirmed_at TIMESTAMP,
    confirmed_by_employee_id UUID REFERENCES employee(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS ix_identity_mapping_org_status ON identity_mapping(organization_id, status);
CREATE INDEX IF NOT EXISTS ix_identity_mapping_employee ON identity_mapping(employee_id);
CREATE INDEX IF NOT EXISTS ix_identity_mapping_jira ON identity_mapping(jira_identity_id);
CREATE INDEX IF NOT EXISTS ix_identity_mapping_github ON identity_mapping(github_identity_id);