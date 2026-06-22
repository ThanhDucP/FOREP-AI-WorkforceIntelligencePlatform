package com.aiworkforce.integration.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ImportedIssueResponse {
    private UUID id;
    private UUID configId;
    private UUID projectId;
    private UUID teamId;
    private UUID assigneeId;
    private String jiraDomain;
    private String projectKey;
    private String issueKey;
    private String providerIssueId;
    private String summary;
    private String statusName;
    private String issueType;
    private String priorityName;
    private String externalUrl;
    private String assigneeAccountId;
    private String assigneeEmail;
    private String assigneeDisplayName;
    private String reporterAccountId;
    private String reporterEmail;
    private String reporterDisplayName;
    private Integer storyPoints;
    private Integer sprintId;
    private String sprintName;
    private String labels;
    private String epicKey;
    private String fixVersions;
    private String components;
    private LocalDate providerCreatedAt;
    private LocalDate providerUpdatedAt;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}