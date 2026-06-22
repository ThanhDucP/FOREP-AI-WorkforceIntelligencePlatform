package com.aiworkforce.integration.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GithubRepositoryOptionResponse {
    private Long providerRepositoryId;
    private String fullName;
    private String name;
    private String ownerLogin;
    private String visibility;
    private String mainLanguage;
    private String htmlUrl;
    private Boolean privateRepository;
    private LocalDateTime pushedAt;
    private LocalDateTime providerUpdatedAt;
}