package com.aiworkforce.identity.dto;

import com.aiworkforce.core.enums.SprintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintResponse {
    private UUID id;
    private Integer sprintNumber;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer committedStoryPoints;
    private Integer completedStoryPoints;
    private Double velocityConfidence;
    private SprintStatus status;
    private UUID organizationId;
    private String organizationName;
}
