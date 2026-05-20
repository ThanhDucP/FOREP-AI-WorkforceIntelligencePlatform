package com.aiworkforce.timetracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestResponse {
    private UUID id;
    private String reason;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private UUID employeeId;
    private String employeeName;
}
