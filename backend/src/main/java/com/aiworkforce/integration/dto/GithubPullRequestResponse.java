package com.aiworkforce.integration.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class GithubPullRequestResponse {
    private UUID id;
    private UUID configId;
    private UUID projectId;
    private UUID teamId;
    private String repositoryFullName;
    private Integer number;
    private String title;
    private String state;
    private String htmlUrl;
    private String authorLogin;
    private Boolean draft;
    private Boolean merged;
    private LocalDateTime providerCreatedAt;
    private LocalDateTime providerUpdatedAt;
    private LocalDateTime closedAt;
    private LocalDateTime mergedAt;
    private Long reviewDelayHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}