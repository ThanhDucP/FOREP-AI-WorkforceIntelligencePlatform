package com.aiworkforce.integration.service;

import com.aiworkforce.core.enums.IntegrationProvider;
import com.aiworkforce.core.enums.TaskPriority;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.integration.entity.TaskIntegrationConfig;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.repository.TaskRepository;
import com.aiworkforce.task.service.TaskAssessmentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class JiraWebhookProcessor implements WebhookProcessorStrategy {

    private final ObjectMapper objectMapper;
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskAssessmentService taskAssessmentService;

    @Override
    @Transactional
    public void processPayload(String payload, String signature, TaskIntegrationConfig config) {
        // In Jira, signatures can be checked similarly if they use Webhook secrets.
        // For simplicity in Phase 1, we will bypass strict signature verification or 
        // implement basic token check depending on how Jira is configured.
        
        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            
            String webhookEvent = rootNode.path("webhookEvent").asText();
            if (!webhookEvent.startsWith("jira:issue_")) {
                log.info("Not an issue event, ignoring payload");
                return;
            }

            JsonNode issueNode = rootNode.path("issue");
            String issueKey = issueNode.path("key").asText(); // e.g., PROJ-123
            
            JsonNode fields = issueNode.path("fields");
            String summary = fields.path("summary").asText();
            String description = fields.path("description").asText();
            
            // Build the URL (Jira payload doesn't always have a direct html_url like Github, 
            // but we can construct it if we know the domain, or it might be in self URI)
            String selfUri = issueNode.path("self").asText();
            String externalUrl = "";
            if (selfUri != null && selfUri.contains("/rest/api/")) {
                externalUrl = selfUri.substring(0, selfUri.indexOf("/rest/api/")) + "/browse/" + issueKey;
            }

            // Find assignee by email
            Employee assignee = null;
            JsonNode assigneeNode = fields.path("assignee");
            if (!assigneeNode.isMissingNode() && !assigneeNode.isNull()) {
                String emailAddress = assigneeNode.path("emailAddress").asText();
                if (emailAddress != null && !emailAddress.isBlank()) {
                    assignee = employeeRepository.findByAccountEmail(emailAddress).orElse(null);
                }
            }

            Optional<Task> existingTaskOpt = config.getProject() != null
                    ? taskRepository.findByExternalTicketRefAndSourceProviderAndProjectId(
                            issueKey, IntegrationProvider.JIRA, config.getProject().getId())
                    : taskRepository.findByExternalTicketRefAndSourceProvider(issueKey, IntegrationProvider.JIRA);

            Task task;
            if (existingTaskOpt.isPresent()) {
                task = existingTaskOpt.get();
                log.info("Updating existing Jira task: {}", issueKey);
            } else {
                task = new Task();
                task.setExternalTicketRef(issueKey);
                task.setSourceProvider(IntegrationProvider.JIRA);
                task.setTeam(config.getTeam());
                task.setProject(config.getProject());
                log.info("Creating new Jira task: {}", issueKey);
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
            
            // Status mapping
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

            taskAssessmentService.assess(task, "JIRA_WEBHOOK");
            taskRepository.save(task);

        } catch (Exception e) {
            log.error("Error processing Jira webhook payload", e);
            throw new RuntimeException("Error processing payload", e);
        }
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
