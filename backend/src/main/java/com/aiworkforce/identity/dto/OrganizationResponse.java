package com.aiworkforce.identity.dto;

import com.aiworkforce.core.enums.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

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
    private ContractStatus contractStatus;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private String adminNote;
    private Integer maxUsers;
    private long userCount;
    private List<EmployeeResponse> users;
    private UUID directorId;
    private String directorName;
    private String directorEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}