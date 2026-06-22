package com.aiworkforce.integration.repository;

import com.aiworkforce.integration.entity.JiraIssueSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JiraIssueSnapshotRepository extends JpaRepository<JiraIssueSnapshot, UUID> {
    Optional<JiraIssueSnapshot> findByConfigIdAndIssueKeyIgnoreCase(UUID configId, String issueKey);
    List<JiraIssueSnapshot> findByAssigneeIdOrderByUpdatedAtDesc(UUID assigneeId);
    List<JiraIssueSnapshot> findByTeamIdOrderByUpdatedAtDesc(UUID teamId);
    List<JiraIssueSnapshot> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);
    List<JiraIssueSnapshot> findByTeamOrganizationIdOrderByUpdatedAtDesc(UUID organizationId);
    long countByConfigIdAndSprintIdIsNotNull(UUID configId);
    long countByConfigIdAndStoryPointsIsNotNull(UUID configId);
    long countByConfigIdAndEpicKeyIsNotNull(UUID configId);
    long countByConfigIdAndFixVersionsIsNotNull(UUID configId);
    long countByConfigIdAndComponentsIsNotNull(UUID configId);
}