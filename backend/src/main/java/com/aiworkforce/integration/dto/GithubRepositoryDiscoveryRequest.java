package com.aiworkforce.integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GithubRepositoryDiscoveryRequest {
    @NotBlank
    private String connectionKey;
}