CREATE TABLE IF NOT EXISTS github_repository_snapshot (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    config_id UUID NOT NULL REFERENCES task_integration_config(id) ON DELETE CASCADE,
    project_id UUID REFERENCES project(id),
    team_id UUID NOT NULL REFERENCES team(id),
    full_name VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    owner_login VARCHAR(255),
    html_url VARCHAR(1000),
    default_branch VARCHAR(255),
    private_repository BOOLEAN,
    stargazers_count INTEGER,
    forks_count INTEGER,
    open_issues_count INTEGER,
    pushed_at TIMESTAMP,
    provider_updated_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_github_repository_snapshot_config_full_name
    ON github_repository_snapshot (config_id, LOWER(full_name));

CREATE TABLE IF NOT EXISTS github_contributor (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    config_id UUID NOT NULL REFERENCES task_integration_config(id) ON DELETE CASCADE,
    project_id UUID REFERENCES project(id),
    team_id UUID NOT NULL REFERENCES team(id),
    repository_full_name VARCHAR(255) NOT NULL,
    login VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(1000),
    html_url VARCHAR(1000),
    contributions INTEGER
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_github_contributor_config_repo_login
    ON github_contributor (config_id, LOWER(repository_full_name), LOWER(login));

CREATE TABLE IF NOT EXISTS github_pull_request (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    config_id UUID NOT NULL REFERENCES task_integration_config(id) ON DELETE CASCADE,
    project_id UUID REFERENCES project(id),
    team_id UUID NOT NULL REFERENCES team(id),
    repository_full_name VARCHAR(255) NOT NULL,
    number INTEGER NOT NULL,
    title VARCHAR(1000),
    state VARCHAR(64),
    html_url VARCHAR(1000),
    author_login VARCHAR(255),
    draft BOOLEAN,
    merged BOOLEAN,
    provider_created_at TIMESTAMP,
    provider_updated_at TIMESTAMP,
    closed_at TIMESTAMP,
    merged_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_github_pull_request_config_repo_number
    ON github_pull_request (config_id, LOWER(repository_full_name), number);

CREATE INDEX IF NOT EXISTS ix_github_pull_request_team_state
    ON github_pull_request (team_id, state);

CREATE TABLE IF NOT EXISTS github_commit (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    config_id UUID NOT NULL REFERENCES task_integration_config(id) ON DELETE CASCADE,
    project_id UUID REFERENCES project(id),
    team_id UUID NOT NULL REFERENCES team(id),
    repository_full_name VARCHAR(255) NOT NULL,
    sha VARCHAR(255) NOT NULL,
    message TEXT,
    author_name VARCHAR(255),
    author_email VARCHAR(255),
    html_url VARCHAR(1000),
    additions INTEGER,
    deletions INTEGER,
    changed_files INTEGER,
    committed_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_github_commit_config_repo_sha
    ON github_commit (config_id, LOWER(repository_full_name), sha);

CREATE INDEX IF NOT EXISTS ix_github_commit_team_committed
    ON github_commit (team_id, committed_at DESC);
