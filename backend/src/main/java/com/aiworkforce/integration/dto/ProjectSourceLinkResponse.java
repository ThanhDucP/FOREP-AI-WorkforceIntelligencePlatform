package com.aiworkforce.integration.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ProjectSourceLinkResponse {
    private UUID id;
    private UUID organizationId;
    private UUID teamId;
    private UUID projectId;
    private UUID jiraProjectSnapshotId;
    private String jiraProjectName;
    private String jiraProjectKey;
    private UUID githubRepositorySnapshotId;
    private String githubRepositoryFullName;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}