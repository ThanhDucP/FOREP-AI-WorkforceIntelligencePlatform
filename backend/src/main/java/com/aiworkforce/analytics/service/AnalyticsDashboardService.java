package com.aiworkforce.analytics.service;

import com.aiworkforce.ai.entity.AIInsight;
import com.aiworkforce.ai.repository.AIInsightRepository;
import com.aiworkforce.analytics.dto.AnalyticsDashboardResponse;
import com.aiworkforce.core.enums.BurnoutRisk;
import com.aiworkforce.core.enums.InsightSeverity;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsDashboardService {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final AIInsightRepository aiInsightRepository;

    public AnalyticsDashboardResponse getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        List<Employee> employees = employeeRepository.findAll();
        List<AIInsight> insights = aiInsightRepository.findAll();

        return AnalyticsDashboardResponse.builder()
                .totalTasks(taskRepository.count())
                .completedTasks(taskRepository.countByStatus(TaskStatus.DONE))
                .overdueTasks(taskRepository.countByStatusNotAndDueDateBefore(TaskStatus.DONE, now))
                .workloadByEmployee(buildWorkloadByEmployee(employees, now))
                .burnoutRiskCount(buildBurnoutRiskCount(employees))
                .recentActivity(buildRecentActivity())
                .aiInsightSummary(buildInsightSummary(insights))
                .build();
    }

    private List<AnalyticsDashboardResponse.EmployeeWorkload> buildWorkloadByEmployee(List<Employee> employees, LocalDateTime now) {
        return employees.stream()
                .map(employee -> AnalyticsDashboardResponse.EmployeeWorkload.builder()
                        .employeeId(employee.getId())
                        .employeeName(fullName(employee))
                        .teamId(employee.getTeam() != null ? employee.getTeam().getId() : null)
                        .teamName(employee.getTeam() != null ? employee.getTeam().getName() : null)
                        .workloadScore(valueOrZero(employee.getWorkloadScore()))
                        .openTasks(taskRepository.countByAssigneeIdAndStatusNot(employee.getId(), TaskStatus.DONE))
                        .overdueTasks(taskRepository.countByAssigneeIdAndStatusNotAndDueDateBefore(employee.getId(), TaskStatus.DONE, now))
                        .burnoutRisk(employee.getBurnoutRisk() != null ? employee.getBurnoutRisk() : BurnoutRisk.NONE)
                        .build())
                .toList();
    }

    private Map<BurnoutRisk, Long> buildBurnoutRiskCount(List<Employee> employees) {
        Map<BurnoutRisk, Long> counts = new EnumMap<>(BurnoutRisk.class);
        for (BurnoutRisk risk : BurnoutRisk.values()) {
            counts.put(risk, 0L);
        }
        for (Employee employee : employees) {
            BurnoutRisk risk = employee.getBurnoutRisk() != null ? employee.getBurnoutRisk() : BurnoutRisk.NONE;
            counts.put(risk, counts.get(risk) + 1);
        }
        return counts;
    }

    private List<AnalyticsDashboardResponse.RecentActivity> buildRecentActivity() {
        return taskRepository.findTop10ByOrderByUpdatedAtDesc().stream()
                .map(task -> AnalyticsDashboardResponse.RecentActivity.builder()
                        .taskId(task.getId())
                        .title(task.getTitle())
                        .status(task.getStatus())
                        .sourceProvider(task.getSourceProvider())
                        .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                        .assigneeName(task.getAssignee() != null ? fullName(task.getAssignee()) : null)
                        .updatedAt(task.getUpdatedAt())
                        .build())
                .toList();
    }

    private AnalyticsDashboardResponse.AiInsightSummary buildInsightSummary(List<AIInsight> insights) {
        Map<InsightSeverity, Long> counts = new EnumMap<>(InsightSeverity.class);
        for (InsightSeverity severity : InsightSeverity.values()) {
            counts.put(severity, 0L);
        }
        for (AIInsight insight : insights) {
            if (insight.getSeverity() != null) {
                counts.put(insight.getSeverity(), counts.get(insight.getSeverity()) + 1);
            }
        }
        return AnalyticsDashboardResponse.AiInsightSummary.builder()
                .totalInsights(insights.size())
                .severityCount(counts)
                .latestSummaries(aiInsightRepository.findTop10ByOrderByCreatedAtDesc().stream()
                        .map(AIInsight::getSummary)
                        .filter(summary -> summary != null && !summary.isBlank())
                        .toList())
                .build();
    }

    private String fullName(Employee employee) {
        String first = employee.getFirstName() != null ? employee.getFirstName() : "";
        String last = employee.getLastName() != null ? employee.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isBlank() ? employee.getId().toString() : name;
    }

    private double valueOrZero(Double value) {
        return value != null ? value : 0.0;
    }
}