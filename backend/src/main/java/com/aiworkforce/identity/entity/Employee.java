package com.aiworkforce.identity.entity;
import com.aiworkforce.identity.entity.Account;
import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.BurnoutRisk;
import com.aiworkforce.identity.entity.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employee")
@Getter
@Setter
public class Employee extends AuditableEntity {
    private String firstName;
    private String lastName;
    private String jobTitle;
    private String phoneNumber;

    /** Department name e.g. Backend, Frontend, DevOps, Design */
    private String department;

    /** 2-char avatar initials shown on heatmap e.g. ER, JS */
    private String avatarInitials;

    // ── Workload & Burnout metrics (computed by WorkloadSnapshotService) ──

    /** Current workload score 0–100 */
    private Double workloadScore;

    /** Current burnout risk level */
    @Enumerated(EnumType.STRING)
    private BurnoutRisk burnoutRisk = BurnoutRisk.NONE;

    /** Contribution score 0–100 */
    private Double contributionScore;

    /** Ratio of overdue tasks (0.0–1.0) */
    private Double overdueRatio;

    /** % of activity happening outside working hours */
    private Double outOfHoursPct;

    /** Average cycle time to complete a task (days) */
    private Double avgCycleTimeDays;

    /** Number of tasks shipped in the current month */
    private Integer tasksShippedThisMonth;

    /** Consecutive days with on-time task completions */
    private Integer streakDays;

    /** Focus score — composite of cycle time, overdue, out-of-hours */
    private Double focusScore;

    @OneToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;
}
