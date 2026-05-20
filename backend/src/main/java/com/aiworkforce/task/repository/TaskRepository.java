package com.aiworkforce.task.repository;

import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByAssigneeId(UUID assigneeId);
    List<Task> findByStatusNotAndDueDateBefore(TaskStatus status, LocalDateTime dateTime);
}
