package com.aiworkforce.ai.service;

import com.aiworkforce.ai.config.AiProperties;
import com.aiworkforce.ai.entity.AIInsight;
import com.aiworkforce.ai.repository.AIInsightRepository;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Project;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.ProjectRepository;
import com.aiworkforce.integration.entity.GithubCommit;
import com.aiworkforce.integration.entity.GithubContributor;
import com.aiworkforce.integration.entity.GithubPullRequest;
import com.aiworkforce.integration.entity.JiraIssueSnapshot;
import com.aiworkforce.integration.entity.JiraSprintSnapshot;
import com.aiworkforce.integration.repository.GithubCommitRepository;
import com.aiworkforce.integration.repository.GithubContributorRepository;
import com.aiworkforce.integration.repository.GithubPullRequestRepository;
import com.aiworkforce.integration.repository.JiraIssueSnapshotRepository;
import com.aiworkforce.integration.repository.JiraSprintSnapshotRepository;
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
    private final GithubCommitRepository githubCommitRepository;
    private final GithubContributorRepository githubContributorRepository;
    private final GithubPullRequestRepository githubPullRequestRepository;
    private final JiraIssueSnapshotRepository jiraIssueSnapshotRepository;
    private final JiraSprintSnapshotRepository jiraSprintSnapshotRepository;

    public String buildEmployeeContext(Employee employee) {
        if (!aiProperties.getRag().isEnabled()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        appendTeamAndProjects(context, employee.getTeam());
        appendRecentTasks(context, employee);
        appendJiraIssues(context, employee);
        appendTeamIntegrationSignals(context, employee.getTeam());
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
                    .append(" | source: ").append(task.getSourceProvider())
                    .append(" | external: ").append(valueOrNone(task.getExternalTicketRef()))
                    .append(" | sprint: ").append(task.getSprintNumber())
                    .append(" | due: ").append(task.getDueDate())
                    .append("\n");
        }
    }

    private void appendJiraIssues(StringBuilder context, Employee employee) {
        List<JiraIssueSnapshot> issues = jiraIssueSnapshotRepository.findByAssigneeIdOrderByUpdatedAtDesc(employee.getId()).stream()
                .limit(8)
                .toList();

        if (issues.isEmpty()) {
            context.append("- Jira issue signals for employee: none.\n");
            return;
        }

        context.append("- Jira issue signals for employee:\n");
        for (JiraIssueSnapshot issue : issues) {
            context.append("  + ")
                    .append(issue.getIssueKey())
                    .append(" | ").append(limit(valueOrNone(issue.getSummary()), 120))
                    .append(" | status: ").append(valueOrNone(issue.getStatusName()))
                    .append(" | priority: ").append(valueOrNone(issue.getPriorityName()))
                    .append(" | sprint: ").append(valueOrNone(issue.getSprintName()))
                    .append(" | story points: ").append(issue.getStoryPoints())
                    .append(" | due: ").append(issue.getDueDate())
                    .append("\n");
        }
    }

    private void appendTeamIntegrationSignals(StringBuilder context, Team team) {
        if (team == null || team.getId() == null) {
            return;
        }

        appendGithubPullRequests(context, team);
        appendGithubCommits(context, team);
        appendGithubContributors(context, team);
        appendJiraSprints(context, team);
    }

    private void appendGithubPullRequests(StringBuilder context, Team team) {
        List<GithubPullRequest> pullRequests = githubPullRequestRepository.findByTeamIdOrderByProviderUpdatedAtDesc(team.getId()).stream()
                .limit(6)
                .toList();
        if (pullRequests.isEmpty()) {
            context.append("- GitHub pull request signals: none.\n");
            return;
        }
        context.append("- GitHub pull request signals:\n");
        for (GithubPullRequest pr : pullRequests) {
            context.append("  + ")
                    .append(pr.getRepositoryFullName()).append("#").append(pr.getNumber())
                    .append(" | ").append(limit(valueOrNone(pr.getTitle()), 120))
                    .append(" | state: ").append(valueOrNone(pr.getState()))
                    .append(" | merged: ").append(pr.getMerged())
                    .append(" | author: ").append(valueOrNone(pr.getAuthorLogin()))
                    .append("\n");
        }
    }

    private void appendGithubCommits(StringBuilder context, Team team) {
        List<GithubCommit> commits = githubCommitRepository.findByTeamIdOrderByCommittedAtDesc(team.getId()).stream()
                .limit(6)
                .toList();
        if (commits.isEmpty()) {
            context.append("- GitHub commit signals: none.\n");
            return;
        }
        context.append("- GitHub commit signals:\n");
        for (GithubCommit commit : commits) {
            context.append("  + ")
                    .append(commit.getRepositoryFullName())
                    .append(" | ").append(shortSha(commit.getSha()))
                    .append(" | +").append(commit.getAdditions())
                    .append(" -").append(commit.getDeletions())
                    .append(" files: ").append(commit.getChangedFiles())
                    .append(" | ").append(limit(valueOrNone(commit.getMessage()), 120))
                    .append("\n");
        }
    }

    private void appendGithubContributors(StringBuilder context, Team team) {
        List<GithubContributor> contributors = githubContributorRepository.findByTeamIdOrderByContributionsDesc(team.getId()).stream()
                .limit(5)
                .toList();
        if (contributors.isEmpty()) {
            context.append("- GitHub contributor signals: none.\n");
            return;
        }
        context.append("- GitHub contributor signals:\n");
        for (GithubContributor contributor : contributors) {
            context.append("  + ")
                    .append(contributor.getLogin())
                    .append(" | repo: ").append(contributor.getRepositoryFullName())
                    .append(" | contributions: ").append(contributor.getContributions())
                    .append("\n");
        }
    }

    private void appendJiraSprints(StringBuilder context, Team team) {
        List<JiraSprintSnapshot> sprints = jiraSprintSnapshotRepository.findByTeamIdOrderByEndDateDesc(team.getId()).stream()
                .limit(5)
                .toList();
        if (sprints.isEmpty()) {
            context.append("- Jira sprint signals: none.\n");
            return;
        }
        context.append("- Jira sprint signals:\n");
        for (JiraSprintSnapshot sprint : sprints) {
            context.append("  + ")
                    .append(valueOrNone(sprint.getName()))
                    .append(" | state: ").append(valueOrNone(sprint.getState()))
                    .append(" | board: ").append(sprint.getBoardId())
                    .append(" | end: ").append(sprint.getEndDate())
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

    private String shortSha(String sha) {
        if (sha == null || sha.length() <= 8) {
            return valueOrNone(sha);
        }
        return sha.substring(0, 8);
    }
}
