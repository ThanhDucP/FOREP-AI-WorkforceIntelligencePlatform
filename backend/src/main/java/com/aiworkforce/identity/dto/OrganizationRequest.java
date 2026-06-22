package com.aiworkforce.identity.dto;

import com.aiworkforce.core.enums.ContractStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
    private ContractStatus contractStatus;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private Integer maxUsers;
    private String adminNote;

    private String directorFirstName;
    private String directorLastName;
    private String directorEmail;
    private String directorPassword;
}