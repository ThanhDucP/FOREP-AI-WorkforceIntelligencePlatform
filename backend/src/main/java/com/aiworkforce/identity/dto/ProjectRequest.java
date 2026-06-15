package com.aiworkforce.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ProjectRequest {
    @NotBlank
    private String name;
    private String description;
    private boolean active = true;

    @NotNull
    private UUID organizationId;

    @NotNull
    private UUID teamId;

    private String githubRepository;
    private String jiraDomain;
    private String jiraProjectKey;
}
