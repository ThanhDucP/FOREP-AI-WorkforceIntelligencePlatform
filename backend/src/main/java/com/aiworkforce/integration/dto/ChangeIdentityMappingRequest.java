package com.aiworkforce.integration.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ChangeIdentityMappingRequest {
    private UUID employeeId;
    private UUID jiraIdentityId;
    private UUID githubIdentityId;
    private String evidenceSummary;
}