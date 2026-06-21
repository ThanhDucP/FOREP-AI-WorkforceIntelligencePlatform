package com.aiworkforce.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRequest {
    @NotBlank(message = "Organization name is required")
    private String name;
    private String domain;
    private String logoUrl;
    private String address;

    private String directorFirstName;
    private String directorLastName;
    private String directorEmail;
    private String directorPassword;
}