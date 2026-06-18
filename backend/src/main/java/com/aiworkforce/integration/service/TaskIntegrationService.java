package com.aiworkforce.integration.service;

import com.aiworkforce.core.enums.IntegrationProvider;
import com.aiworkforce.core.enums.IntegrationSyncStatus;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.core.security.AccessPolicyService;
import com.aiworkforce.identity.entity.Project;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.ProjectRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.integration.dto.TaskIntegrationConfigRequest;
import com.aiworkforce.integration.dto.IntegrationConnectRequest;
import com.aiworkforce.integration.dto.IntegrationConnectResponse;
import com.aiworkforce.integration.dto.IntegrationRuntimeStatusResponse;
import com.aiworkforce.integration.dto.IntegrationSyncLogResponse;
import com.aiworkforce.integration.dto.TaskIntegrationConfigResponse;
import com.aiworkforce.integration.entity.IntegrationSyncLog;
import com.aiworkforce.integration.entity.TaskIntegrationConfig;
import com.aiworkforce.integration.repository.IntegrationSyncLogRepository;
import com.aiworkforce.integration.repository.TaskIntegrationConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskIntegrationService {

    private final TaskIntegrationConfigRepository configRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final IntegrationSyncLogRepository syncLogRepository;
    private final AccessPolicyService accessPolicyService;
    private final GithubApiClient githubApiClient;
    private final JiraApiClient jiraApiClient;
    private final GithubWebhookRegistrar githubWebhookRegistrar;

    @Value("${integration.sync.enabled:true}")
    private boolean scheduledSyncEnabled;

    @Value("${integration.sync.success-interval-minutes:30}")
    private long scheduledSuccessIntervalMinutes;

    @Value("${integration.sync.failure-retry-delay-minutes:10}")
    private long scheduledFailureRetryDelayMinutes;

    @Transactional
    public void syncTasks(UUID configId) {
        TaskIntegrationConfig config = getActiveConfigById(configId);
        accessPolicyService.ensureIntegrationAccess(config);
        syncConfig(config);
    }

    @Transactional
    public IntegrationConnectResponse connectWithKey(IntegrationConnectRequest request) {
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        Project project = resolveProject(request.getProjectId(), team, request.getProjectKey(), request.getJiraDomain(), request.getProvider());
        accessPolicyService.ensureTeamManage(team);

        TaskIntegrationConfig config = new TaskIntegrationConfig();
        config.setTeam(team);
        config.setProject(project);
        config.setProvider(request.getProvider());
        // generate webhook secret
        byte[] randomBytes = new byte[24];
        new SecureRandom().nextBytes(randomBytes);
        String webhookSecret = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        config.setWebhookSecret(webhookSecret);
        config.setAccessToken(request.getConnectionKey());
        config.setProjectKey(resolveProviderProjectKey(project, request.getProvider(), request.getProjectKey()));
        config.setIsActive(request.getProvider() == IntegrationProvider.JIRA);

        TaskIntegrationConfig saved = configRepository.save(config);

        IntegrationConnectResponse resp = new IntegrationConnectResponse();
        resp.setConfigId(saved.getId().toString());
        resp.setWebhookSecret(webhookSecret);

        if (request.getProvider() == IntegrationProvider.GITHUB) {
            String ownerRepo = config.getProjectKey();
            String payloadUrl = String.format("%s/api/v1/webhooks/github/%s", getAppBaseUrl(), saved.getId().toString());
            resp.setWebhookUrl(payloadUrl);

            GithubWebhookRegistrar.RegistrationResult res = githubWebhookRegistrar.createRepoWebhook(
                    ownerRepo, request.getConnectionKey(), payloadUrl, webhookSecret
            );

            if (res.registered) {
                saved.setIsActive(true);
                if (res.webhookId != null) saved.setProviderWebhookId(res.webhookId);
                saved = configRepository.save(saved);
                syncConfig(saved);
                resp.setWebhookRegistered(true);
                resp.setMessage("Webhook registered automatically and GitHub tasks synced");
            } else {
                resp.setWebhookRegistered(false);
                resp.setMessage("Auto-registration failed: " + res.message);
            }
        } else if (request.getProvider() == IntegrationProvider.JIRA) {
            syncConfig(saved);
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
        Project project = resolveProject(request.getProjectId(), team, request.getProjectKey(), request.getJiraDomain(), request.getProvider());
        accessPolicyService.ensureTeamManage(team);

        TaskIntegrationConfig config = new TaskIntegrationConfig();
        config.setTeam(team);
        config.setProject(project);
        config.setProvider(request.getProvider());
        config.setWebhookSecret(request.getWebhookSecret());
        config.setAccessToken(request.getAccessToken());
        config.setProjectKey(resolveProviderProjectKey(project, request.getProvider(), request.getProjectKey()));
        config.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        TaskIntegrationConfig saved = configRepository.save(config);
        if (Boolean.TRUE.equals(saved.getIsActive())) {
            syncConfig(saved);
        }
        return mapToResponse(saved);
    }

    public List<TaskIntegrationConfigResponse> getConfigsByTeam(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        accessPolicyService.ensureTeamAccess(team);
        return configRepository.findByTeamId(teamId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<IntegrationSyncLogResponse> getSyncLogs(UUID configId) {
        TaskIntegrationConfig config = getConfigById(configId);
        accessPolicyService.ensureIntegrationAccess(config);
        return syncLogRepository.findTop20ByConfigIdOrderByStartedAtDesc(configId).stream()
                .map(this::mapSyncLogToResponse)
                .toList();
    }

    public IntegrationRuntimeStatusResponse getRuntimeStatus() {
        List<TaskIntegrationConfig> activeConfigs = configRepository.findByIsActiveTrue();
        long failedConfigCount = activeConfigs.stream()
                .filter(config -> config.getLastSyncStatus() == IntegrationSyncStatus.FAILED)
                .count();
        LocalDateTime latestSyncAt = activeConfigs.stream()
                .map(TaskIntegrationConfig::getLastSyncAt)
                .filter(syncAt -> syncAt != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return IntegrationRuntimeStatusResponse.builder()
                .scheduledSyncEnabled(scheduledSyncEnabled)
                .successIntervalMinutes(scheduledSuccessIntervalMinutes)
                .failureRetryDelayMinutes(scheduledFailureRetryDelayMinutes)
                .activeConfigCount(activeConfigs.size())
                .failedConfigCount(failedConfigCount)
                .latestSyncAt(latestSyncAt)
                .build();
    }

    @Scheduled(cron = "${integration.sync.cron:0 */15 * * * *}")
    @Transactional
    public void syncActiveConfigsOnSchedule() {
        if (!scheduledSyncEnabled) {
            return;
        }

        List<TaskIntegrationConfig> activeConfigs = configRepository.findByIsActiveTrue();
        for (TaskIntegrationConfig config : activeConfigs) {
            if (!shouldRunScheduledSync(config)) {
                continue;
            }

            try {
                syncConfig(config);
            } catch (RuntimeException ex) {
                log.warn("Scheduled sync failed for integration config {}: {}", config.getId(), ex.getMessage());
            }
        }
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
        accessPolicyService.ensureIntegrationManage(config);
        
        if (request.getWebhookSecret() != null && !request.getWebhookSecret().isBlank()) {
            config.setWebhookSecret(request.getWebhookSecret());
        }
        if (request.getAccessToken() != null) {
            config.setAccessToken(request.getAccessToken());
        }
        if (request.getProjectKey() != null) {
            config.setProjectKey(resolveProviderProjectKey(config.getProject(), config.getProvider(), request.getProjectKey()));
        }
        if (request.getIsActive() != null) {
            config.setIsActive(request.getIsActive());
        }
        
        return mapToResponse(configRepository.save(config));
    }

    @Transactional
    public void deleteConfig(UUID id) {
        TaskIntegrationConfig config = getConfigById(id);
        accessPolicyService.ensureIntegrationManage(config);
        configRepository.delete(config);
    }

    private TaskIntegrationConfigResponse mapToResponse(TaskIntegrationConfig config) {
        return TaskIntegrationConfigResponse.builder()
                .id(config.getId())
                .teamId(config.getTeam().getId())
                .projectId(config.getProject() != null ? config.getProject().getId() : null)
                .projectName(config.getProject() != null ? config.getProject().getName() : null)
                .provider(config.getProvider())
                .isActive(config.getIsActive())
                .projectKey(config.getProjectKey())
                .lastSyncAt(config.getLastSyncAt())
                .lastSyncStatus(config.getLastSyncStatus())
                .lastSyncError(config.getLastSyncError())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private void syncConfig(TaskIntegrationConfig config) {
        config = ensureProjectResolved(config);

        IntegrationSyncLog syncLog = new IntegrationSyncLog();
        syncLog.setConfig(config);
        syncLog.setProvider(config.getProvider());
        syncLog.setStatus(IntegrationSyncStatus.STARTED);
        syncLog.setStartedAt(LocalDateTime.now());
        syncLog.setMessage("Sync started");
        syncLog = syncLogRepository.save(syncLog);

        try {
            if (config.getProvider() == IntegrationProvider.GITHUB) {
                githubApiClient.syncIssues(config);
            } else if (config.getProvider() == IntegrationProvider.JIRA) {
                jiraApiClient.syncIssues(config);
            } else {
                throw new IllegalArgumentException("Unsupported sync provider: " + config.getProvider());
            }

            LocalDateTime finishedAt = LocalDateTime.now();
            syncLog.setStatus(IntegrationSyncStatus.SUCCESS);
            syncLog.setFinishedAt(finishedAt);
            syncLog.setMessage("Sync completed successfully");
            syncLogRepository.save(syncLog);

            config.setLastSyncAt(finishedAt);
            config.setLastSyncStatus(IntegrationSyncStatus.SUCCESS);
            config.setLastSyncError(null);
            configRepository.save(config);
        } catch (RuntimeException ex) {
            LocalDateTime finishedAt = LocalDateTime.now();
            syncLog.setStatus(IntegrationSyncStatus.FAILED);
            syncLog.setFinishedAt(finishedAt);
            syncLog.setMessage(ex.getMessage());
            syncLogRepository.save(syncLog);

            config.setLastSyncAt(finishedAt);
            config.setLastSyncStatus(IntegrationSyncStatus.FAILED);
            config.setLastSyncError(ex.getMessage());
            configRepository.save(config);
            throw ex;
        }
    }

    private IntegrationSyncLogResponse mapSyncLogToResponse(IntegrationSyncLog syncLog) {
        return IntegrationSyncLogResponse.builder()
                .id(syncLog.getId())
                .configId(syncLog.getConfig().getId())
                .provider(syncLog.getProvider())
                .status(syncLog.getStatus())
                .startedAt(syncLog.getStartedAt())
                .finishedAt(syncLog.getFinishedAt())
                .message(syncLog.getMessage())
                .build();
    }

    private boolean shouldRunScheduledSync(TaskIntegrationConfig config) {
        if (config.getLastSyncAt() == null || config.getLastSyncStatus() == null) {
            return true;
        }

        LocalDateTime nextRunAt = config.getLastSyncStatus() == IntegrationSyncStatus.FAILED
                ? config.getLastSyncAt().plusMinutes(scheduledFailureRetryDelayMinutes)
                : config.getLastSyncAt().plusMinutes(scheduledSuccessIntervalMinutes);
        return !LocalDateTime.now().isBefore(nextRunAt);
    }

    private TaskIntegrationConfig ensureProjectResolved(TaskIntegrationConfig config) {
        if (config.getProject() != null) {
            return config;
        }

        String projectKey = config.getProjectKey();
        String jiraDomain = null;
        String providerProjectKey = projectKey;
        if (config.getProvider() == IntegrationProvider.JIRA && projectKey != null && projectKey.contains("/")) {
            int slashIndex = projectKey.indexOf("/");
            jiraDomain = projectKey.substring(0, slashIndex);
            providerProjectKey = projectKey.substring(slashIndex + 1);
        }

        Project project = resolveProject(null, config.getTeam(), providerProjectKey, jiraDomain, config.getProvider());
        config.setProject(project);
        return configRepository.save(config);
    }

    private Project resolveProject(UUID projectId, Team team, String providerProjectKey, String jiraDomain, IntegrationProvider provider) {
        if (projectId != null) {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
            if (!project.getTeam().getId().equals(team.getId())) {
                throw new BusinessException("Project does not belong to the requested team");
            }
            if (!project.isActive()) {
                throw new BusinessException("Project is inactive");
            }
            return project;
        }

        if (provider == IntegrationProvider.GITHUB && providerProjectKey != null && !providerProjectKey.isBlank()) {
            return projectRepository.findByGithubRepositoryIgnoreCase(providerProjectKey)
                    .orElseThrow(() -> new ResourceNotFoundException("GitHub repository is not registered as a project"));
        }

        if (provider == IntegrationProvider.JIRA && providerProjectKey != null && !providerProjectKey.isBlank()) {
            String normalizedDomain = normalizeJiraDomain(jiraDomain);
            if (normalizedDomain != null) {
                return projectRepository.findByJiraDomainIgnoreCaseAndJiraProjectKeyIgnoreCase(normalizedDomain, providerProjectKey)
                        .orElseThrow(() -> new ResourceNotFoundException("Jira project is not registered as a project"));
            }
        }

        throw new BusinessException("A projectId or registered provider project key is required");
    }

    private String resolveProviderProjectKey(Project project, IntegrationProvider provider, String fallback) {
        if (provider == IntegrationProvider.GITHUB && project != null && project.getGithubRepository() != null) {
            return project.getGithubRepository();
        }
        if (provider == IntegrationProvider.JIRA && project != null && project.getJiraProjectKey() != null) {
            if (project.getJiraDomain() != null && !project.getJiraDomain().isBlank()) {
                return project.getJiraDomain() + "/" + project.getJiraProjectKey();
            }
            return project.getJiraProjectKey();
        }
        return fallback;
    }

    private String normalizeJiraDomain(String domain) {
        if (domain == null || domain.isBlank()) return null;
        return domain.replace("https://", "").replace("http://", "").replaceAll("/+$", "");
    }

}
