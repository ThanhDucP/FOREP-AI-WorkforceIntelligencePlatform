-- V20: foundation for organization director provisioning, account lifecycle status, and map place fields.

ALTER TABLE account ADD COLUMN IF NOT EXISTS status VARCHAR(32) DEFAULT 'ACTIVE';
UPDATE account SET status = 'ACTIVE' WHERE status IS NULL;
CREATE INDEX IF NOT EXISTS ix_account_status ON account(status);

ALTER TABLE employee ADD COLUMN IF NOT EXISTS organization_id UUID REFERENCES organization(id);
CREATE INDEX IF NOT EXISTS ix_employee_organization ON employee(organization_id);

ALTER TABLE organization ADD COLUMN IF NOT EXISTS formatted_address TEXT;
ALTER TABLE organization ADD COLUMN IF NOT EXISTS place_id VARCHAR(255);
ALTER TABLE organization ADD COLUMN IF NOT EXISTS director_employee_id UUID REFERENCES employee(id);
CREATE INDEX IF NOT EXISTS ix_organization_director ON organization(director_employee_id);
CREATE INDEX IF NOT EXISTS ix_organization_place ON organization(place_id);