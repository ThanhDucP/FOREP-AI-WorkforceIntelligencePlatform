package com.aiworkforce.integration.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class GithubContributorResponse {
    private UUID id;
    private UUID configId;
    private UUID projectId;
    private UUID teamId;
    private String repositoryFullName;
    private String login;
    private String avatarUrl;
    private String htmlUrl;
    private Integer contributions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}