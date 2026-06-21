package com.aiworkforce.core.controller;

import com.aiworkforce.core.dto.AuditLogResponse;
import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.core.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getRecentLogs() {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getRecentLogs()));
    }

    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'DIRECTOR')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getRecentLogsByOrganization(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getRecentLogsByOrganization(organizationId)));
    }
}