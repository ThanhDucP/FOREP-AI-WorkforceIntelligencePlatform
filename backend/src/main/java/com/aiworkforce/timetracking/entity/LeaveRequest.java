package com.aiworkforce.timetracking.entity;
import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.LeaveStatus;
import com.aiworkforce.core.enums.LeaveType;
import com.aiworkforce.identity.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_request")
@Getter
@Setter
public class LeaveRequest extends AuditableEntity {
    private String reason;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private LeaveStatus status;

    /** Type of leave taken */
    @Enumerated(EnumType.STRING)
    private LeaveType leaveType;

    /** ID of the manager/admin who approved this request */
    private UUID approvedById;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;
}
