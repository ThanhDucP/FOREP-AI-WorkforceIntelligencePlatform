package com.aiworkforce.integration.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ImportedProjectResponse {
    private UUID id;
    private UUID configId;
    private UUID projectId;
    private UUID teamId;
    private String jiraDomain;
    private String projectKey;
    private String providerProjectId;
    private String name;
    private String projectTypeKey;
    private String leadAccountId;
    private String leadDisplayName;
    private Boolean sprintDataAvailable;
    private Boolean storyPointsAvailable;
    private Boolean epicDataAvailable;
    private Boolean versionDataAvailable;
    private Boolean componentDataAvailable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}