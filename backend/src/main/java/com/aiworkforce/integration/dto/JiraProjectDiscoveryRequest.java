package com.aiworkforce.integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JiraProjectDiscoveryRequest {
    @NotBlank
    private String jiraDomain;

    @NotBlank
    private String connectionKey;
}