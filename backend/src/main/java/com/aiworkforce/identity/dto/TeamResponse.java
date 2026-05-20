package com.aiworkforce.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID organizationId;
    private String organizationName;
    private UUID managerId;
    private String managerName;
}
