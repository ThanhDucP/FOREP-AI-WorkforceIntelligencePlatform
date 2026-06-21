-- V21: organization address is plain business data, not GPS/check-in data.

ALTER TABLE organization ADD COLUMN IF NOT EXISTS address TEXT;

UPDATE organization
SET address = COALESCE(address, formatted_address)
WHERE address IS NULL AND formatted_address IS NOT NULL;

DROP INDEX IF EXISTS ix_organization_place;

ALTER TABLE organization DROP COLUMN IF EXISTS formatted_address;
ALTER TABLE organization DROP COLUMN IF EXISTS place_id;
ALTER TABLE organization DROP COLUMN IF EXISTS latitude;
ALTER TABLE organization DROP COLUMN IF EXISTS longitude;
ALTER TABLE organization DROP COLUMN IF EXISTS allowed_radius_meters;