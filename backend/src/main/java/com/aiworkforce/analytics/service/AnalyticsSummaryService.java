package com.aiworkforce.analytics.service;

import com.aiworkforce.analytics.dto.AnalyticsSummaryResponse;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsSummaryService {
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;

    public AnalyticsSummaryResponse getEmployeeSummary(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        long completed = taskRepository.countByAssigneeIdAndStatus(employeeId, TaskStatus.DONE);
        long open = taskRepository.countByAssigneeIdAndStatusNot(employeeId, TaskStatus.DONE);
        long overdue = taskRepository.countByAssigneeIdAndStatusNotAndDueDateBefore(
                employeeId, TaskStatus.DONE, LocalDateTime.now());

        return AnalyticsSummaryResponse.builder()
                .scope("EMPLOYEE")
                .scopeId(employee.getId())
                .scopeName(fullName(employee))
                .completedTasks(completed)
                .openTasks(open)
                .overdueTasks(overdue)
                .overdueRatio(ratio(overdue, open))
                .workloadScore(roundOne(valueOrZero(employee.getWorkloadScore())))
                .memberCount(1)
                .build();
    }

    public AnalyticsSummaryResponse getTeamSummary(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));
        List<Employee> members = employeeRepository.findByTeamId(teamId);

        long completed = taskRepository.countByTeamIdAndStatus(teamId, TaskStatus.DONE);
        long open = taskRepository.countByTeamIdAndStatusNot(teamId, TaskStatus.DONE);
        long overdue = taskRepository.countByTeamIdAndStatusNotAndDueDateBefore(
                teamId, TaskStatus.DONE, LocalDateTime.now());

        double workload = members.stream()
                .mapToDouble(employee -> valueOrZero(employee.getWorkloadScore()))
                .average()
                .orElse(0.0);

        return AnalyticsSummaryResponse.builder()
                .scope("TEAM")
                .scopeId(team.getId())
                .scopeName(team.getName())
                .completedTasks(completed)
                .openTasks(open)
                .overdueTasks(overdue)
                .overdueRatio(ratio(overdue, open))
                .workloadScore(roundOne(workload))
                .memberCount(members.size())
                .build();
    }

    private double ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return roundThree((double) numerator / denominator);
    }

    private double valueOrZero(Double value) {
        return value != null ? value : 0.0;
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double roundThree(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private String fullName(Employee employee) {
        String first = employee.getFirstName() != null ? employee.getFirstName() : "";
        String last = employee.getLastName() != null ? employee.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isBlank() ? employee.getId().toString() : name;
    }
}
