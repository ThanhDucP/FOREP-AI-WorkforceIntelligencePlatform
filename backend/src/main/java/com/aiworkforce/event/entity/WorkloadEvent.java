package com.aiworkforce.event.entity;
import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.EventType;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.task.entity.Task;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = ""workload_event"")
@Getter
@Setter
public class WorkloadEvent extends AuditableEntity {
    
    @Enumerated(EnumType.STRING)
    private EventType eventType;
    
    private String eventDetails;
    
    private int impactScore; // Used for analytics
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = ""employee_id"")
    private Employee employee;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = ""task_id"")
    private Task task;
}
