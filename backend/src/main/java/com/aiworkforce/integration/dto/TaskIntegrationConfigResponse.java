package com.aiworkforce.integration.dto;

import com.aiworkforce.core.enums.IntegrationProvider;
import com.aiworkforce.core.enums.IntegrationSyncStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TaskIntegrationConfigResponse {
    private UUID id;
    private UUID teamId;
    private UUID projectId;
    private String projectName;
    private IntegrationProvider provider;
    private Boolean isActive;
    private String projectKey;
    private LocalDateTime lastSyncAt;
    private IntegrationSyncStatus lastSyncStatus;
    private String lastSyncError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // We intentionally do not expose webhookSecret and accessToken in responses
}
