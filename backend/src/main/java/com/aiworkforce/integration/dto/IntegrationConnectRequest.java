package com.aiworkforce.integration.dto;

import com.aiworkforce.core.enums.IntegrationProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class IntegrationConnectRequest {
    @NotNull
    private UUID teamId;

    @NotNull
    private IntegrationProvider provider;

    @NotBlank
    private String projectKey;

    @NotBlank
    private String connectionKey; // PAT or installation token
}
