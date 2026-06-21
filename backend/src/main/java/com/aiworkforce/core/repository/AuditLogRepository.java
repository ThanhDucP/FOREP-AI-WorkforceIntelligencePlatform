package com.aiworkforce.core.repository;

import com.aiworkforce.core.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findTop100ByOrderByOccurredAtDesc();
    List<AuditLog> findTop100ByOrganizationIdOrderByOccurredAtDesc(UUID organizationId);
}