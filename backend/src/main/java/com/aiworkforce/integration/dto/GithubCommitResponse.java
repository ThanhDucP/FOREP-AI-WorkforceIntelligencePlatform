package com.aiworkforce.integration.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class GithubCommitResponse {
    private UUID id;
    private UUID configId;
    private UUID projectId;
    private UUID teamId;
    private String repositoryFullName;
    private String sha;
    private String message;
    private String authorName;
    private String authorEmail;
    private String htmlUrl;
    private Integer additions;
    private Integer deletions;
    private Integer changedFiles;
    private LocalDateTime committedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}