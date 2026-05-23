package com.aiworkforce.task.entity;
import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.TaskPriority;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "task")
@Getter
@Setter
public class Task extends AuditableEntity {
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    private TaskPriority priority;

    private LocalDateTime dueDate;
    private int estimatedHours;

    /** External ticket reference e.g. APX-2117, T-220 */
    private String externalTicketRef;

    /** Sprint number retained for analytics and external Apex AI views */
    private Integer sprintNumber;

    /** Sprint this task belongs to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private com.aiworkforce.identity.entity.Sprint sprint;

    /** Story points (for velocity calculation) */
    private Integer storyPoints;

    /** Actual time taken to complete the task in days */
    private Double cycleTimeDays;

    /** Actual time taken to complete the task in hours */
    private Double cycleTimeHours;

    /** When the task was marked completed */
    private LocalDateTime completedAt;

    /** True if this task is on the sprint critical path */
    private Boolean isOnCriticalPath = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private Employee assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private Employee reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;
}
