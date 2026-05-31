-- V4: Add external_url and source_provider to task table for ticket linking and integrations
ALTER TABLE task ADD COLUMN external_url VARCHAR(1000);
ALTER TABLE task ADD COLUMN source_provider VARCHAR(255) DEFAULT 'INTERNAL';
