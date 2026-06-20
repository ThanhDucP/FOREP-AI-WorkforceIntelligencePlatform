package com.aiworkforce.integration.repository;

import com.aiworkforce.integration.entity.JiraSprintSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JiraSprintSnapshotRepository extends JpaRepository<JiraSprintSnapshot, UUID> {
    Optional<JiraSprintSnapshot> findByConfigIdAndSprintId(UUID configId, Integer sprintId);
    List<JiraSprintSnapshot> findByTeamIdOrderByEndDateDesc(UUID teamId);
}
