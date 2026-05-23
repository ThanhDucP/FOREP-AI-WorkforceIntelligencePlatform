package com.aiworkforce.event.entity;
import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.EventType;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.task.entity.Task;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workload_event")
@Getter
@Setter
public class WorkloadEvent extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private String eventDetails;

    /** Workload impact score for analytics */
    private int impactScore;

    /** ID of the actor who triggered the event (employee or system) */
    private UUID actorId;

    /** True if AI flagged this as an anomaly */
    private Boolean isAnomaly = false;

    /** Anomaly description from AI analysis */
    private String anomalyDescription;

    /** Precise timestamp of when the event occurred */
    private LocalDateTime occurredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;
}
