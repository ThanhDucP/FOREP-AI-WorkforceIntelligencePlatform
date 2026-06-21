package com.aiworkforce.integration.dto;

import com.aiworkforce.core.enums.MappingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class IdentityMappingResponse {
    private UUID id;
    private UUID organizationId;
    private UUID employeeId;
    private String employeeName;
    private UUID jiraIdentityId;
    private UUID githubIdentityId;
    private MappingStatus status;
    private Double confidenceScore;
    private String evidenceSummary;
    private LocalDateTime confirmedAt;
    private UUID confirmedByEmployeeId;
}