package com.aiworkforce.ai.service;

import com.aiworkforce.ai.config.AiProperties;
import com.aiworkforce.ai.entity.AIInsight;
import com.aiworkforce.ai.repository.AIInsightRepository;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Project;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.ProjectRepository;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RagContextService {

    private final AiProperties aiProperties;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final AIInsightRepository insightRepository;

    public String buildEmployeeContext(Employee employee) {
        if (!aiProperties.getRag().isEnabled()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        appendTeamAndProjects(context, employee.getTeam());
        appendRecentTasks(context, employee);
        appendPreviousInsights(context, employee);
        return limit(context.toString(), aiProperties.getRag().getMaxContextCharacters());
    }

    private void appendTeamAndProjects(StringBuilder context, Team team) {
        if (team == null) {
            context.append("- Employee is not assigned to an active team.\n");
            return;
        }

        context.append("- Team: ").append(team.getName()).append("\n");
        List<Project> projects = projectRepository.findByTeamId(team.getId()).stream()
                .filter(Project::isActive)
                .limit(8)
                .toList();

        if (projects.isEmpty()) {
            context.append("- Active projects: none registered.\n");
            return;
        }

        context.append("- Active projects:\n");
        for (Project project : projects) {
            context.append("  + ")
                    .append(project.getName())
                    .append(" | GitHub: ").append(valueOrNone(project.getGithubRepository()))
                    .append(" | Jira: ").append(valueOrNone(project.getJiraDomain()))
                    .append("/").append(valueOrNone(project.getJiraProjectKey()))
                    .append("\n");
        }
    }

    private void appendRecentTasks(StringBuilder context, Employee employee) {
        List<Task> tasks = taskRepository.findByAssigneeId(employee.getId()).stream()
                .sorted(Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(aiProperties.getRag().getMaxTasks())
                .toList();

        if (tasks.isEmpty()) {
            context.append("- Recent assigned tasks: none.\n");
            return;
        }

        context.append("- Recent assigned tasks:\n");
        for (Task task : tasks) {
            context.append("  + ")
                    .append(task.getTitle())
                    .append(" | status: ").append(task.getStatus())
                    .append(" | priority: ").append(task.getPriority())
                    .append(" | project: ").append(task.getProject() != null ? task.getProject().getName() : "none")
                    .append(" | due: ").append(task.getDueDate())
                    .append("\n");
        }
    }

    private void appendPreviousInsights(StringBuilder context, Employee employee) {
        List<AIInsight> insights = insightRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId()).stream()
                .limit(aiProperties.getRag().getMaxPreviousInsights())
                .toList();

        if (insights.isEmpty()) {
            context.append("- Previous AI insights: none.\n");
            return;
        }

        context.append("- Previous AI insights:\n");
        for (AIInsight insight : insights) {
            context.append("  + ")
                    .append(insight.getSeverity())
                    .append(": ")
                    .append(limit(valueOrNone(insight.getSummary()), 240))
                    .append("\n");
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String valueOrNone(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }
}
