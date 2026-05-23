package com.aiworkforce.analytics.entity;
import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.BurnoutRisk;
import com.aiworkforce.identity.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "employee_workload_snapshot")
@Getter
@Setter
public class EmployeeWorkloadSnapshot extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate snapshotDate;

    private Double workloadScore;

    @Enumerated(EnumType.STRING)
    private BurnoutRisk burnoutRisk = BurnoutRisk.NONE;

    private Integer tasksOpen;
    private Integer tasksOverdue;

    /** % of activity outside working hours on this date */
    private Double outOfHoursPct;

    /** Average cycle time in days as of this snapshot */
    private Double cycleTimeAvg;
}
