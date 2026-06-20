package com.aiworkforce.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryResponse {
    private String scope;
    private UUID scopeId;
    private String scopeName;
    private long completedTasks;
    private long openTasks;
    private long overdueTasks;
    private double overdueRatio;
    private double workloadScore;
    private int memberCount;
}
