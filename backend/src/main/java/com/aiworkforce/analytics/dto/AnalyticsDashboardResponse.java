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
    private ProjectHealth projectHealth;
    private TeamAnalytics teamAnalytics;
    private GithubAnalytics githubAnalytics;
    private SprintAnalytics sprintAnalytics;

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
    public static class ProjectHealth {
        private int score;
        private long totalIssues;
        private long overdueIssues;
        private long blockedIssues;
        private double completionRate;
        private Map<String, Long> priorityDistribution;
        private Map<String, Long> statusDistribution;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamAnalytics {
        private long assignedIssueCount;
        private long unassignedIssueCount;
        private double averageOpenIssuesPerEmployee;
        private List<EmployeeWorkload> workloadDistribution;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GithubAnalytics {
        private boolean available;
        private long repositoryCount;
        private long commitCount;
        private long pullRequestCount;
        private long openPullRequestCount;
        private long mergedPullRequestCount;
        private long reviewDelayRiskCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SprintAnalytics {
        private boolean available;
        private String message;
        private long sprintCount;
        private long issuesWithSprint;
        private long issuesWithStoryPoints;
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