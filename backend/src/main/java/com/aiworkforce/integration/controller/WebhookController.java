package com.aiworkforce.integration.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.integration.entity.TaskIntegrationConfig;
import com.aiworkforce.integration.service.GithubWebhookProcessor;
import com.aiworkforce.integration.service.JiraWebhookProcessor;
import com.aiworkforce.integration.service.TaskIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final TaskIntegrationService integrationService;
    private final GithubWebhookProcessor githubProcessor;
    private final JiraWebhookProcessor jiraProcessor;

    @PostMapping("/github/{configId}")
    public ResponseEntity<ApiResponse<String>> handleGithubWebhook(
            @PathVariable UUID configId,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String payload) {
        
        log.info("Received GitHub webhook for config: {}", configId);
        
        TaskIntegrationConfig config = integrationService.getActiveConfigById(configId);
        
        githubProcessor.processPayload(payload, signature, config);
        
        return ResponseEntity.ok(ApiResponse.success("Webhook processed successfully"));
    }

    @PostMapping("/jira/{configId}")
    public ResponseEntity<ApiResponse<String>> handleJiraWebhook(
            @PathVariable UUID configId,
            @RequestBody String payload) {
        
        log.info("Received Jira webhook for config: {}", configId);
        
        TaskIntegrationConfig config = integrationService.getActiveConfigById(configId);
        
        jiraProcessor.processPayload(payload, null, config);
        
        return ResponseEntity.ok(ApiResponse.success("Webhook processed successfully"));
    }
}
