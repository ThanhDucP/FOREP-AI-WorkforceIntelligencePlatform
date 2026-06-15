package com.aiworkforce.task.repository;

import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByAssigneeId(UUID assigneeId);
    List<Task> findByReporterId(UUID reporterId);
    List<Task> findByTeamId(UUID teamId);
    List<Task> findByTeamIdIn(List<UUID> teamIds);
    List<Task> findByTeamOrganizationId(UUID organizationId);
    List<Task> findBySprintId(UUID sprintId);
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByStatusNotAndDueDateBefore(TaskStatus status, LocalDateTime dateTime);
    long countByStatus(TaskStatus status);
    
    java.util.Optional<Task> findByExternalTicketRefAndSourceProvider(String externalTicketRef, com.aiworkforce.core.enums.IntegrationProvider sourceProvider);
    java.util.Optional<Task> findByExternalTicketRefAndSourceProviderAndProjectId(String externalTicketRef, com.aiworkforce.core.enums.IntegrationProvider sourceProvider, UUID projectId);
}
