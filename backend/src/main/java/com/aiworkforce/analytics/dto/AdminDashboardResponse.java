package com.aiworkforce.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private long totalUsers;
    private long activeUsers;
    private long totalTeams;
    private long totalOrganizations;
    private long totalTasks;
    private long completedTasks;
    private Map<String, Long> burnoutRiskDistribution;
}
