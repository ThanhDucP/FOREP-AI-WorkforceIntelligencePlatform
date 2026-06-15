package com.aiworkforce.integration.dto;

import com.aiworkforce.core.enums.IntegrationProvider;
import com.aiworkforce.core.enums.IntegrationSyncStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class IntegrationSyncLogResponse {
    private UUID id;
    private UUID configId;
    private IntegrationProvider provider;
    private IntegrationSyncStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String message;
}
