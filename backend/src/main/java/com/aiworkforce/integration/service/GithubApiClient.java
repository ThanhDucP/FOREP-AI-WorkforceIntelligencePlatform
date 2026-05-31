package com.aiworkforce.integration.service;

import com.aiworkforce.core.enums.IntegrationProvider;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.integration.entity.TaskIntegrationConfig;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.repository.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubApiClient {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;

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

                Optional<Task> existingTaskOpt = taskRepository.findByExternalTicketRefAndSourceProvider(
                        externalTicketRef, IntegrationProvider.GITHUB);

                Task task;
                if (existingTaskOpt.isPresent()) {
                    task = existingTaskOpt.get();
                } else {
                    task = new Task();
                    task.setExternalTicketRef(externalTicketRef);
                    task.setSourceProvider(IntegrationProvider.GITHUB);
                    task.setTeam(config.getTeam());
                }

                task.setTitle(title);
                task.setDescription(body);
                task.setExternalUrl(htmlUrl);
                task.setAssignee(assignee);

                // Map status
                if ("closed".equalsIgnoreCase(state)) {
                    task.setStatus(TaskStatus.DONE);
                } else {
                    if (task.getStatus() == null || task.getStatus() == TaskStatus.DONE) {
                        task.setStatus(TaskStatus.TODO);
                    }
                }

                taskRepository.save(task);
                count++;
            }

            log.info("Successfully synced {} issues from GitHub repository: {}", count, projectKey);

        } catch (Exception e) {
            log.error("Failed to sync issues from GitHub for config: {}", config.getId(), e);
            throw new RuntimeException("GitHub Sync Failed: " + e.getMessage(), e);
        }
    }
}
