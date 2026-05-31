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
public class JiraApiClient {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;

    // Field for testing
    private String jiraApiUrlOverride = null;

    @Transactional
    public void syncIssues(TaskIntegrationConfig config) {
        String projectKeyRaw = config.getProjectKey(); // e.g. "domain.atlassian.net/PROJ" or "PROJ"
        String accessToken = config.getAccessToken();

        if (projectKeyRaw == null || projectKeyRaw.isBlank()) {
            log.warn("Project key is blank, skipping Jira sync for config: {}", config.getId());
            return;
        }

        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Access token is blank, skipping Jira sync for config: {}", config.getId());
            return;
        }

        String domain = "your-domain.atlassian.net";
        String projectKey = projectKeyRaw;

        if (projectKeyRaw.contains("/")) {
            int slashIndex = projectKeyRaw.indexOf("/");
            domain = projectKeyRaw.substring(0, slashIndex);
            projectKey = projectKeyRaw.substring(slashIndex + 1);
        }

        log.info("Starting active Jira sync for project: {} on domain: {}", projectKey, domain);

        try {
            String baseUrl = (jiraApiUrlOverride != null) ? jiraApiUrlOverride : "https://" + domain;
            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + accessToken)
                    .defaultHeader("Accept", "application/json")
                    .build();

            String finalProjectKey = projectKey;
            String responseJson = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/rest/api/2/search")
                            .queryParam("jql", "project=" + finalProjectKey)
                            .queryParam("maxResults", 100)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseJson == null) {
                log.warn("No issues found or empty response from Jira API");
                return;
            }

            JsonNode rootNode = objectMapper.readTree(responseJson);
            JsonNode issuesNode = rootNode.path("issues");
            if (!issuesNode.isArray()) {
                log.warn("Expected JSON array in issues field from Jira API, but got: {}", responseJson);
                return;
            }

            int count = 0;
            for (JsonNode issueNode : issuesNode) {
                String issueKey = issueNode.path("key").asText();
                JsonNode fields = issueNode.path("fields");
                String summary = fields.path("summary").asText();
                String description = fields.path("description").asText();

                String selfUri = issueNode.path("self").asText();
                String externalUrl = "";
                if (selfUri != null && selfUri.contains("/rest/api/")) {
                    externalUrl = selfUri.substring(0, selfUri.indexOf("/rest/api/")) + "/browse/" + issueKey;
                }

                Employee assignee = null;
                JsonNode assigneeNode = fields.path("assignee");
                if (!assigneeNode.isMissingNode() && !assigneeNode.isNull()) {
                    String emailAddress = assigneeNode.path("emailAddress").asText();
                    if (emailAddress != null && !emailAddress.isBlank()) {
                        assignee = employeeRepository.findByAccountEmail(emailAddress).orElse(null);
                    }
                }

                Optional<Task> existingTaskOpt = taskRepository.findByExternalTicketRefAndSourceProvider(
                        issueKey, IntegrationProvider.JIRA);

                Task task;
                if (existingTaskOpt.isPresent()) {
                    task = existingTaskOpt.get();
                } else {
                    task = new Task();
                    task.setExternalTicketRef(issueKey);
                    task.setSourceProvider(IntegrationProvider.JIRA);
                    task.setTeam(config.getTeam());
                }

                task.setTitle(summary);
                task.setDescription(description);
                task.setExternalUrl(externalUrl);
                task.setAssignee(assignee);

                // Map status
                JsonNode statusNode = fields.path("status");
                if (!statusNode.isMissingNode() && !statusNode.isNull()) {
                    String statusName = statusNode.path("name").asText().toUpperCase();
                    if (statusName.contains("DONE") || statusName.contains("RESOLVED") || statusName.contains("CLOSED")) {
                        task.setStatus(TaskStatus.DONE);
                    } else if (statusName.contains("IN PROGRESS")) {
                        task.setStatus(TaskStatus.IN_PROGRESS);
                    } else if (statusName.contains("REVIEW")) {
                        task.setStatus(TaskStatus.REVIEW);
                    } else {
                        task.setStatus(TaskStatus.TODO);
                    }
                }

                taskRepository.save(task);
                count++;
            }

            log.info("Successfully synced {} issues from Jira project: {}", count, projectKey);

        } catch (Exception e) {
            log.error("Failed to sync issues from Jira for config: {}", config.getId(), e);
            throw new RuntimeException("Jira Sync Failed: " + e.getMessage(), e);
        }
    }
}
