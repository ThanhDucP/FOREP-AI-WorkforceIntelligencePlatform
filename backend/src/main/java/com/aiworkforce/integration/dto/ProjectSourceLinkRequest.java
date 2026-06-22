package com.aiworkforce.integration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ProjectSourceLinkRequest {
    @NotNull
    private UUID jiraProjectSnapshotId;

    @NotNull
    private UUID githubRepositorySnapshotId;

    private String note;
}