package com.aiworkforce.timetracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {
    private UUID id;
    private LocalDate checkInDate;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String status;
    private UUID employeeId;
    private String employeeName;
}
