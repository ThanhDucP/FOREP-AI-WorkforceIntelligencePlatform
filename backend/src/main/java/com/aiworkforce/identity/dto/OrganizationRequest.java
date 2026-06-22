package com.aiworkforce.identity.dto;

import com.aiworkforce.core.enums.ContractStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
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
    @Null(message = "logoUrl is no longer accepted. Upload organization logo through the file upload API")
    private Object logoUrl;
    private String address;

    @Null(message = "latitude is no longer supported. Use address text instead")
    private Object latitude;

    @Null(message = "longitude is no longer supported. Use address text instead")
    private Object longitude;
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