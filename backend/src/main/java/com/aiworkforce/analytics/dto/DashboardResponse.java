package com.aiworkforce.analytics.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private int totalTasksCompleted;
    private int totalOverdueTasks;
    private int currentWorkloadScore;
    private String burnoutRiskLevel; // LOW, MEDIUM, HIGH
    private List<WorkloadTrendInfo> recentTrends;

    @Data
    @Builder
    public static class WorkloadTrendInfo {
        private String period; // e.g., 'Mon', 'Tue'
        private int workloadScore;
    }
}
