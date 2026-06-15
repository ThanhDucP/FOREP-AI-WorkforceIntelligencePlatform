package com.aiworkforce.task.service;

import com.aiworkforce.core.enums.EventType;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.core.security.AccessPolicyService;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Project;
import com.aiworkforce.identity.entity.Sprint;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.ProjectRepository;
import com.aiworkforce.identity.repository.SprintRepository;
import com.aiworkforce.identity.repository.TeamRepository;
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
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final SprintRepository sprintRepository;
    private final AccessPolicyService accessPolicyService;
    private final EventPublisher eventPublisher;
    private final TaskAssessmentService taskAssessmentService;

    @Transactional
    public Task createTask(TaskRequest request) {
        Task task = new Task();
        task.setStatus(TaskStatus.TODO);
        applyTaskRequest(task, request);
        assessTask(task, request, "AGENT");

        Task savedTask = taskRepository.save(task);

        publishEvent(EventType.TASK_CREATED, savedTask, "Task created: " + savedTask.getTitle());
        return savedTask;
    }

    public Task getTask(UUID id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        accessPolicyService.ensureTaskAccess(task);
        return task;
    }

    public List<Task> getAllTasks() {
        Employee current = accessPolicyService.currentEmployee();
        if (accessPolicyService.isAdmin(current)) {
            return taskRepository.findAll();
        }
        List<UUID> managedTeamIds = teamRepository.findByManagerId(current.getId()).stream()
                .map(Team::getId)
                .toList();
        if (!managedTeamIds.isEmpty()) {
            return taskRepository.findByTeamIdIn(managedTeamIds);
        }
        return Collections.emptyList();
    }

    public List<Task> getMyTasks() {
        Employee currentEmployee = accessPolicyService.currentEmployee();
        return getTasksByEmployee(currentEmployee.getId());
    }

    public List<Task> getTasksByProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        accessPolicyService.ensureProjectAccess(project);
        return taskRepository.findByProjectId(projectId);
    }

    public List<Task> getTasksForCurrentActiveTeam() {
        Employee current = accessPolicyService.currentEmployee();
        return taskRepository.findByTeamId(current.getTeam().getId());
    }

    public List<Task> getTasksByEmployee(UUID employeeId) {
        ensureEmployeeExists(employeeId, "Employee not found with id: " + employeeId);
        return taskRepository.findByAssigneeId(employeeId).stream()
                .filter(task -> accessPolicyService.canAccessTask(accessPolicyService.currentEmployee(), task))
                .toList();
    }

    public List<Task> getTasksByReporter(UUID reporterId) {
        ensureEmployeeExists(reporterId, "Reporter not found with id: " + reporterId);
        return taskRepository.findByReporterId(reporterId).stream()
                .filter(task -> accessPolicyService.canAccessTask(accessPolicyService.currentEmployee(), task))
                .toList();
    }

    public List<Task> getTasksByTeam(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));
        accessPolicyService.ensureTeamAccess(team);
        return taskRepository.findByTeamId(teamId);
    }

    public List<Task> getTasksByOrganization(UUID organizationId) {
        Employee current = accessPolicyService.currentEmployee();
        if (!accessPolicyService.isAdmin(current)) {
            throw new BusinessException("Only admins can view all organization tasks");
        }
        return taskRepository.findByTeamOrganizationId(organizationId);
    }

    public List<Task> getTasksForManagedTeams() {
        Employee currentEmployee = accessPolicyService.currentEmployee();
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
        Employee current = accessPolicyService.currentEmployee();
        return taskRepository.findBySprintId(sprintId).stream()
                .filter(task -> accessPolicyService.canAccessTask(current, task))
                .toList();
    }

    public List<Task> getTasksByStatus(TaskStatus status) {
        Employee current = accessPolicyService.currentEmployee();
        return taskRepository.findByStatus(status).stream()
                .filter(task -> accessPolicyService.canAccessTask(current, task))
                .toList();
    }

    @Transactional
    public Task updateTask(UUID id, TaskRequest request) {
        Task task = getTask(id);
        applyTaskRequest(task, request);
        assessTask(task, request, "AGENT");
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
        taskAssessmentService.assess(task, "AGENT");
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
        task.setDifficultyScore(request.getDifficultyScore());
        task.setProgressPercent(request.getProgressPercent());
        task.setLeadEvaluation(request.getLeadEvaluation());

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

        if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
            if (!project.isActive()) {
                throw new BusinessException("Project is inactive");
            }
            if (request.getTeamId() != null && !project.getTeam().getId().equals(request.getTeamId())) {
                throw new BusinessException("Task team must match the selected project team");
            }
            accessPolicyService.ensureProjectAccess(project);
            task.setProject(project);
            task.setTeam(project.getTeam());
        } else if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
            accessPolicyService.ensureTeamAccess(team);
            task.setProject(null);
            task.setTeam(team);
        } else {
            task.setProject(null);
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

    @Transactional
    public Task assessTask(UUID id) {
        Task task = getTask(id);
        taskAssessmentService.assess(task, "AGENT");
        Task updatedTask = taskRepository.save(task);
        publishEvent(EventType.TASK_UPDATED, updatedTask, "Task assessed: " + updatedTask.getTitle());
        return updatedTask;
    }

    private void assessTask(Task task, TaskRequest request, String defaultSource) {
        Boolean agentAssess = request.getAgentAssess();
        if (agentAssess == null || agentAssess) {
            taskAssessmentService.assess(task, defaultSource);
        } else {
            taskAssessmentService.assess(task, "MANUAL");
        }
    }

    private void ensureEmployeeExists(UUID employeeId, String message) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException(message);
        }
    }

    private void ensureSprintExists(UUID sprintId) {
        if (!sprintRepository.existsById(sprintId)) {
            throw new ResourceNotFoundException("Sprint not found with id: " + sprintId);
        }
    }
}
