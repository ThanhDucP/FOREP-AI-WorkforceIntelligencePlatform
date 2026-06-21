package com.aiworkforce.ai.dto;

import com.aiworkforce.core.enums.InsightSeverity;
import com.aiworkforce.core.enums.InsightType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIInsightResponse {
    private UUID id;
    private String summary;
    private String fullAnalysis;
    private InsightSeverity severity;
    private InsightType insightType;
    private Double confidenceScore;
    private UUID employeeId;
    private String employeeName;
    private UUID teamId;
    private String teamName;
    private UUID projectId;
    private String projectName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}