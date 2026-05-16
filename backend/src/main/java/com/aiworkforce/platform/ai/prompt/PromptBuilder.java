package com.aiworkforce.platform.ai.prompt;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PromptBuilder {

    public String buildBurnoutPrompt(Map<String, Object> analyticsData) {
        return "Analyze the following employee workload data and predict burnout risk:\n" + 
               analyticsData.toString() + 
               "\nProvide a summary and recommendations for the manager.";
    }

    public String buildWorkloadSummaryPrompt(Map<String, Object> teamData) {
        return "Summarize the team workload and efficiency based on this data:\n" + 
               teamData.toString() + 
               "\nIdentify bottlenecks and suggests improvements.";
    }
}
