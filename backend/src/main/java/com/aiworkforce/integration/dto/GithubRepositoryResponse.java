package com.aiworkforce.integration.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class GithubRepositoryResponse {
    private UUID id;
    private UUID configId;
    private UUID projectId;
    private UUID teamId;
    private String fullName;
    private String name;
    private String ownerLogin;
    private String htmlUrl;
    private String defaultBranch;
    private Boolean privateRepository;
    private Integer stargazersCount;
    private Integer forksCount;
    private Integer openIssuesCount;
    private LocalDateTime pushedAt;
    private LocalDateTime providerUpdatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}