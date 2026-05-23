package com.aiworkforce.timetracking.entity;
import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.core.enums.AttendanceStatus;
import com.aiworkforce.identity.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "attendance")
@Getter
@Setter
public class Attendance extends AuditableEntity {
    private LocalDate checkInDate;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;

    /** Total hours worked (checkOut - checkIn) */
    private Double workHoursTotal;

    /** Location string from GPS reverse-geocode e.g. "San Francisco HQ" */
    private String checkInLocation;

    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;
}
