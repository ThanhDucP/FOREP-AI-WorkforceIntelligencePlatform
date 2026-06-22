package com.aiworkforce.integration.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JiraProjectOptionResponse {
    private String providerProjectId;
    private String projectKey;
    private String name;
    private String projectTypeKey;
    private String leadDisplayName;
    private String selfUrl;
}