package com.aiworkforce.identity.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EmployeeInvitationResponse {
    private UUID employeeId;
    private String email;
    private String accountStatus;
    private String activationToken;
    private String activationLink;
    private LocalDateTime invitationSentAt;
    private LocalDateTime activatedAt;
}