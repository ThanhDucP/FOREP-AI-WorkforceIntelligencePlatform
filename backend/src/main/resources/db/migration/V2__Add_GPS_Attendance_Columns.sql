-- V2: Add GPS attendance validation columns to organization
ALTER TABLE organization ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE organization ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE organization ADD COLUMN allowed_radius_meters INTEGER DEFAULT 200;
