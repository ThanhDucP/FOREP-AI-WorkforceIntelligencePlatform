package com.aiworkforce.integration.service;

import com.aiworkforce.core.enums.ExternalIdentityProvider;
import com.aiworkforce.core.enums.IntegrationProvider;
import com.aiworkforce.core.enums.TaskPriority;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.core.service.TokenProtectionService;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.service.TeamMembershipService;
import com.aiworkforce.integration.dto.SyncResult;
import com.aiworkforce.integration.entity.ExternalIdentity;
import com.aiworkforce.integration.entity.JiraIssueSnapshot;
import com.aiworkforce.integration.entity.JiraProjectSnapshot;
import com.aiworkforce.integration.entity.JiraSprintSnapshot;
import com.aiworkforce.integration.entity.TaskIntegrationConfig;
import com.aiworkforce.integration.repository.ExternalIdentityRepository;
import com.aiworkforce.integration.repository.JiraIssueSnapshotRepository;
import com.aiworkforce.integration.repository.JiraProjectSnapshotRepository;
import com.aiworkforce.integration.repository.JiraSprintSnapshotRepository;
import com.aiworkforce.task.entity.Task;
import com.aiworkforce.task.repository.TaskRepository;
import com.aiworkforce.task.service.TaskAssessmentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JiraApiClient {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;
    private final TaskAssessmentService taskAssessmentService;
    private final TeamMembershipService membershipService;
    private final JiraProjectSnapshotRepository projectSnapshotRepository;
    private final JiraSprintSnapshotRepository sprintSnapshotRepository;
    private final JiraIssueSnapshotRepository issueSnapshotRepository;
    private final TokenProtectionService tokenProtectionService;
    private final ExternalIdentityRepository externalIdentityRepository;

    // Field for testing
    private String jiraApiUrlOverride = null;

    @Transactional
    public SyncResult syncIssues(TaskIntegrationConfig config) {
        return syncIssues(config, null);
    }

    @Transactional
    public SyncResult syncIssues(TaskIntegrationConfig config, LocalDateTime lastSyncTime) {
        String projectKeyRaw = config.getProjectKey(); // e.g. "domain.atlassian.net/PROJ" or "PROJ"
        String accessToken = tokenProtectionService.unprotect(config.getAccessToken());

        if (projectKeyRaw == null || projectKeyRaw.isBlank()) {
            log.warn("Project key is blank, skipping Jira sync for config: {}", config.getId());
            return SyncResult.empty();
        }

        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Access token is blank, skipping Jira sync for config: {}", config.getId());
            return SyncResult.empty();
        }

        JiraProjectRef projectRef = parseProjectRef(projectKeyRaw);
        log.info("Starting active Jira sync for project: {} on domain: {}", projectRef.projectKey(), projectRef.domain());

        try {
            String baseUrl = (jiraApiUrlOverride != null) ? jiraApiUrlOverride : "https://" + projectRef.domain();
            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", buildAuthorizationHeader(accessToken))
                    .defaultHeader("Accept", "application/json")
                    .build();

            syncProjectMetadata(webClient, config, projectRef);
            syncProjectSprints(webClient, config, projectRef);
            SyncResult result = syncIssueTasks(webClient, config, projectRef, lastSyncTime);
            refreshProjectFeatureAvailability(config, projectRef);

            log.info("Successfully synced Jira project {} | fetched: {}, created: {}, updated: {}", projectRef.projectKey(), result.getTotalFetched(), result.getTotalCreated(), result.getTotalUpdated());
            return result;
        } catch (Exception e) {
            log.error("Failed to sync issues from Jira for config: {}", config.getId(), e);
            throw new RuntimeException("Jira Sync Failed: " + e.getMessage(), e);
        }
    }

    private SyncResult syncIssueTasks(WebClient webClient, TaskIntegrationConfig config, JiraProjectRef projectRef, LocalDateTime lastSyncTime) throws Exception {
        String responseJson = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/api/3/search/jql")
                        .queryParam("jql", buildIssueJql(projectRef, lastSyncTime))
                        .queryParam("maxResults", 100)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (responseJson == null) {
            log.warn("No issues found or empty response from Jira API");
            return SyncResult.empty();
        }

        JsonNode rootNode = objectMapper.readTree(responseJson);
        JsonNode issuesNode = rootNode.path("issues");
        if (!issuesNode.isArray()) {
            log.warn("Expected JSON array in issues field from Jira API, but got: {}", responseJson);
            return SyncResult.empty();
        }

        SyncResult result = SyncResult.empty();
        for (JsonNode issueNode : issuesNode) {
            String issueKey = issueNode.path("key").asText();
            JsonNode fields = issueNode.path("fields");
            String summary = fields.path("summary").asText();
            String description = fields.path("description").asText();
            String externalUrl = buildExternalUrl(issueNode, projectRef, issueKey);
            Employee assignee = resolveAssignee(fields.path("assignee"), config);
            JiraSprintRef sprintRef = extractSprint(fields);

            Task task = upsertJiraTask(config, issueKey);
            boolean created = task.getId() == null;
            task.setTitle(summary);
            task.setDescription(description);
            task.setExternalUrl(externalUrl);
            task.setAssignee(assignee);
            task.setTeam(config.getTeam());
            task.setProject(config.getProject());
            task.setPriority(mapPriority(fields.path("priority").path("name").asText()));
            task.setDueDate(parseDueDateTime(fields.path("duedate").asText(null)));
            task.setEstimatedHours(estimateHours(fields));
            task.setStoryPoints(extractStoryPoints(fields));
            task.setSprintNumber(sprintRef != null ? sprintRef.id() : null);
            task.setExternalDeleted(false);
            applyStatus(task, fields.path("status"));

            saveIssueSnapshot(config, projectRef, issueNode, fields, assignee, externalUrl, sprintRef);
            taskAssessmentService.assess(task, "JIRA_SYNC");
            taskRepository.save(task);
            result.addFetched(1);
            if (created) {
                result.addCreated();
            } else {
                result.addUpdated();
            }
        }
        return result;
    }

    private void syncProjectMetadata(WebClient webClient, TaskIntegrationConfig config, JiraProjectRef projectRef) {
        try {
            String projectJson = webClient.get()
                    .uri("/rest/api/3/project/" + projectRef.projectKey())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            if (projectJson == null) {
                return;
            }
            JsonNode projectNode = objectMapper.readTree(projectJson);
            JiraProjectSnapshot snapshot = projectSnapshotRepository
                    .findByConfigIdAndJiraDomainIgnoreCaseAndProjectKeyIgnoreCase(
                            config.getId(), projectRef.domain(), projectRef.projectKey())
                    .orElseGet(JiraProjectSnapshot::new);
            snapshot.setConfig(config);
            snapshot.setProject(config.getProject());
            snapshot.setTeam(config.getTeam());
            snapshot.setJiraDomain(projectRef.domain());
            snapshot.setProjectKey(projectRef.projectKey());
            snapshot.setProviderProjectId(projectNode.path("id").asText(null));
            snapshot.setName(projectNode.path("name").asText(null));
            snapshot.setProjectTypeKey(projectNode.path("projectTypeKey").asText(null));
            snapshot.setLeadAccountId(projectNode.path("lead").path("accountId").asText(null));
            snapshot.setLeadDisplayName(projectNode.path("lead").path("displayName").asText(null));
            snapshot.setSelfUrl(projectNode.path("self").asText(null));
            snapshot.setSprintDataAvailable(false);
            snapshot.setStoryPointsAvailable(false);
            snapshot.setEpicDataAvailable(false);
            snapshot.setVersionDataAvailable(false);
            snapshot.setComponentDataAvailable(false);
            projectSnapshotRepository.save(snapshot);
        } catch (Exception e) {
            log.warn("Unable to sync Jira project metadata for {}, continuing", projectRef.projectKey());
        }
    }

    private void syncProjectSprints(WebClient webClient, TaskIntegrationConfig config, JiraProjectRef projectRef) {
        try {
            String boardsJson = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/rest/agile/1.0/board")
                            .queryParam("projectKeyOrId", projectRef.projectKey())
                            .queryParam("maxResults", 50)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            JsonNode boards = objectMapper.readTree(boardsJson).path("values");
            if (!boards.isArray()) {
                return;
            }
            for (JsonNode board : boards) {
                int boardId = board.path("id").asInt(0);
                if (boardId <= 0) {
                    continue;
                }
                syncBoardSprints(webClient, config, projectRef, boardId);
            }
        } catch (Exception e) {
            log.warn("Unable to sync Jira boards/sprints for {}, continuing", projectRef.projectKey());
        }
    }

    private void syncBoardSprints(WebClient webClient, TaskIntegrationConfig config, JiraProjectRef projectRef, int boardId) {
        try {
            String sprintsJson = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/rest/agile/1.0/board/" + boardId + "/sprint")
                            .queryParam("state", "active,future,closed")
                            .queryParam("maxResults", 100)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            JsonNode sprints = objectMapper.readTree(sprintsJson).path("values");
            if (!sprints.isArray()) {
                return;
            }
            for (JsonNode sprintNode : sprints) {
                int sprintId = sprintNode.path("id").asInt(0);
                if (sprintId <= 0) {
                    continue;
                }
                JiraSprintSnapshot sprint = sprintSnapshotRepository
                        .findByConfigIdAndSprintId(config.getId(), sprintId)
                        .orElseGet(JiraSprintSnapshot::new);
                sprint.setConfig(config);
                sprint.setProject(config.getProject());
                sprint.setTeam(config.getTeam());
                sprint.setJiraDomain(projectRef.domain());
                sprint.setProjectKey(projectRef.projectKey());
                sprint.setBoardId(boardId);
                sprint.setSprintId(sprintId);
                sprint.setName(sprintNode.path("name").asText(null));
                sprint.setState(sprintNode.path("state").asText(null));
                sprint.setStartDate(parseJiraDateTime(sprintNode.path("startDate").asText(null)));
                sprint.setEndDate(parseJiraDateTime(sprintNode.path("endDate").asText(null)));
                sprint.setCompleteDate(parseJiraDateTime(sprintNode.path("completeDate").asText(null)));
                sprint.setSelfUrl(sprintNode.path("self").asText(null));
                sprintSnapshotRepository.save(sprint);
                markProjectFeatureAvailability(config, projectRef, true, null, null, null, null);
            }
        } catch (Exception e) {
            log.warn("Unable to sync Jira sprints for board {}, continuing", boardId);
        }
    }

    private void saveIssueSnapshot(TaskIntegrationConfig config, JiraProjectRef projectRef, JsonNode issueNode, JsonNode fields,
                                   Employee assignee, String externalUrl, JiraSprintRef sprintRef) {
        String issueKey = issueNode.path("key").asText();
        JiraIssueSnapshot snapshot = issueSnapshotRepository
                .findByConfigIdAndIssueKeyIgnoreCase(config.getId(), issueKey)
                .orElseGet(JiraIssueSnapshot::new);
        JsonNode assigneeNode = fields.path("assignee");
        JsonNode reporterNode = fields.path("reporter");
        snapshot.setConfig(config);
        snapshot.setProject(config.getProject());
        snapshot.setTeam(config.getTeam());
        snapshot.setAssignee(assignee);
        snapshot.setJiraDomain(projectRef.domain());
        snapshot.setProjectKey(projectRef.projectKey());
        snapshot.setIssueKey(issueKey);
        snapshot.setProviderIssueId(issueNode.path("id").asText(null));
        snapshot.setSummary(fields.path("summary").asText(null));
        snapshot.setStatusName(fields.path("status").path("name").asText(null));
        snapshot.setIssueType(fields.path("issuetype").path("name").asText(null));
        snapshot.setPriorityName(fields.path("priority").path("name").asText(null));
        snapshot.setExternalUrl(externalUrl);
        snapshot.setAssigneeAccountId(assigneeNode.path("accountId").asText(null));
        snapshot.setAssigneeEmail(assigneeNode.path("emailAddress").asText(null));
        snapshot.setAssigneeDisplayName(assigneeNode.path("displayName").asText(null));
        snapshot.setReporterAccountId(reporterNode.path("accountId").asText(null));
        snapshot.setReporterEmail(reporterNode.path("emailAddress").asText(null));
        snapshot.setReporterDisplayName(reporterNode.path("displayName").asText(null));
        snapshot.setStoryPoints(extractStoryPoints(fields));
        snapshot.setOriginalEstimateSeconds(fields.path("timeoriginalestimate").asInt(0));
        snapshot.setRemainingEstimateSeconds(fields.path("timeestimate").asInt(0));
        snapshot.setSprintId(sprintRef != null ? sprintRef.id() : null);
        snapshot.setSprintName(sprintRef != null ? sprintRef.name() : null);
        snapshot.setLabels(joinTextArray(fields.path("labels")));
        snapshot.setEpicKey(extractEpicKey(fields));
        snapshot.setFixVersions(joinNamedArray(fields.path("fixVersions")));
        snapshot.setComponents(joinNamedArray(fields.path("components")));
        snapshot.setProviderCreatedAt(parseJiraDate(fields.path("created").asText(null)));
        snapshot.setProviderUpdatedAt(parseJiraDate(fields.path("updated").asText(null)));
        snapshot.setDueDate(parseDueDate(fields.path("duedate").asText(null)));
        issueSnapshotRepository.save(snapshot);
        upsertJiraIdentity(config, assigneeNode);
        upsertJiraIdentity(config, reporterNode);
        markProjectFeatureAvailability(config, projectRef, sprintRef != null, snapshot.getStoryPoints() != null, snapshot.getEpicKey() != null, snapshot.getFixVersions() != null, snapshot.getComponents() != null);
    }



    private void upsertJiraIdentity(TaskIntegrationConfig config, JsonNode userNode) {
        if (userNode == null || userNode.isMissingNode() || userNode.isNull()) return;
        String accountId = userNode.path("accountId").asText(null);
        if (accountId == null || accountId.isBlank()) return;
        ExternalIdentity identity = externalIdentityRepository
                .findByProviderAndExternalId(ExternalIdentityProvider.JIRA, accountId)
                .orElseGet(ExternalIdentity::new);
        identity.setProvider(ExternalIdentityProvider.JIRA);
        identity.setExternalId(accountId);
        identity.setUsername(userNode.path("emailAddress").asText(null));
        identity.setDisplayName(userNode.path("displayName").asText(null));
        identity.setEmail(userNode.path("emailAddress").asText(null));
        identity.setAvatarUrl(userNode.path("avatarUrls").path("48x48").asText(null));
        identity.setTeam(config.getTeam());
        if (config.getTeam() != null) {
            identity.setOrganization(config.getTeam().getOrganization());
        }
        externalIdentityRepository.save(identity);
    }
    private void refreshProjectFeatureAvailability(TaskIntegrationConfig config, JiraProjectRef projectRef) {
        markProjectFeatureAvailability(
                config,
                projectRef,
                issueSnapshotRepository.countByConfigIdAndSprintIdIsNotNull(config.getId()) > 0,
                issueSnapshotRepository.countByConfigIdAndStoryPointsIsNotNull(config.getId()) > 0,
                issueSnapshotRepository.countByConfigIdAndEpicKeyIsNotNull(config.getId()) > 0,
                issueSnapshotRepository.countByConfigIdAndFixVersionsIsNotNull(config.getId()) > 0,
                issueSnapshotRepository.countByConfigIdAndComponentsIsNotNull(config.getId()) > 0
        );
    }

    private void markProjectFeatureAvailability(TaskIntegrationConfig config, JiraProjectRef projectRef, Boolean sprint, Boolean storyPoints, Boolean epic, Boolean version, Boolean component) {
        JiraProjectSnapshot snapshot = projectSnapshotRepository
                .findByConfigIdAndJiraDomainIgnoreCaseAndProjectKeyIgnoreCase(config.getId(), projectRef.domain(), projectRef.projectKey())
                .orElseGet(JiraProjectSnapshot::new);
        snapshot.setConfig(config);
        snapshot.setProject(config.getProject());
        snapshot.setTeam(config.getTeam());
        snapshot.setJiraDomain(projectRef.domain());
        snapshot.setProjectKey(projectRef.projectKey());
        if (sprint != null && sprint) snapshot.setSprintDataAvailable(true);
        if (storyPoints != null && storyPoints) snapshot.setStoryPointsAvailable(true);
        if (epic != null && epic) snapshot.setEpicDataAvailable(true);
        if (version != null && version) snapshot.setVersionDataAvailable(true);
        if (component != null && component) snapshot.setComponentDataAvailable(true);
        projectSnapshotRepository.save(snapshot);
    }
    private String buildIssueJql(JiraProjectRef projectRef, LocalDateTime lastSyncTime) {
        String base = "project=" + projectRef.projectKey();
        if (lastSyncTime == null) {
            return base;
        }
        return base + " AND updated >= \"" + lastSyncTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\"";
    }

    private JiraProjectRef parseProjectRef(String projectKeyRaw) {
        String domain = "your-domain.atlassian.net";
        String projectKey = projectKeyRaw;
        if (projectKeyRaw.contains("/")) {
            int slashIndex = projectKeyRaw.indexOf("/");
            domain = projectKeyRaw.substring(0, slashIndex);
            projectKey = projectKeyRaw.substring(slashIndex + 1);
        }
        return new JiraProjectRef(domain, projectKey);
    }

    private Task upsertJiraTask(TaskIntegrationConfig config, String issueKey) {
        Optional<Task> existingTaskOpt = config.getProject() != null
                ? taskRepository.findByExternalTicketRefAndSourceProviderAndProjectId(
                        issueKey, IntegrationProvider.JIRA, config.getProject().getId())
                : taskRepository.findByExternalTicketRefAndSourceProvider(issueKey, IntegrationProvider.JIRA);
        if (existingTaskOpt.isPresent()) {
            return existingTaskOpt.get();
        }
        Task task = new Task();
        task.setExternalTicketRef(issueKey);
        task.setSourceProvider(IntegrationProvider.JIRA);
        task.setTeam(config.getTeam());
        task.setProject(config.getProject());
        return task;
    }

    private Employee resolveAssignee(JsonNode assigneeNode, TaskIntegrationConfig config) {
        if (assigneeNode == null || assigneeNode.isMissingNode() || assigneeNode.isNull()) {
            return null;
        }
        String emailAddress = assigneeNode.path("emailAddress").asText();
        if (emailAddress == null || emailAddress.isBlank()) {
            return null;
        }
        Employee assignee = employeeRepository.findByAccountEmail(emailAddress).orElse(null);
        if (assignee != null && !membershipService.hasActiveTeamAccess(assignee.getId(), config.getTeam().getId())) {
            return null;
        }
        return assignee;
    }

    private String buildExternalUrl(JsonNode issueNode, JiraProjectRef projectRef, String issueKey) {
        String selfUri = issueNode.path("self").asText();
        if (selfUri != null && selfUri.contains("/rest/api/")) {
            return selfUri.substring(0, selfUri.indexOf("/rest/api/")) + "/browse/" + issueKey;
        }
        return "https://" + projectRef.domain() + "/browse/" + issueKey;
    }

    private void applyStatus(Task task, JsonNode statusNode) {
        if (statusNode == null || statusNode.isMissingNode() || statusNode.isNull()) {
            return;
        }
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

    private LocalDateTime parseDueDateTime(String dueDate) {
        LocalDate parsed = parseDueDate(dueDate);
        return parsed != null ? parsed.atStartOfDay() : null;
    }

    private LocalDate parseDueDate(String dueDate) {
        if (dueDate == null || dueDate.isBlank()) return null;
        try {
            return LocalDate.parse(dueDate);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseJiraDateTime(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException e) {
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


    private LocalDate parseJiraDate(String value) {
        LocalDateTime dateTime = parseJiraDateTime(value);
        return dateTime != null ? dateTime.toLocalDate() : null;
    }

    private String joinTextArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) return null;
        List<String> values = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            String value = node.asText(null);
            if (value != null && !value.isBlank()) values.add(value);
        }
        return values.isEmpty() ? null : String.join(",", values);
    }

    private String joinNamedArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) return null;
        List<String> values = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            String value = node.path("name").asText(null);
            if (value != null && !value.isBlank()) values.add(value);
        }
        return values.isEmpty() ? null : String.join(",", values);
    }

    private String extractEpicKey(JsonNode fields) {
        String parentKey = fields.path("parent").path("key").asText(null);
        if (parentKey != null && !parentKey.isBlank()) return parentKey;
        String epicLink = fields.path("customfield_10014").asText(null);
        return epicLink != null && !epicLink.isBlank() ? epicLink : null;
    }
    private Integer extractStoryPoints(JsonNode fields) {
        JsonNode storyPoints = fields.path("customfield_10016");
        if (storyPoints.isNumber()) {
            return storyPoints.asInt();
        }
        return null;
    }

    private JiraSprintRef extractSprint(JsonNode fields) {
        JsonNode sprintField = fields.path("customfield_10020");
        if (sprintField.isArray() && sprintField.size() > 0) {
            JsonNode sprint = sprintField.get(sprintField.size() - 1);
            return new JiraSprintRef(sprint.path("id").asInt(0), sprint.path("name").asText(null));
        }
        if (sprintField.isObject()) {
            return new JiraSprintRef(sprintField.path("id").asInt(0), sprintField.path("name").asText(null));
        }
        return null;
    }

    private record JiraProjectRef(String domain, String projectKey) {
    }

    private record JiraSprintRef(Integer id, String name) {
    }
}
