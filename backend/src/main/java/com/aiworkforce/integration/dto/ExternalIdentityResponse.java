package com.aiworkforce.integration.dto;

import com.aiworkforce.core.enums.ExternalIdentityProvider;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ExternalIdentityResponse {
    private UUID id;
    private ExternalIdentityProvider provider;
    private String externalId;
    private String username;
    private String displayName;
    private String email;
    private String avatarUrl;
    private UUID organizationId;
    private UUID teamId;
}