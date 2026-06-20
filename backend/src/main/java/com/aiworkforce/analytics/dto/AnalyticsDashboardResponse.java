package com.aiworkforce.analytics.dto;

import com.aiworkforce.core.enums.BurnoutRisk;
import com.aiworkforce.core.enums.InsightSeverity;
import com.aiworkforce.core.enums.IntegrationProvider;
import com.aiworkforce.core.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDashboardResponse {
    private long totalTasks;
    private long completedTasks;
    private long overdueTasks;
    private List<EmployeeWorkload> workloadByEmployee;
    private Map<BurnoutRisk, Long> burnoutRiskCount;
    private List<RecentActivity> recentActivity;
    private AiInsightSummary aiInsightSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeWorkload {
        private UUID employeeId;
        private String employeeName;
        private UUID teamId;
        private String teamName;
        private double workloadScore;
        private long openTasks;
        private long overdueTasks;
        private BurnoutRisk burnoutRisk;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private UUID taskId;
        private String title;
        private TaskStatus status;
        private IntegrationProvider sourceProvider;
        private UUID assigneeId;
        private String assigneeName;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiInsightSummary {
        private long totalInsights;
        private Map<InsightSeverity, Long> severityCount;
        private List<String> latestSummaries;
    }
}