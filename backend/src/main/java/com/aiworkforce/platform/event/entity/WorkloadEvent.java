package com.aiworkforce.platform.event.entity;

import com.aiworkforce.platform.common.base.BaseEntity;
import com.aiworkforce.platform.common.enums.Enums.EventType;
import com.aiworkforce.platform.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workload_event")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadEvent extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "entity_type")
    private String entityType; // e.g., "TASK"

    @Column(name = "entity_id")
    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON string of specific event details

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
