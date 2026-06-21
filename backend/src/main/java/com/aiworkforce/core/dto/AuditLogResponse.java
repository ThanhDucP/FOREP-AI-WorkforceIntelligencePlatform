package com.aiworkforce.core.dto;

import com.aiworkforce.core.enums.AuditActionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AuditLogResponse {
    private UUID id;
    private AuditActionType actionType;
    private UUID actorEmployeeId;
    private String actorName;
    private UUID organizationId;
    private String targetType;
    private UUID targetId;
    private String provider;
    private LocalDateTime occurredAt;
    private String metadata;
}