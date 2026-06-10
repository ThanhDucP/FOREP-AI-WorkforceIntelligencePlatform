package com.aiworkforce.integration.service;

import com.aiworkforce.core.enums.IntegrationProvider;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.integration.dto.TaskIntegrationConfigRequest;
import com.aiworkforce.integration.dto.IntegrationConnectRequest;
import com.aiworkforce.integration.dto.IntegrationConnectResponse;
import com.aiworkforce.integration.dto.TaskIntegrationConfigResponse;
import com.aiworkforce.integration.entity.TaskIntegrationConfig;
import com.aiworkforce.integration.repository.TaskIntegrationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskIntegrationService {

    private final TaskIntegrationConfigRepository configRepository;
    private final TeamRepository teamRepository;
    private final GithubApiClient githubApiClient;
    private final JiraApiClient jiraApiClient;
    private final GithubWebhookRegistrar githubWebhookRegistrar;

    @Transactional
    public void syncTasks(UUID configId) {
        TaskIntegrationConfig config = getActiveConfigById(configId);
        syncConfig(config);
    }

    @Transactional
    public IntegrationConnectResponse connectWithKey(IntegrationConnectRequest request) {
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        TaskIntegrationConfig config = new TaskIntegrationConfig();
        config.setTeam(team);
        config.setProvider(request.getProvider());
        // generate webhook secret
        byte[] randomBytes = new byte[24];
        new SecureRandom().nextBytes(randomBytes);
        String webhookSecret = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        config.setWebhookSecret(webhookSecret);
        config.setAccessToken(request.getConnectionKey());
        config.setProjectKey(request.getProjectKey());
        config.setIsActive(request.getProvider() == IntegrationProvider.JIRA);

        TaskIntegrationConfig saved = configRepository.save(config);

        IntegrationConnectResponse resp = new IntegrationConnectResponse();
        resp.setConfigId(saved.getId().toString());
        resp.setWebhookSecret(webhookSecret);

        if (request.getProvider() == IntegrationProvider.GITHUB) {
            String ownerRepo = request.getProjectKey();
            String payloadUrl = String.format("%s/api/v1/webhooks/github/%s", getAppBaseUrl(), saved.getId().toString());
            resp.setWebhookUrl(payloadUrl);

            GithubWebhookRegistrar.RegistrationResult res = githubWebhookRegistrar.createRepoWebhook(
                    ownerRepo, request.getConnectionKey(), payloadUrl, webhookSecret
            );

            if (res.registered) {
                saved.setIsActive(true);
                if (res.webhookId != null) saved.setProviderWebhookId(res.webhookId);
                saved = configRepository.save(saved);
                githubApiClient.syncIssues(saved);
                resp.setWebhookRegistered(true);
                resp.setMessage("Webhook registered automatically and GitHub tasks synced");
            } else {
                resp.setWebhookRegistered(false);
                resp.setMessage("Auto-registration failed: " + res.message);
            }
        } else if (request.getProvider() == IntegrationProvider.JIRA) {
            jiraApiClient.syncIssues(saved);
            resp.setWebhookRegistered(true);
            resp.setMessage("Jira connected and tasks synced");
        } else {
            resp.setWebhookRegistered(false);
            resp.setMessage("Unsupported provider: " + request.getProvider());
        }

        return resp;
    }

    // Helper to derive app base url from env or default; production should set APP_BASE_URL
    private String getAppBaseUrl() {
        String url = System.getenv("APP_BASE_URL");
        if (url != null && !url.isBlank()) return url.replaceAll("/+$", "");
        return "https://localhost:8080";
    }

    @Transactional
    public TaskIntegrationConfigResponse createConfig(TaskIntegrationConfigRequest request) {
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        TaskIntegrationConfig config = new TaskIntegrationConfig();
        config.setTeam(team);
        config.setProvider(request.getProvider());
        config.setWebhookSecret(request.getWebhookSecret());
        config.setAccessToken(request.getAccessToken());
        config.setProjectKey(request.getProjectKey());
        config.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        TaskIntegrationConfig saved = configRepository.save(config);
        if (Boolean.TRUE.equals(saved.getIsActive())) {
            syncConfig(saved);
        }
        return mapToResponse(saved);
    }

    public List<TaskIntegrationConfigResponse> getConfigsByTeam(UUID teamId) {
        return configRepository.findByTeamId(teamId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public TaskIntegrationConfig getConfigById(UUID id) {
        return configRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Integration config not found"));
    }
    
    public TaskIntegrationConfig getActiveConfigById(UUID id) {
        return configRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Active integration config not found"));
    }

    @Transactional
    public TaskIntegrationConfigResponse updateConfig(UUID id, TaskIntegrationConfigRequest request) {
        TaskIntegrationConfig config = getConfigById(id);
        
        if (request.getWebhookSecret() != null && !request.getWebhookSecret().isBlank()) {
            config.setWebhookSecret(request.getWebhookSecret());
        }
        if (request.getAccessToken() != null) {
            config.setAccessToken(request.getAccessToken());
        }
        if (request.getProjectKey() != null) {
            config.setProjectKey(request.getProjectKey());
        }
        if (request.getIsActive() != null) {
            config.setIsActive(request.getIsActive());
        }
        
        return mapToResponse(configRepository.save(config));
    }

    @Transactional
    public void deleteConfig(UUID id) {
        TaskIntegrationConfig config = getConfigById(id);
        configRepository.delete(config);
    }

    private TaskIntegrationConfigResponse mapToResponse(TaskIntegrationConfig config) {
        return TaskIntegrationConfigResponse.builder()
                .id(config.getId())
                .teamId(config.getTeam().getId())
                .provider(config.getProvider())
                .isActive(config.getIsActive())
                .projectKey(config.getProjectKey())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private void syncConfig(TaskIntegrationConfig config) {
        if (config.getProvider() == IntegrationProvider.GITHUB) {
            githubApiClient.syncIssues(config);
        } else if (config.getProvider() == IntegrationProvider.JIRA) {
            jiraApiClient.syncIssues(config);
        } else {
            throw new IllegalArgumentException("Unsupported sync provider: " + config.getProvider());
        }
    }
}
