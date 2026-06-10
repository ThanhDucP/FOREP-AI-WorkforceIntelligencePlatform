-- V9: Store agent/manual task assessment and GitHub commit evaluation metrics.
ALTER TABLE task ADD COLUMN IF NOT EXISTS difficulty_score INTEGER;
ALTER TABLE task ADD COLUMN IF NOT EXISTS task_score DOUBLE PRECISION;
ALTER TABLE task ADD COLUMN IF NOT EXISTS progress_percent INTEGER;
ALTER TABLE task ADD COLUMN IF NOT EXISTS lead_evaluation TEXT;
ALTER TABLE task ADD COLUMN IF NOT EXISTS assessment_summary TEXT;
ALTER TABLE task ADD COLUMN IF NOT EXISTS assessment_source VARCHAR(255);
ALTER TABLE task ADD COLUMN IF NOT EXISTS assessed_at TIMESTAMP;
ALTER TABLE task ADD COLUMN IF NOT EXISTS github_commit_count INTEGER;
ALTER TABLE task ADD COLUMN IF NOT EXISTS github_commit_difficulty_score INTEGER;
ALTER TABLE task ADD COLUMN IF NOT EXISTS github_commit_size_score INTEGER;
ALTER TABLE task ADD COLUMN IF NOT EXISTS github_commit_score DOUBLE PRECISION;
