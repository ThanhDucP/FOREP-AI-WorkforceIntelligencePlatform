package com.aiworkforce.core.service;

import com.aiworkforce.core.dto.AuditLogResponse;
import com.aiworkforce.core.entity.AuditLog;
import com.aiworkforce.core.enums.AuditActionType;
import com.aiworkforce.core.exception.ForbiddenException;
import com.aiworkforce.core.repository.AuditLogRepository;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.core.security.AccessPolicyService;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.service.EmployeeService;
import com.aiworkforce.identity.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final EmployeeService employeeService;
    private final AccessPolicyService accessPolicyService;
    private final OrganizationRepository organizationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditActionType actionType, String targetType, UUID targetId, String provider, Map<String, ?> metadata) {
        AuditLog log = new AuditLog();
        log.setActionType(actionType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setProvider(provider);
        log.setOccurredAt(LocalDateTime.now());
        log.setMetadata(toSafeMetadata(metadata));

        try {
            Employee actor = employeeService.getCurrentEmployee();
            log.setActor(actor);
            log.setOrganization(resolveOrganization(actor));
        } catch (RuntimeException ignored) {
            // Background sync or unauthenticated system flow: keep actor null.
        }

        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getRecentLogs() {
        Employee actor = currentEmployeeOrNull();
        if (actor == null || !accessPolicyService.isSystemAdmin(actor)) {
            throw new ForbiddenException("Only system admins can view all audit logs");
        }
        return auditLogRepository.findTop100ByOrderByOccurredAtDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getRecentLogsByOrganization(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        accessPolicyService.ensureOrganizationAccess(organization);
        return auditLogRepository.findTop100ByOrganizationIdOrderByOccurredAtDesc(organizationId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Employee currentEmployeeOrNull() {
        try {
            return employeeService.getCurrentEmployee();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Organization resolveOrganization(Employee employee) {
        return employee != null && employee.getTeam() != null ? employee.getTeam().getOrganization() : null;
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        Employee actor = log.getActor();
        return AuditLogResponse.builder()
                .id(log.getId())
                .actionType(log.getActionType())
                .actorEmployeeId(actor != null ? actor.getId() : null)
                .actorName(actor != null ? ((actor.getFirstName() + " " + actor.getLastName()).trim()) : null)
                .organizationId(log.getOrganization() != null ? log.getOrganization().getId() : null)
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .provider(log.getProvider())
                .occurredAt(log.getOccurredAt())
                .metadata(log.getMetadata())
                .build();
    }

    private String toSafeMetadata(Map<String, ?> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : metadata.entrySet()) {
            String key = entry.getKey();
            if (key == null || isSensitive(key)) {
                continue;
            }
            if (!first) {
                builder.append(",");
            }
            first = false;
            builder.append('"').append(escape(key)).append('"').append(":");
            Object value = entry.getValue();
            builder.append('"').append(escape(value != null ? value.toString() : "null")).append('"');
        }
        builder.append("}");
        return builder.toString();
    }

    private boolean isSensitive(String key) {
        String normalized = key.toLowerCase();
        return normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("password")
                || normalized.contains("credential")
                || normalized.contains("key");
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}