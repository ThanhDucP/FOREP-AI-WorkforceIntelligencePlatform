package com.aiworkforce.integration.repository;

import com.aiworkforce.integration.entity.TaskIntegrationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskIntegrationConfigRepository extends JpaRepository<TaskIntegrationConfig, UUID> {
    List<TaskIntegrationConfig> findByTeamId(UUID teamId);
    Optional<TaskIntegrationConfig> findByIdAndIsActiveTrue(UUID id);
}
