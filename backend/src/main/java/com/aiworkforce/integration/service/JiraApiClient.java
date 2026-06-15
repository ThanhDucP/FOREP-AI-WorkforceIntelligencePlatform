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
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class JiraApiClient {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;
    private final TaskAssessmentService taskAssessmentService;
    private final TeamMembershipService membershipService;

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
                    .defaultHeader("Authorization", buildAuthorizationHeader(accessToken))
                    .defaultHeader("Accept", "application/json")
                    .build();

            String finalProjectKey = projectKey;
            String responseJson = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/rest/api/3/search/jql")
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
                if (assignee != null && !membershipService.hasActiveTeamAccess(assignee.getId(), config.getTeam().getId())) {
                    assignee = null;
                }

                Optional<Task> existingTaskOpt = config.getProject() != null
                        ? taskRepository.findByExternalTicketRefAndSourceProviderAndProjectId(
                                issueKey, IntegrationProvider.JIRA, config.getProject().getId())
                        : taskRepository.findByExternalTicketRefAndSourceProvider(issueKey, IntegrationProvider.JIRA);

                Task task;
                if (existingTaskOpt.isPresent()) {
                    task = existingTaskOpt.get();
                } else {
                    task = new Task();
                    task.setExternalTicketRef(issueKey);
                    task.setSourceProvider(IntegrationProvider.JIRA);
                    task.setTeam(config.getTeam());
                    task.setProject(config.getProject());
                }

                task.setTitle(summary);
                task.setDescription(description);
                task.setExternalUrl(externalUrl);
                task.setAssignee(assignee);
                task.setTeam(config.getTeam());
                task.setProject(config.getProject());
                task.setPriority(mapPriority(fields.path("priority").path("name").asText()));
                task.setDueDate(parseDueDate(fields.path("duedate").asText(null)));
                task.setEstimatedHours(estimateHours(fields));
                task.setStoryPoints(extractStoryPoints(fields));

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

                taskAssessmentService.assess(task, "JIRA_SYNC");
                taskRepository.save(task);
                count++;
            }

            log.info("Successfully synced {} issues from Jira project: {}", count, projectKey);

        } catch (Exception e) {
            log.error("Failed to sync issues from Jira for config: {}", config.getId(), e);
            throw new RuntimeException("Jira Sync Failed: " + e.getMessage(), e);
        }
    }

    private String buildAuthorizationHeader(String accessToken) {
        if (accessToken.contains(":")) {
            String encoded = Base64.getEncoder()
                    .encodeToString(accessToken.getBytes(StandardCharsets.UTF_8));
            return "Basic " + encoded;
        }
        return "Bearer " + accessToken;
    }

    private TaskPriority mapPriority(String priorityName) {
        if (priorityName == null) return TaskPriority.MEDIUM;
        String normalized = priorityName.toUpperCase();
        if (normalized.contains("HIGHEST") || normalized.contains("CRITICAL") || normalized.contains("BLOCKER")) {
            return TaskPriority.CRITICAL;
        }
        if (normalized.contains("HIGH")) return TaskPriority.HIGH;
        if (normalized.contains("LOW")) return TaskPriority.LOW;
        return TaskPriority.MEDIUM;
    }

    private java.time.LocalDateTime parseDueDate(String dueDate) {
        if (dueDate == null || dueDate.isBlank()) return null;
        try {
            return LocalDate.parse(dueDate).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private int estimateHours(JsonNode fields) {
        int seconds = fields.path("timeoriginalestimate").asInt(0);
        if (seconds == 0) {
            seconds = fields.path("timeestimate").asInt(0);
        }
        return seconds > 0 ? Math.max(1, (int) Math.ceil(seconds / 3600.0)) : 0;
    }

    private Integer extractStoryPoints(JsonNode fields) {
        JsonNode storyPoints = fields.path("customfield_10016");
        if (storyPoints.isNumber()) {
            return storyPoints.asInt();
        }
        return null;
    }
}
