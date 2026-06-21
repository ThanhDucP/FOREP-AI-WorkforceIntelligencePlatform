package com.aiworkforce.core.security;

import com.aiworkforce.core.enums.AuditActionType;
import com.aiworkforce.core.exception.ForbiddenException;
import com.aiworkforce.core.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service("readOnlyScopeGuard")
@RequiredArgsConstructor
public class ReadOnlyScopeGuard {

    private final AuditLogService auditLogService;

    public void block(String operation, String targetType, UUID targetId) {
        auditLogService.record(
                AuditActionType.BLOCKED_READ_ONLY_OPERATION,
                targetType,
                targetId,
                null,
                Map.of("operation", operation, "reason", "System scope is read-only for project source data")
        );
        throw new ForbiddenException("This operation is disabled. The system is read-only and only aggregates data from Jira/GitHub.");
    }
}