package com.aiworkforce.analytics.service;

import com.aiworkforce.ai.entity.AIInsight;
import com.aiworkforce.ai.repository.AIInsightRepository;
import com.aiworkforce.analytics.dto.AnalyticsDashboardResponse;
import com.aiworkforce.core.enums.BurnoutRisk;
import com.aiworkforce.core.enums.InsightSeverity;
import com.aiworkforce.core.enums.TaskPriority;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.integration.entity.GithubPullRequest;
import com.aiworkforce.integration.entity.JiraIssueSnapshot;
import com.aiworkforce.integration.repository.GithubCommitRepository;
import com.aiworkforce.integration.repository.GithubPullRequestRepository;
import com.aiworkforce.integration.repository.GithubRepositorySnapshotRepository;
import com.aiworkforce.integration.repository.JiraIssueSnapshotRepository;
import com.aiworkforce.integration.repository.JiraSprintSnapshotRepository;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsDashboardService {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final AIInsightRepository aiInsightRepository;
    private final JiraIssueSnapshotRepository jiraIssueSnapshotRepository;
    private final JiraSprintSnapshotRepository jiraSprintSnapshotRepository;
    private final GithubRepositorySnapshotRepository githubRepositorySnapshotRepository;
    private final GithubCommitRepository githubCommitRepository;
    private final GithubPullRequestRepository githubPullRequestRepository;

    public AnalyticsDashboardResponse getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        List<Employee> employees = employeeRepository.findAll();
        List<AIInsight> insights = aiInsightRepository.findAll();
        List<Task> tasks = taskRepository.findAll();
        List<JiraIssueSnapshot> jiraIssues = jiraIssueSnapshotRepository.findAll();
        List<AnalyticsDashboardResponse.EmployeeWorkload> workload = buildWorkloadByEmployee(employees, now);

        return AnalyticsDashboardResponse.builder()
                .totalTasks(tasks.size())
                .completedTasks(tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count())
                .overdueTasks(tasks.stream().filter(task -> task.getStatus() != TaskStatus.DONE && task.getDueDate() != null && task.getDueDate().isBefore(now)).count())
                .workloadByEmployee(workload)
                .burnoutRiskCount(buildBurnoutRiskCount(employees))
                .recentActivity(buildRecentActivity())
                .aiInsightSummary(buildInsightSummary(insights))
                .projectHealth(buildProjectHealth(tasks, jiraIssues, now))
                .teamAnalytics(buildTeamAnalytics(tasks, workload, employees))
                .githubAnalytics(buildGithubAnalytics(now))
                .sprintAnalytics(buildSprintAnalytics())
                .build();
    }

    private AnalyticsDashboardResponse.ProjectHealth buildProjectHealth(List<Task> tasks, List<JiraIssueSnapshot> jiraIssues, LocalDateTime now) {
        long totalIssues = tasks.size();
        long completed = tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
        long overdue = tasks.stream().filter(task -> task.getStatus() != TaskStatus.DONE && task.getDueDate() != null && task.getDueDate().isBefore(now)).count();
        long blocked = jiraIssues.stream()
                .filter(issue -> issue.getStatusName() != null)
                .filter(issue -> {
                    String status = issue.getStatusName().toLowerCase();
                    return status.contains("blocked") || status.contains("impediment");
                })
                .count();
        double completionRate = totalIssues == 0 ? 0.0 : (completed * 100.0 / totalIssues);
        int score = calculateHealthScore(totalIssues, completionRate, overdue, blocked);

        return AnalyticsDashboardResponse.ProjectHealth.builder()
                .score(score)
                .totalIssues(totalIssues)
                .overdueIssues(overdue)
                .blockedIssues(blocked)
                .completionRate(roundOne(completionRate))
                .priorityDistribution(buildPriorityDistribution(tasks))
                .statusDistribution(buildStatusDistribution(tasks))
                .build();
    }

    private AnalyticsDashboardResponse.TeamAnalytics buildTeamAnalytics(List<Task> tasks, List<AnalyticsDashboardResponse.EmployeeWorkload> workload, List<Employee> employees) {
        long assigned = tasks.stream().filter(task -> task.getAssignee() != null).count();
        long unassigned = tasks.size() - assigned;
        long open = tasks.stream().filter(task -> task.getStatus() != TaskStatus.DONE).count();
        double averageOpen = employees.isEmpty() ? 0.0 : open * 1.0 / employees.size();
        return AnalyticsDashboardResponse.TeamAnalytics.builder()
                .assignedIssueCount(assigned)
                .unassignedIssueCount(unassigned)
                .averageOpenIssuesPerEmployee(roundOne(averageOpen))
                .workloadDistribution(workload)
                .build();
    }

    private AnalyticsDashboardResponse.GithubAnalytics buildGithubAnalytics(LocalDateTime now) {
        long prCount = githubPullRequestRepository.count();
        long openPrCount = githubPullRequestRepository.countByStateIgnoreCase("open");
        long mergedPrCount = githubPullRequestRepository.countByMergedTrue();
        long reviewDelayRisk = githubPullRequestRepository.findAll().stream()
                .filter(pr -> Boolean.TRUE.equals(pr.getMerged()) == false)
                .filter(pr -> pr.getProviderCreatedAt() != null && pr.getProviderCreatedAt().isBefore(now.minusDays(2)))
                .count();
        long repoCount = githubRepositorySnapshotRepository.count();
        long commitCount = githubCommitRepository.count();
        return AnalyticsDashboardResponse.GithubAnalytics.builder()
                .available(repoCount > 0 || commitCount > 0 || prCount > 0)
                .repositoryCount(repoCount)
                .commitCount(commitCount)
                .pullRequestCount(prCount)
                .openPullRequestCount(openPrCount)
                .mergedPullRequestCount(mergedPrCount)
                .reviewDelayRiskCount(reviewDelayRisk)
                .build();
    }

    private AnalyticsDashboardResponse.SprintAnalytics buildSprintAnalytics() {
        long sprintCount = jiraSprintSnapshotRepository.count();
        long issuesWithSprint = jiraIssueSnapshotRepository.findAll().stream()
                .filter(issue -> issue.getSprintId() != null)
                .count();
        long issuesWithStoryPoints = jiraIssueSnapshotRepository.findAll().stream()
                .filter(issue -> issue.getStoryPoints() != null)
                .count();
        boolean available = sprintCount > 0 || issuesWithSprint > 0;
        return AnalyticsDashboardResponse.SprintAnalytics.builder()
                .available(available)
                .message(available ? null : "No Sprint data available for this project.")
                .sprintCount(sprintCount)
                .issuesWithSprint(issuesWithSprint)
                .issuesWithStoryPoints(issuesWithStoryPoints)
                .build();
    }

    private int calculateHealthScore(long totalIssues, double completionRate, long overdue, long blocked) {
        if (totalIssues == 0) return 100;
        double overduePenalty = overdue * 100.0 / totalIssues;
        double blockedPenalty = blocked * 100.0 / totalIssues;
        double score = 70 + (completionRate * 0.3) - overduePenalty - blockedPenalty;
        return (int) Math.max(0, Math.min(100, Math.round(score)));
    }

    private Map<String, Long> buildPriorityDistribution(List<Task> tasks) {
        Map<String, Long> counts = tasks.stream()
                .collect(Collectors.groupingBy(task -> task.getPriority() != null ? task.getPriority().name() : "UNSPECIFIED", Collectors.counting()));
        for (TaskPriority priority : TaskPriority.values()) {
            counts.putIfAbsent(priority.name(), 0L);
        }
        return counts;
    }

    private Map<String, Long> buildStatusDistribution(List<Task> tasks) {
        Map<String, Long> counts = tasks.stream()
                .collect(Collectors.groupingBy(task -> task.getStatus() != null ? task.getStatus().name() : "UNSPECIFIED", Collectors.counting()));
        for (TaskStatus status : TaskStatus.values()) {
            counts.putIfAbsent(status.name(), 0L);
        }
        return counts;
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

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}