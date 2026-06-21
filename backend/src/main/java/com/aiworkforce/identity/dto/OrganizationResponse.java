package com.aiworkforce.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponse {
    private UUID id;
    private String name;
    private String domain;
    private String logoUrl;
    private String address;
    private UUID directorId;
    private String directorName;
    private String directorEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}