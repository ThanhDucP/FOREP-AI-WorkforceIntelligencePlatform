package com.aiworkforce.platform.ai.service;

import com.aiworkforce.platform.ai.client.OllamaClient;
import com.aiworkforce.platform.ai.prompt.PromptBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIInsightService {

    private final OllamaClient ollamaClient;
    private final PromptBuilder promptBuilder;

    public String generateBurnoutInsight(Map<String, Object> analyticsData) {
        String prompt = promptBuilder.buildBurnoutPrompt(analyticsData);
        return ollamaClient.generate(prompt);
    }

    public String generateTeamSummary(Map<String, Object> teamData) {
        String prompt = promptBuilder.buildWorkloadSummaryPrompt(teamData);
        return ollamaClient.generate(prompt);
    }
}
