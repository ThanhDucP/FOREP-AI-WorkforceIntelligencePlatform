package com.aiworkforce.ai.dto;

import com.aiworkforce.core.enums.SuggestionType;
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
public class AISuggestionResponse {
    private UUID id;
    private Integer sprintNumber;
    private SuggestionType suggestionType;
    private String description;
    private Double confidenceScore;
    private UUID sourceEmployeeId;
    private String sourceEmployeeName;
    private UUID targetEmployeeId;
    private String targetEmployeeName;
    private UUID sourceTaskId;
    private String sourceTaskTitle;
    private Boolean isAdopted;
    private LocalDateTime createdAt;
}
