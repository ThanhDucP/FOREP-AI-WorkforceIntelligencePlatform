package com.aiworkforce.identity.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ProjectResponse {
    private UUID id;
    private String name;
    private String description;
    private boolean active;
    private UUID organizationId;
    private String organizationName;
    private UUID teamId;
    private String teamName;
    private String githubRepository;
    private String jiraDomain;
    private String jiraProjectKey;
}
