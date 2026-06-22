ALTER TABLE account ADD COLUMN IF NOT EXISTS activation_token VARCHAR(255);
ALTER TABLE account ADD COLUMN IF NOT EXISTS invitation_sent_at TIMESTAMP;
ALTER TABLE account ADD COLUMN IF NOT EXISTS activated_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS ix_account_activation_token ON account(activation_token);