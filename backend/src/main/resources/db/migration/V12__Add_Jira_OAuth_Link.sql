ALTER TABLE account
    ADD COLUMN IF NOT EXISTS jira_id VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS ux_account_jira_id
    ON account (jira_id)
    WHERE jira_id IS NOT NULL;
