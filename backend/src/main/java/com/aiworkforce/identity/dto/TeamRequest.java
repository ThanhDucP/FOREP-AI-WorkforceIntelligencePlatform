package com.aiworkforce.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamRequest {
    @NotBlank(message = "Team name is required")
    private String name;
    
    private String description;
    private UUID organizationId;
    private UUID managerId;
}
