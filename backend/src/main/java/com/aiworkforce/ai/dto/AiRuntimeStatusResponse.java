package com.aiworkforce.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiRuntimeStatusResponse {
    private String provider;
    private String model;
    private boolean apiKeyConfigured;
    private boolean ragEnabled;
    private int ragMaxContextCharacters;
    private int ragMaxTasks;
    private int ragMaxPreviousInsights;
}
