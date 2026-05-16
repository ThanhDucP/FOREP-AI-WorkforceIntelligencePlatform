package com.aiworkforce.platform.common.enums;

public class Enums {

    public enum RoleType {
        ADMIN, MANAGER, EMPLOYEE
    }

    public enum TaskStatus {
        TODO, IN_PROGRESS, REVIEW, DONE, CANCELLED
    }

    public enum TaskPriority {
        LOW, MEDIUM, HIGH, URGENT
    }

    public enum EventType {
        TASK_CREATED, TASK_ASSIGNED, TASK_UPDATED, TASK_COMPLETED, TASK_OVERDUE,
        LOGIN_SUCCESS, LOGIN_FAILED, ACCOUNT_LOCKED
    }

    public enum AttendanceStatus {
        PRESENT, ABSENT, LATE, EARLY_LEAVE
    }

    public enum LeaveStatus {
        PENDING, APPROVED, REJECTED
    }

    public enum InsightSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
