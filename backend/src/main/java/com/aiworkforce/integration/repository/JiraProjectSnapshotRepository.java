package com.aiworkforce.integration.repository;

import com.aiworkforce.integration.entity.JiraProjectSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JiraProjectSnapshotRepository extends JpaRepository<JiraProjectSnapshot, UUID> {
    Optional<JiraProjectSnapshot> findByConfigIdAndJiraDomainIgnoreCaseAndProjectKeyIgnoreCase(UUID configId, String jiraDomain, String projectKey);
    List<JiraProjectSnapshot> findByTeamIdOrderByUpdatedAtDesc(UUID teamId);
    List<JiraProjectSnapshot> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);
    List<JiraProjectSnapshot> findByTeamOrganizationIdOrderByUpdatedAtDesc(UUID organizationId);
}