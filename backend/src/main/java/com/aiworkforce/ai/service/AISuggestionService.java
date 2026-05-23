package com.aiworkforce.ai.service;

import com.aiworkforce.ai.dto.AISuggestionResponse;
import com.aiworkforce.ai.entity.AISuggestion;
import com.aiworkforce.ai.repository.AISuggestionRepository;
import com.aiworkforce.core.enums.EventType;
import com.aiworkforce.core.enums.NotificationType;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.core.service.NotificationService;
import com.aiworkforce.event.entity.WorkloadEvent;
import com.aiworkforce.event.publisher.EventPublisher;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.identity.service.EmployeeService;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AISuggestionService {

    private final AISuggestionRepository aiSuggestionRepository;
    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;
    private final EmployeeService employeeService;
    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final EventPublisher eventPublisher;

    public List<AISuggestionResponse> getSuggestionsForSprint(Integer sprintNumber) {
        return aiSuggestionRepository.findBySprintNumberOrderByCreatedAtDesc(sprintNumber).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AISuggestionResponse> getSuggestionsForEmployee(UUID employeeId) {
        return aiSuggestionRepository.findBySourceEmployeeIdOrTargetEmployeeIdOrderByCreatedAtDesc(employeeId, employeeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AISuggestionResponse> getSuggestionsForTeam(UUID teamId) {
        List<UUID> employeeIds = employeeRepository.findByTeamId(teamId).stream()
                .map(Employee::getId)
                .collect(Collectors.toList());

        if (employeeIds.isEmpty()) {
            return Collections.emptyList();
        }

        return aiSuggestionRepository.findBySourceEmployeeIdInOrTargetEmployeeIdInOrderByCreatedAtDesc(employeeIds, employeeIds).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AISuggestionResponse> getSuggestionsForManagedTeams() {
        Employee currentEmployee = employeeService.getCurrentEmployee();
        List<UUID> teamIds = teamRepository.findByManagerId(currentEmployee.getId()).stream()
                .map(Team::getId)
                .collect(Collectors.toList());

        if (teamIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> employeeIds = employeeRepository.findAll().stream()
                .filter(employee -> employee.getTeam() != null && teamIds.contains(employee.getTeam().getId()))
                .map(Employee::getId)
                .collect(Collectors.toList());

        if (employeeIds.isEmpty()) {
            return Collections.emptyList();
        }

        return aiSuggestionRepository.findBySourceEmployeeIdInOrTargetEmployeeIdInOrderByCreatedAtDesc(employeeIds, employeeIds).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AISuggestionResponse> getSuggestionsForOrganization(UUID organizationId) {
        List<UUID> employeeIds = employeeRepository.findByTeamOrganizationId(organizationId).stream()
                .map(Employee::getId)
                .collect(Collectors.toList());

        if (employeeIds.isEmpty()) {
            return Collections.emptyList();
        }

        return aiSuggestionRepository.findBySourceEmployeeIdInOrTargetEmployeeIdInOrderByCreatedAtDesc(employeeIds, employeeIds).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AISuggestionResponse adoptSuggestion(UUID id) {
        AISuggestion suggestion = aiSuggestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AI Suggestion not found with id: " + id));

        if (Boolean.TRUE.equals(suggestion.getIsAdopted())) {
            throw new IllegalStateException("This AI Suggestion has already been adopted.");
        }

        // Reassign the task
        Task task = taskRepository.findById(suggestion.getSourceTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + suggestion.getSourceTaskId()));

        Employee targetEmployee = employeeRepository.findById(suggestion.getTargetEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Target Employee not found with id: " + suggestion.getTargetEmployeeId()));

        Employee sourceEmployee = employeeRepository.findById(suggestion.getSourceEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Source Employee not found with id: " + suggestion.getSourceEmployeeId()));

        // Assign the task to the target employee
        task.setAssignee(targetEmployee);
        taskRepository.save(task);

        // Mark suggestion as adopted
        suggestion.setIsAdopted(true);
        AISuggestion savedSuggestion = aiSuggestionRepository.save(suggestion);

        // Publish event
        WorkloadEvent event = new WorkloadEvent();
        event.setEventType(EventType.WORKLOAD_REBALANCED);
        event.setTask(task);
        event.setEmployee(targetEmployee);
        event.setActorId(sourceEmployee.getId()); // tracking context of rebalance
        event.setEventDetails(String.format("Task '%s' rebalanced from %s to %s to prevent burnout.",
                task.getTitle(),
                sourceEmployee.getFirstName() + " " + sourceEmployee.getLastName(),
                targetEmployee.getFirstName() + " " + targetEmployee.getLastName()));
        eventPublisher.publishEvent(event);

        // Notify target employee
        notificationService.createNotification(
                targetEmployee.getId(),
                NotificationType.WORKLOAD_REBALANCE,
                "Tái cân bằng công việc",
                String.format("Bạn đã được giao tác vụ '%s' từ %s để tối ưu hóa tải công việc sprint.",
                        task.getTitle(),
                        sourceEmployee.getFirstName() + " " + sourceEmployee.getLastName()),
                task.getId(),
                null
        );

        // Notify source employee
        notificationService.createNotification(
                sourceEmployee.getId(),
                NotificationType.WORKLOAD_REBALANCE,
                "Tác vụ được chuyển giao",
                String.format("Tác vụ '%s' của bạn đã được chuyển giao cho %s nhằm giảm tải công việc.",
                        task.getTitle(),
                        targetEmployee.getFirstName() + " " + targetEmployee.getLastName()),
                task.getId(),
                null
        );

        return mapToResponse(savedSuggestion);
    }

    public AISuggestionResponse mapToResponse(AISuggestion suggestion) {
        if (suggestion == null) return null;

        String sourceEmployeeName = "";
        if (suggestion.getSourceEmployeeId() != null) {
            Employee emp = employeeRepository.findById(suggestion.getSourceEmployeeId()).orElse(null);
            if (emp != null) {
                sourceEmployeeName = emp.getFirstName() + " " + emp.getLastName();
            }
        }

        String targetEmployeeName = "";
        if (suggestion.getTargetEmployeeId() != null) {
            Employee emp = employeeRepository.findById(suggestion.getTargetEmployeeId()).orElse(null);
            if (emp != null) {
                targetEmployeeName = emp.getFirstName() + " " + emp.getLastName();
            }
        }

        String taskTitle = "";
        if (suggestion.getSourceTaskId() != null) {
            Task t = taskRepository.findById(suggestion.getSourceTaskId()).orElse(null);
            if (t != null) {
                taskTitle = t.getTitle();
            }
        }

        return AISuggestionResponse.builder()
                .id(suggestion.getId())
                .sprintNumber(suggestion.getSprintNumber())
                .suggestionType(suggestion.getSuggestionType())
                .description(suggestion.getDescription())
                .confidenceScore(suggestion.getConfidenceScore())
                .sourceEmployeeId(suggestion.getSourceEmployeeId())
                .sourceEmployeeName(sourceEmployeeName)
                .targetEmployeeId(suggestion.getTargetEmployeeId())
                .targetEmployeeName(targetEmployeeName)
                .sourceTaskId(suggestion.getSourceTaskId())
                .sourceTaskTitle(taskTitle)
                .isAdopted(suggestion.getIsAdopted())
                .createdAt(suggestion.getCreatedAt())
                .build();
    }
}
