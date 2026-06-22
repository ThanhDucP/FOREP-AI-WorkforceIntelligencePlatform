ALTER TABLE organization ADD COLUMN IF NOT EXISTS contract_status VARCHAR(32) DEFAULT 'ACTIVE';
ALTER TABLE organization ADD COLUMN IF NOT EXISTS contract_start_date DATE;
ALTER TABLE organization ADD COLUMN IF NOT EXISTS contract_end_date DATE;
ALTER TABLE organization ADD COLUMN IF NOT EXISTS max_users INTEGER;
ALTER TABLE organization ADD COLUMN IF NOT EXISTS admin_note TEXT;

UPDATE organization
SET contract_status = 'ACTIVE'
WHERE contract_status IS NULL;

CREATE INDEX IF NOT EXISTS ix_organization_contract_status ON organization(contract_status);
