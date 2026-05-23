-- V3: Add Sprint, Comments, Notifications and Workload/Burnout metrics columns and tables

-- 1. Create Sprints Table
CREATE TABLE sprint (
    id UUID PRIMARY KEY,
    sprint_number INTEGER,
    name VARCHAR(255),
    start_date DATE,
    end_date DATE,
    committed_story_points INTEGER DEFAULT 0,
    completed_story_points INTEGER DEFAULT 0,
    velocity_confidence DOUBLE PRECISION DEFAULT 0.85,
    status VARCHAR(255) DEFAULT 'PLANNING',
    organization_id UUID REFERENCES organization(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- 2. Add columns to account and organization
ALTER TABLE account ADD COLUMN avatar_url VARCHAR(255);
ALTER TABLE account ADD COLUMN timezone VARCHAR(255);
ALTER TABLE account ADD COLUMN focus_score DOUBLE PRECISION DEFAULT 100.0;

ALTER TABLE organization ADD COLUMN current_sprint_number INTEGER;

-- 3. Add columns to employee
ALTER TABLE employee ADD COLUMN department VARCHAR(255);
ALTER TABLE employee ADD COLUMN avatar_initials VARCHAR(2);
ALTER TABLE employee ADD COLUMN workload_score DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE employee ADD COLUMN burnout_risk VARCHAR(255) DEFAULT 'NONE';
ALTER TABLE employee ADD COLUMN contribution_score DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE employee ADD COLUMN overdue_ratio DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE employee ADD COLUMN out_of_hours_pct DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE employee ADD COLUMN avg_cycle_time_days DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE employee ADD COLUMN tasks_shipped_this_month INTEGER DEFAULT 0;
ALTER TABLE employee ADD COLUMN streak_days INTEGER DEFAULT 0;
ALTER TABLE employee ADD COLUMN focus_score DOUBLE PRECISION DEFAULT 100.0;

-- 4. Add columns to team
ALTER TABLE team ADD COLUMN capacity_used_pct DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE team ADD COLUMN utilization_score DOUBLE PRECISION DEFAULT 0.0;

-- 5. Add columns to task and foreign keys
ALTER TABLE task ADD COLUMN sprint_id UUID REFERENCES sprint(id);
ALTER TABLE task ADD COLUMN sprint_number INTEGER;
ALTER TABLE task ADD COLUMN story_points INTEGER DEFAULT 0;
ALTER TABLE task ADD COLUMN cycle_time_days DOUBLE PRECISION;
ALTER TABLE task ADD COLUMN cycle_time_hours DOUBLE PRECISION;
ALTER TABLE task ADD COLUMN completed_at TIMESTAMP;
ALTER TABLE task ADD COLUMN is_on_critical_path BOOLEAN DEFAULT FALSE;
ALTER TABLE task ADD COLUMN external_ticket_ref VARCHAR(255);
ALTER TABLE task ADD COLUMN team_id UUID REFERENCES team(id);

-- 6. Add columns to workload_event
ALTER TABLE workload_event ADD COLUMN actor_id UUID;
ALTER TABLE workload_event ADD COLUMN is_anomaly BOOLEAN DEFAULT FALSE;
ALTER TABLE workload_event ADD COLUMN anomaly_description VARCHAR(255);
ALTER TABLE workload_event ADD COLUMN occurred_at TIMESTAMP;

-- 7. Add columns to ai_insight
ALTER TABLE ai_insight ADD COLUMN insight_type VARCHAR(255);
ALTER TABLE ai_insight ADD COLUMN confidence_score DOUBLE PRECISION;
ALTER TABLE ai_insight ADD COLUMN affected_employee_ids TEXT;
ALTER TABLE ai_insight ADD COLUMN adopted_at TIMESTAMP;
ALTER TABLE ai_insight ADD COLUMN team_id UUID REFERENCES team(id);

-- 8. Add columns to attendance
ALTER TABLE attendance ADD COLUMN work_hours_total DOUBLE PRECISION;
ALTER TABLE attendance ADD COLUMN check_in_location VARCHAR(255);

-- 9. Add columns to leave_request
ALTER TABLE leave_request ADD COLUMN leave_type VARCHAR(255);
ALTER TABLE leave_request ADD COLUMN approved_by_id UUID;

-- 10. Create task_comment Table
CREATE TABLE task_comment (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES employee(id),
    content TEXT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- 11. Create notification Table
CREATE TABLE notification (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    type VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    related_task_id UUID,
    related_employee_id UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- 12. Create employee_workload_snapshot Table
CREATE TABLE employee_workload_snapshot (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    snapshot_date DATE NOT NULL,
    workload_score DOUBLE PRECISION DEFAULT 0.0,
    burnout_risk VARCHAR(255) DEFAULT 'NONE',
    tasks_open INTEGER DEFAULT 0,
    tasks_overdue INTEGER DEFAULT 0,
    out_of_hours_pct DOUBLE PRECISION DEFAULT 0.0,
    cycle_time_avg DOUBLE PRECISION DEFAULT 0.0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- 13. Create ai_suggestion Table
CREATE TABLE ai_suggestion (
    id UUID PRIMARY KEY,
    sprint_number INTEGER,
    suggestion_type VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    confidence_score DOUBLE PRECISION,
    source_employee_id UUID,
    target_employee_id UUID,
    source_task_id UUID REFERENCES task(id) ON DELETE CASCADE,
    is_adopted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- 14. Seed default sprint 24 for clean databases
INSERT INTO sprint (
    id,
    sprint_number,
    name,
    start_date,
    end_date,
    committed_story_points,
    completed_story_points,
    velocity_confidence,
    status,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    24,
    'Sprint 24',
    CURRENT_DATE,
    CURRENT_DATE + INTERVAL '14 days',
    0,
    0,
    0.85,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
