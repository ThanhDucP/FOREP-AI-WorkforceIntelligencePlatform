package com.aiworkforce.integration.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IntegrationRuntimeStatusResponse {
    private boolean scheduledSyncEnabled;
    private long successIntervalMinutes;
    private long failureRetryDelayMinutes;
    private long activeConfigCount;
    private long failedConfigCount;
    private LocalDateTime latestSyncAt;
}
