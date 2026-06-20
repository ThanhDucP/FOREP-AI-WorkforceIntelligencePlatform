package com.aiworkforce.analytics.service;

import com.aiworkforce.analytics.dto.BurnoutResponse;
import com.aiworkforce.core.enums.BurnoutRisk;
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
public class BurnoutAnalyticsService {
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;

    public BurnoutResponse getEmployeeBurnout(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        long open = taskRepository.countByAssigneeIdAndStatusNot(employeeId, TaskStatus.DONE);
        long overdue = taskRepository.countByAssigneeIdAndStatusNotAndDueDateBefore(
                employeeId, TaskStatus.DONE, LocalDateTime.now());

        double workload = valueOrZero(employee.getWorkloadScore());
        double overdueRatio = ratio(overdue, open);
        double outOfHours = valueOrZero(employee.getOutOfHoursPct());
        double cycleTime = valueOrZero(employee.getAvgCycleTimeDays());
        double score = calculateBurnoutScore(workload, overdueRatio, outOfHours, cycleTime, open);
        BurnoutRisk risk = resolveRisk(score);

        return BurnoutResponse.builder()
                .scope("EMPLOYEE")
                .scopeId(employee.getId())
                .scopeName(fullName(employee))
                .burnoutScore(score)
                .burnoutRisk(risk)
                .workloadScore(roundOne(workload))
                .overdueRatio(overdueRatio)
                .outOfHoursPct(roundOne(outOfHours))
                .avgCycleTimeDays(roundOne(cycleTime))
                .openTasks(open)
                .overdueTasks(overdue)
                .recommendation(recommendation(risk))
                .build();
    }

    public BurnoutResponse getTeamBurnout(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));
        List<Employee> members = employeeRepository.findByTeamId(teamId);

        long open = taskRepository.countByTeamIdAndStatusNot(teamId, TaskStatus.DONE);
        long overdue = taskRepository.countByTeamIdAndStatusNotAndDueDateBefore(
                teamId, TaskStatus.DONE, LocalDateTime.now());

        double workload = average(members, Metric.WORKLOAD);
        double outOfHours = average(members, Metric.OUT_OF_HOURS);
        double cycleTime = average(members, Metric.CYCLE_TIME);
        double overdueRatio = ratio(overdue, open);
        double score = calculateBurnoutScore(workload, overdueRatio, outOfHours, cycleTime, open);
        BurnoutRisk risk = resolveRisk(score);

        return BurnoutResponse.builder()
                .scope("TEAM")
                .scopeId(team.getId())
                .scopeName(team.getName())
                .burnoutScore(score)
                .burnoutRisk(risk)
                .workloadScore(roundOne(workload))
                .overdueRatio(overdueRatio)
                .outOfHoursPct(roundOne(outOfHours))
                .avgCycleTimeDays(roundOne(cycleTime))
                .openTasks(open)
                .overdueTasks(overdue)
                .recommendation(recommendation(risk))
                .build();
    }

    private double calculateBurnoutScore(double workload, double overdueRatio, double outOfHoursPct,
                                         double avgCycleTimeDays, long openTasks) {
        double taskLoad = Math.min(100.0, openTasks * 8.0);
        double cyclePressure = Math.min(100.0, avgCycleTimeDays * 10.0);
        double score = (clamp(workload) * 0.40)
                + (clamp(overdueRatio * 100.0) * 0.25)
                + (clamp(outOfHoursPct) * 0.15)
                + (cyclePressure * 0.10)
                + (taskLoad * 0.10);
        return roundOne(score);
    }

    private BurnoutRisk resolveRisk(double score) {
        if (score >= 75.0) {
            return BurnoutRisk.HIGH;
        }
        if (score >= 55.0) {
            return BurnoutRisk.MEDIUM;
        }
        if (score >= 35.0) {
            return BurnoutRisk.WATCH;
        }
        return BurnoutRisk.NONE;
    }

    private String recommendation(BurnoutRisk risk) {
        return switch (risk) {
            case HIGH -> "Rebalance workload immediately and review overdue critical tasks.";
            case MEDIUM -> "Monitor workload and move non-urgent tasks away from this scope.";
            case WATCH -> "Keep an eye on trend changes before adding more work.";
            case NONE -> "Current workload appears stable.";
        };
    }

    private double average(List<Employee> employees, Metric metric) {
        return employees.stream()
                .mapToDouble(employee -> switch (metric) {
                    case WORKLOAD -> valueOrZero(employee.getWorkloadScore());
                    case OUT_OF_HOURS -> valueOrZero(employee.getOutOfHoursPct());
                    case CYCLE_TIME -> valueOrZero(employee.getAvgCycleTimeDays());
                })
                .average()
                .orElse(0.0);
    }

    private double ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return roundThree((double) numerator / denominator);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
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

    private enum Metric {
        WORKLOAD,
        OUT_OF_HOURS,
        CYCLE_TIME
    }
}
