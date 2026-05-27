package com.aiworkforce.integration.service;

import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.integration.dto.TaskIntegrationConfigRequest;
import com.aiworkforce.integration.dto.TaskIntegrationConfigResponse;
import com.aiworkforce.integration.entity.TaskIntegrationConfig;
import com.aiworkforce.integration.repository.TaskIntegrationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskIntegrationService {

    private final TaskIntegrationConfigRepository configRepository;
    private final TeamRepository teamRepository;

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

        return mapToResponse(configRepository.save(config));
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
}
