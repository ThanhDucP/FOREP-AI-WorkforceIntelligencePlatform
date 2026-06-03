-- V8: Add provider webhook id column for external webhook synchronization

ALTER TABLE IF EXISTS task_integration_config
ADD COLUMN IF NOT EXISTS provider_webhook_id VARCHAR(255);
