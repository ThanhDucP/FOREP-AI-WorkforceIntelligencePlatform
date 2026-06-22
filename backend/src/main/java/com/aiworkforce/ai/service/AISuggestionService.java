package com.aiworkforce.ai.service;

import com.aiworkforce.ai.dto.AISuggestionResponse;
import com.aiworkforce.ai.entity.AISuggestion;
import com.aiworkforce.ai.repository.AISuggestionRepository;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.identity.service.EmployeeService;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AISuggestionService {

    private final AISuggestionRepository aiSuggestionRepository;
    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;
    private final EmployeeService employeeService;
    private final TaskRepository taskRepository;

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
