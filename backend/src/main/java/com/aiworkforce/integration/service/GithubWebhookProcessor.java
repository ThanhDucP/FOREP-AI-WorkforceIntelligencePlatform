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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubWebhookProcessor implements WebhookProcessorStrategy {

    private final ObjectMapper objectMapper;
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskAssessmentService taskAssessmentService;

    @Override
    @Transactional
    public void processPayload(String payload, String signature, TaskIntegrationConfig config) {
        if (!verifySignature(payload, signature, config.getWebhookSecret())) {
            log.warn("Invalid GitHub webhook signature for config: {}", config.getId());
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            
            // Check if it's an issue event
            if (!rootNode.has("issue")) {
                log.info("Not an issue event, ignoring payload");
                return;
            }

            String action = rootNode.path("action").asText();
            JsonNode issueNode = rootNode.path("issue");
            
            String issueNumber = issueNode.path("number").asText();
            String externalTicketRef = "GH-" + issueNumber;
            String title = issueNode.path("title").asText();
            String body = issueNode.path("body").asText();
            String htmlUrl = issueNode.path("html_url").asText();
            String state = issueNode.path("state").asText();
            
            // Find assignee (try to match by GitHub email if present, or assign to unassigned)
            Employee assignee = null;
            JsonNode assigneeNode = issueNode.path("assignee");
            if (!assigneeNode.isMissingNode() && !assigneeNode.isNull()) {
                String assigneeLogin = assigneeNode.path("login").asText();
                // In a real scenario, we might have a mapping of Github username to Employee. 
                // For now, we try to match by email if github provides it, or just leave unassigned.
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
                log.info("Updating existing GitHub task: {}", externalTicketRef);
            } else {
                task = new Task();
                task.setExternalTicketRef(externalTicketRef);
                task.setSourceProvider(IntegrationProvider.GITHUB);
                task.setTeam(config.getTeam());
                log.info("Creating new GitHub task: {}", externalTicketRef);
            }

            task.setTitle(title);
            task.setDescription(body);
            task.setExternalUrl(htmlUrl);
            task.setAssignee(assignee);
            task.setPriority(mapPriority(issueNode.path("labels")));
            task.setEstimatedHours(estimateHours(title, body));
            
            // Map status
            if ("closed".equalsIgnoreCase(state)) {
                task.setStatus(TaskStatus.DONE);
            } else {
                if (task.getStatus() == null || task.getStatus() == TaskStatus.DONE) {
                    task.setStatus(TaskStatus.TODO); // Default when opened/reopened
                }
            }

            taskAssessmentService.assess(task, "GITHUB_WEBHOOK");
            taskRepository.save(task);

        } catch (Exception e) {
            log.error("Error processing GitHub webhook payload", e);
            throw new RuntimeException("Error processing payload", e);
        }
    }

    private boolean verifySignature(String payload, String signatureHeader, String secret) {
        if (signatureHeader == null || signatureHeader.isBlank()) return false;
        
        try {
            String expectedSignature = "sha256=" + calculateHmac(payload, secret);
            return signatureHeader.equals(expectedSignature);
        } catch (Exception e) {
            log.error("Error verifying signature", e);
            return false;
        }
    }

    private String calculateHmac(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
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
}
