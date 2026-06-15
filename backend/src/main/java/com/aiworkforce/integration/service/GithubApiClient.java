package com.aiworkforce.integration.service;

import com.aiworkforce.core.enums.IntegrationProvider;
import com.aiworkforce.core.enums.TaskPriority;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.service.TeamMembershipService;
import com.aiworkforce.integration.entity.TaskIntegrationConfig;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.repository.TaskRepository;
import com.aiworkforce.task.service.TaskAssessmentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubApiClient {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;
    private final TaskAssessmentService taskAssessmentService;
    private final TeamMembershipService membershipService;

    // Field for testing
    private String githubApiUrl = "https://api.github.com";

    @Transactional
    public void syncIssues(TaskIntegrationConfig config) {
        String projectKey = config.getProjectKey(); // e.g. "owner/repo"
        String accessToken = config.getAccessToken();

        if (projectKey == null || projectKey.isBlank()) {
            log.warn("Project key is blank, skipping GitHub sync for config: {}", config.getId());
            return;
        }

        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Access token is blank, skipping GitHub sync for config: {}", config.getId());
            return;
        }

        log.info("Starting active GitHub sync for repository: {}", projectKey);

        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(githubApiUrl)
                    .defaultHeader("Authorization", "Bearer " + accessToken)
                    .defaultHeader("User-Agent", "FOREP-AI-Platform")
                    .defaultHeader("Accept", "application/vnd.github+json")
                    .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                    .build();

            String responseJson = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/" + projectKey + "/issues")
                            .queryParam("state", "all")
                            .queryParam("per_page", 100)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseJson == null) {
                log.warn("No issues found or empty response from GitHub API");
                return;
            }

            JsonNode rootNode = objectMapper.readTree(responseJson);
            if (!rootNode.isArray()) {
                log.warn("Expected JSON array from GitHub API, but got: {}", responseJson);
                return;
            }

            List<GithubCommitSignal> commitSignals = fetchCommitSignals(webClient, projectKey);
            int count = 0;
            for (JsonNode issueNode : rootNode) {
                // In GitHub, Pull Requests are also considered issues. They have a "pull_request" key in the JSON.
                if (issueNode.has("pull_request")) {
                    continue;
                }

                String issueNumber = issueNode.path("number").asText();
                String externalTicketRef = "GH-" + issueNumber;
                String title = issueNode.path("title").asText();
                String body = issueNode.path("body").asText();
                String htmlUrl = issueNode.path("html_url").asText();
                String state = issueNode.path("state").asText();

                Employee assignee = null;
                JsonNode assigneeNode = issueNode.path("assignee");
                if (!assigneeNode.isMissingNode() && !assigneeNode.isNull()) {
                    String assigneeEmail = assigneeNode.path("email").asText();
                    if (assigneeEmail != null && !assigneeEmail.isBlank()) {
                        assignee = employeeRepository.findByAccountEmail(assigneeEmail).orElse(null);
                    }
                }
                if (assignee != null && !membershipService.hasActiveTeamAccess(assignee.getId(), config.getTeam().getId())) {
                    assignee = null;
                }

                Optional<Task> existingTaskOpt = config.getProject() != null
                        ? taskRepository.findByExternalTicketRefAndSourceProviderAndProjectId(
                                externalTicketRef, IntegrationProvider.GITHUB, config.getProject().getId())
                        : taskRepository.findByExternalTicketRefAndSourceProvider(externalTicketRef, IntegrationProvider.GITHUB);

                Task task;
                if (existingTaskOpt.isPresent()) {
                    task = existingTaskOpt.get();
                } else {
                    task = new Task();
                    task.setExternalTicketRef(externalTicketRef);
                    task.setSourceProvider(IntegrationProvider.GITHUB);
                    task.setTeam(config.getTeam());
                    task.setProject(config.getProject());
                }

                task.setTitle(title);
                task.setDescription(body);
                task.setExternalUrl(htmlUrl);
                task.setAssignee(assignee);
                task.setTeam(config.getTeam());
                task.setProject(config.getProject());
                task.setPriority(mapPriority(issueNode.path("labels")));
                task.setEstimatedHours(estimateHours(title, body));

                // Map status
                if ("closed".equalsIgnoreCase(state)) {
                    task.setStatus(TaskStatus.DONE);
                } else {
                    if (task.getStatus() == null || task.getStatus() == TaskStatus.DONE) {
                        task.setStatus(TaskStatus.TODO);
                    }
                }

                TaskAssessmentService.GithubCommitMetrics metrics = assessLinkedCommits(
                        commitSignals, issueNumber, externalTicketRef);
                taskAssessmentService.assess(task, "GITHUB_SYNC", metrics);
                taskRepository.save(task);
                count++;
            }

            log.info("Successfully synced {} issues from GitHub repository: {}", count, projectKey);

        } catch (Exception e) {
            log.error("Failed to sync issues from GitHub for config: {}", config.getId(), e);
            throw new RuntimeException("GitHub Sync Failed: " + e.getMessage(), e);
        }
    }

    private List<GithubCommitSignal> fetchCommitSignals(WebClient webClient, String projectKey) {
        try {
            String commitsJson = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/" + projectKey + "/commits")
                            .queryParam("per_page", 100)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode commits = objectMapper.readTree(commitsJson);
            if (!commits.isArray()) return List.of();

            List<GithubCommitSignal> signals = new ArrayList<>();
            for (JsonNode commitNode : commits) {
                String sha = commitNode.path("sha").asText();
                String message = commitNode.path("commit").path("message").asText("");
                signals.add(fetchCommitDetail(webClient, projectKey, sha, message));
            }
            return signals;
        } catch (Exception e) {
            log.warn("Unable to load GitHub commit metrics for repository {}, continuing without commits", projectKey);
            return List.of();
        }
    }

    private GithubCommitSignal fetchCommitDetail(WebClient webClient, String projectKey, String sha, String message) {
        try {
            String detailJson = webClient.get()
                    .uri("/repos/" + projectKey + "/commits/" + sha)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            JsonNode detail = objectMapper.readTree(detailJson);
            int additions = detail.path("stats").path("additions").asInt(0);
            int deletions = detail.path("stats").path("deletions").asInt(0);
            int files = detail.path("files").isArray() ? detail.path("files").size() : 0;
            return new GithubCommitSignal(message, additions, deletions, files);
        } catch (Exception e) {
            return new GithubCommitSignal(message, 0, 0, 0);
        }
    }

    private TaskAssessmentService.GithubCommitMetrics assessLinkedCommits(
            List<GithubCommitSignal> commits,
            String issueNumber,
            String externalTicketRef
    ) {
        int count = 0;
        int additions = 0;
        int deletions = 0;
        int files = 0;
        String issueMarker = "#" + issueNumber;
        for (GithubCommitSignal commit : commits) {
            String message = commit.message().toUpperCase();
            if (message.contains(issueMarker.toUpperCase()) || message.contains(externalTicketRef.toUpperCase())) {
                count++;
                additions += commit.additions();
                deletions += commit.deletions();
                files += commit.changedFiles();
            }
        }
        return taskAssessmentService.assessGithubCommits(count, additions, deletions, files);
    }

    private TaskPriority mapPriority(JsonNode labels) {
        if (labels == null || !labels.isArray()) return TaskPriority.MEDIUM;
        for (JsonNode label : labels) {
            String name = label.path("name").asText("").toLowerCase();
            if (name.contains("critical") || name.contains("blocker")) return TaskPriority.CRITICAL;
            if (name.contains("high")) return TaskPriority.HIGH;
            if (name.contains("low")) return TaskPriority.LOW;
        }
        return TaskPriority.MEDIUM;
    }

    private int estimateHours(String title, String body) {
        int length = (title == null ? 0 : title.length()) + (body == null ? 0 : body.length());
        return Math.max(1, Math.min(40, 2 + (length / 400)));
    }

    private record GithubCommitSignal(String message, int additions, int deletions, int changedFiles) {
    }
}
