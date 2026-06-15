package com.aiworkforce.integration.dto;

import com.aiworkforce.core.enums.IntegrationProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class TaskIntegrationConfigRequest {
    @NotNull(message = "Team ID is required")
    private UUID teamId;

    private UUID projectId;

    @NotNull(message = "Provider is required")
    private IntegrationProvider provider;

    @NotBlank(message = "Webhook secret is required")
    private String webhookSecret;

    private String accessToken;
    private String projectKey;
    private String jiraDomain;
    private Boolean isActive;
}
