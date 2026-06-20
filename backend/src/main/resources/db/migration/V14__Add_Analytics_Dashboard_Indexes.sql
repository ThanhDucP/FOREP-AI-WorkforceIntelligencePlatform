CREATE INDEX IF NOT EXISTS ix_task_assignee_status_due
    ON task (assignee_id, status, due_date);

CREATE INDEX IF NOT EXISTS ix_task_team_status_due
    ON task (team_id, status, due_date);

CREATE INDEX IF NOT EXISTS ix_task_project_status
    ON task (project_id, status);

CREATE INDEX IF NOT EXISTS ix_task_sprint_status
    ON task (sprint_id, status);

CREATE INDEX IF NOT EXISTS ix_employee_team
    ON employee (team_id);

CREATE INDEX IF NOT EXISTS ix_employee_workload_snapshot_employee_date
    ON employee_workload_snapshot (employee_id, snapshot_date DESC);

CREATE INDEX IF NOT EXISTS ix_employee_workload_snapshot_date_score
    ON employee_workload_snapshot (snapshot_date DESC, workload_score DESC);

CREATE INDEX IF NOT EXISTS ix_notification_recipient_read_created
    ON notification (recipient_id, is_read, created_at DESC);
