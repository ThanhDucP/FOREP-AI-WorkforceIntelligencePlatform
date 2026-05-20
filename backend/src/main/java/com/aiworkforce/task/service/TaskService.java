package com.aiworkforce.task.service;

import com.aiworkforce.core.enums.EventType;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.event.entity.WorkloadEvent;
import com.aiworkforce.event.publisher.EventPublisher;
import com.aiworkforce.task.dto.TaskRequest;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public Task createTask(TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(request.getDueDate());
        task.setEstimatedHours(request.getEstimatedHours());

        if (request.getAssigneeId() != null) {
            Employee assignee = employeeRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
            task.setAssignee(assignee);
        }

        Task savedTask = taskRepository.save(task);

        publishEvent(EventType.TASK_CREATED, savedTask, "Task created: " + savedTask.getTitle());
        return savedTask;
    }

    public Task getTask(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @Transactional
    public Task updateTaskStatus(UUID id, TaskStatus status) {
        Task task = getTask(id);
        task.setStatus(status);
        Task updatedTask = taskRepository.save(task);

        EventType type = EventType.TASK_UPDATED;
        if (status == TaskStatus.DONE) {
            type = EventType.TASK_COMPLETED;
        }
        publishEvent(type, updatedTask, "Task status changed to " + status);
        return updatedTask;
    }
    
    @Transactional
    @Scheduled(cron = "0 0 1 * * ?") // Runs daily at 1:00 AM
    public void checkOverdueTasks() {
        log.info("Running scheduled check for overdue tasks...");
        LocalDateTime now = LocalDateTime.now();
        List<Task> overdueTasks = taskRepository.findByStatusNotAndDueDateBefore(TaskStatus.DONE, now);
        
        for (Task task : overdueTasks) {
            if (task.getStatus() != TaskStatus.OVERDUE) {
                task.setStatus(TaskStatus.OVERDUE);
                taskRepository.save(task);
                publishEvent(EventType.TASK_OVERDUE, task, "Tác vụ quá hạn: " + task.getTitle());
                log.info("Task {} has been marked as OVERDUE", task.getId());
            }
        }
    }

    private void publishEvent(EventType type, Task task, String details) {
        WorkloadEvent event = new WorkloadEvent();
        event.setEventType(type);
        event.setTask(task);
        event.setEmployee(task.getAssignee()); // May be null initially
        event.setEventDetails(details);
        
        eventPublisher.publishEvent(event);
    }
}
