package com.aiworkforce.analytics.dto;

import com.aiworkforce.core.enums.BurnoutRisk;
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
public class WorkloadHistoryResponse {
    private UUID employeeId;
    private String employeeName;
    private LocalDate date;
    private Double workloadScore;
    private BurnoutRisk burnoutRisk;
    private Integer tasksOpen;
    private Integer tasksOverdue;
    private Double outOfHoursPct;
    private Double cycleTimeAvg;
}
