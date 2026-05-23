package com.aiworkforce.task.service;

import com.aiworkforce.core.enums.EventType;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Sprint;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.SprintRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.identity.service.EmployeeService;
import com.aiworkforce.event.entity.WorkloadEvent;
import com.aiworkforce.event.publisher.EventPublisher;
import com.aiworkforce.task.dto.TaskRequest;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;
    private final SprintRepository sprintRepository;
    private final EmployeeService employeeService;
    private final EventPublisher eventPublisher;

    @Transactional
    public Task createTask(TaskRequest request) {
        Task task = new Task();
        task.setStatus(TaskStatus.TODO);
        applyTaskRequest(task, request);

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

    public List<Task> getMyTasks() {
        Employee currentEmployee = employeeService.getCurrentEmployee();
        return getTasksByEmployee(currentEmployee.getId());
    }

    public List<Task> getTasksByEmployee(UUID employeeId) {
        ensureEmployeeExists(employeeId, "Employee not found with id: " + employeeId);
        return taskRepository.findByAssigneeId(employeeId);
    }

    public List<Task> getTasksByReporter(UUID reporterId) {
        ensureEmployeeExists(reporterId, "Reporter not found with id: " + reporterId);
        return taskRepository.findByReporterId(reporterId);
    }

    public List<Task> getTasksByTeam(UUID teamId) {
        ensureTeamExists(teamId);
        return taskRepository.findByTeamId(teamId);
    }

    public List<Task> getTasksByOrganization(UUID organizationId) {
        return taskRepository.findByTeamOrganizationId(organizationId);
    }

    public List<Task> getTasksForManagedTeams() {
        Employee currentEmployee = employeeService.getCurrentEmployee();
        List<UUID> teamIds = teamRepository.findByManagerId(currentEmployee.getId()).stream()
                .map(Team::getId)
                .collect(Collectors.toList());

        if (teamIds.isEmpty()) {
            return Collections.emptyList();
        }

        return taskRepository.findByTeamIdIn(teamIds);
    }

    public List<Task> getTasksBySprint(UUID sprintId) {
        ensureSprintExists(sprintId);
        return taskRepository.findBySprintId(sprintId);
    }

    public List<Task> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    @Transactional
    public Task updateTask(UUID id, TaskRequest request) {
        Task task = getTask(id);
        applyTaskRequest(task, request);
        Task updatedTask = taskRepository.save(task);
        publishEvent(EventType.TASK_UPDATED, updatedTask, "Task updated: " + updatedTask.getTitle());
        return updatedTask;
    }

    @Transactional
    public Task updateTaskStatus(UUID id, TaskStatus status) {
        Task task = getTask(id);
        task.setStatus(status);
        if (status == TaskStatus.DONE) {
            task.setCompletedAt(LocalDateTime.now());
        } else {
            task.setCompletedAt(null);
        }
        Task updatedTask = taskRepository.save(task);

        EventType type = EventType.TASK_UPDATED;
        if (status == TaskStatus.DONE) {
            type = EventType.TASK_COMPLETED;
        }
        publishEvent(type, updatedTask, "Task status changed to " + status);
        return updatedTask;
    }

    @Transactional
    public void deleteTask(UUID id) {
        Task task = getTask(id);
        taskRepository.delete(task);
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

    private void applyTaskRequest(Task task, TaskRequest request) {
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setEstimatedHours(request.getEstimatedHours());
        task.setExternalTicketRef(request.getExternalTicketRef());
        task.setSprintNumber(request.getSprintNumber());
        task.setStoryPoints(request.getStoryPoints());

        Employee assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = employeeRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
        }
        task.setAssignee(assignee);

        if (request.getReporterId() != null) {
            Employee reporter = employeeRepository.findById(request.getReporterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reporter not found"));
            task.setReporter(reporter);
        } else {
            task.setReporter(null);
        }

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
            task.setTeam(team);
        } else {
            task.setTeam(assignee != null ? assignee.getTeam() : null);
        }

        if (request.getSprintId() != null) {
            Sprint sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));
            task.setSprint(sprint);
        } else {
            task.setSprint(null);
        }
    }

    private void ensureEmployeeExists(UUID employeeId, String message) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException(message);
        }
    }

    private void ensureTeamExists(UUID teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Team not found with id: " + teamId);
        }
    }

    private void ensureSprintExists(UUID sprintId) {
        if (!sprintRepository.existsById(sprintId)) {
            throw new ResourceNotFoundException("Sprint not found with id: " + sprintId);
        }
    }
}
