package com.aiworkforce.integration.repository;

import com.aiworkforce.integration.entity.IntegrationSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IntegrationSyncLogRepository extends JpaRepository<IntegrationSyncLog, UUID> {
    List<IntegrationSyncLog> findTop20ByConfigIdOrderByStartedAtDesc(UUID configId);
}
