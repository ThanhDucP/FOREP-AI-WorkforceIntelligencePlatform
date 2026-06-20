package com.aiworkforce.analytics.dto;

import com.aiworkforce.core.enums.BurnoutRisk;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BurnoutResponse {
    private String scope;
    private UUID scopeId;
    private String scopeName;
    private double burnoutScore;
    private BurnoutRisk burnoutRisk;
    private double workloadScore;
    private double overdueRatio;
    private double outOfHoursPct;
    private double avgCycleTimeDays;
    private long openTasks;
    private long overdueTasks;
    private String recommendation;
}
