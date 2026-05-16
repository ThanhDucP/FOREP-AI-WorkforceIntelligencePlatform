package com.aiworkforce.ai.prompt;

import com.aiworkforce.analytics.dto.DashboardResponse;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String buildBurnoutPrompt(String employeeName, DashboardResponse analyticsData) {
        return String.format(
            "Act as an HR/Workload Analyst. Analyze employee %s. " +
            "Last 7 days stats: %d tasks completed, %d tasks overdue, workload score is %d. " +
            "The system detects a %s risk of burnout. " +
            "Provide a short 3-sentence summary of their performance and 1 recommendation for the manager.",
            employeeName,
            analyticsData.getTotalTasksCompleted(),
            analyticsData.getTotalOverdueTasks(),
            analyticsData.getCurrentWorkloadScore(),
            analyticsData.getBurnoutRiskLevel()
        );
    }
}
