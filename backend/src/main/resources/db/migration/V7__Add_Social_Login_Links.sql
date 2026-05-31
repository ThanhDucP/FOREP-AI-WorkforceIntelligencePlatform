-- V7: Support Google/GitHub OAuth2 login and account linking

ALTER TABLE account
    ADD COLUMN IF NOT EXISTS google_id VARCHAR(255);

ALTER TABLE account
    ADD COLUMN IF NOT EXISTS github_id VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS ux_account_google_id
    ON account (google_id)
    WHERE google_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_account_github_id
    ON account (github_id)
    WHERE github_id IS NOT NULL;