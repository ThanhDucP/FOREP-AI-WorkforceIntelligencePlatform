package com.aiworkforce.integration.service;

import com.aiworkforce.core.enums.IntegrationProvider;
import com.aiworkforce.core.enums.TaskPriority;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.core.service.TokenProtectionService;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.service.TeamMembershipService;
import com.aiworkforce.integration.dto.SyncResult;
import com.aiworkforce.integration.entity.GithubCommit;
import com.aiworkforce.integration.entity.GithubContributor;
import com.aiworkforce.integration.entity.GithubPullRequest;
import com.aiworkforce.integration.entity.GithubRepositorySnapshot;
import com.aiworkforce.integration.entity.TaskIntegrationConfig;
import com.aiworkforce.integration.repository.GithubCommitRepository;
import com.aiworkforce.integration.repository.GithubContributorRepository;
import com.aiworkforce.integration.repository.GithubPullRequestRepository;
import com.aiworkforce.integration.repository.GithubRepositorySnapshotRepository;
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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubApiClient {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;
    private final TaskAssessmentService taskAssessmentService;
    private final TeamMembershipService membershipService;
    private final GithubRepositorySnapshotRepository repositorySnapshotRepository;
    private final GithubContributorRepository contributorRepository;
    private final GithubPullRequestRepository pullRequestRepository;
    private final GithubCommitRepository commitRepository;
    private final TokenProtectionService tokenProtectionService;

    private String githubApiUrl = "https://api.github.com";

    @Transactional
    public SyncResult syncIssues(TaskIntegrationConfig config) {
        return syncIssues(config, null);
    }

    @Transactional
    public SyncResult syncIssues(TaskIntegrationConfig config, LocalDateTime lastSyncTime) {
        String projectKey = config.getProjectKey();
        String accessToken = tokenProtectionService.unprotect(config.getAccessToken());

        if (projectKey == null || projectKey.isBlank()) {
            log.warn("Project key is blank, skipping GitHub sync for config: {}", config.getId());
            return SyncResult.empty();
        }

        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Access token is blank, skipping GitHub sync for config: {}", config.getId());
            return SyncResult.empty();
        }

        log.info("Starting GitHub sync for repository: {} | incremental since: {}", projectKey, lastSyncTime);

        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(githubApiUrl)
                    .defaultHeader("Authorization", "Bearer " + accessToken)
                    .defaultHeader("User-Agent", "FOREP-AI-Platform")
                    .defaultHeader("Accept", "application/vnd.github+json")
                    .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                    .build();

            SyncResult result = SyncResult.empty();
            syncRepositoryMetadata(webClient, config, projectKey);
            syncContributors(webClient, config, projectKey);
            result.addFetched(syncPullRequests(webClient, config, projectKey, lastSyncTime));
            List<GithubCommitSignal> commitSignals = fetchCommitSignals(webClient, config, projectKey, lastSyncTime);
            result.addFetched(commitSignals.size());
            result.merge(syncIssueTasks(webClient, config, projectKey, commitSignals, lastSyncTime));

            log.info("Successfully synced GitHub repository {} | fetched: {}, created: {}, updated: {}",
                    projectKey, result.getTotalFetched(), result.getTotalCreated(), result.getTotalUpdated());
            return result;
        } catch (Exception e) {
            log.error("Failed to sync GitHub repository for config: {}", config.getId(), e);
            throw new RuntimeException("GitHub Sync Failed: " + e.getMessage(), e);
        }
    }

    private SyncResult syncIssueTasks(WebClient webClient, TaskIntegrationConfig config, String projectKey,
                                      List<GithubCommitSignal> commitSignals, LocalDateTime lastSyncTime) throws Exception {
        String responseJson = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/" + projectKey + "/issues")
                        .queryParam("state", "all")
                        .queryParam("per_page", 100)
                        .queryParamIfPresent("since", Optional.ofNullable(formatGithubSince(lastSyncTime)))
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (responseJson == null) {
            log.warn("No issues found or empty response from GitHub API");
            return SyncResult.empty();
        }

        JsonNode rootNode = objectMapper.readTree(responseJson);
        if (!rootNode.isArray()) {
            log.warn("Expected JSON array from GitHub API, but got: {}", responseJson);
            return SyncResult.empty();
        }

        SyncResult result = SyncResult.empty();
        for (JsonNode issueNode : rootNode) {
            if (issueNode.has("pull_request")) {
                continue;
            }

            String issueNumber = issueNode.path("number").asText();
            String externalTicketRef = "GH-" + issueNumber;
            Task task = upsertGithubTask(config, externalTicketRef);
            boolean created = task.getId() == null;

            String title = issueNode.path("title").asText();
            String body = issueNode.path("body").asText("");
            task.setTitle(title);
            task.setDescription(body);
            task.setExternalUrl(issueNode.path("html_url").asText(null));
            task.setAssignee(resolveAssignee(issueNode.path("assignee"), config));
            task.setTeam(config.getTeam());
            task.setProject(config.getProject());
            task.setPriority(mapPriority(issueNode.path("labels")));
            task.setEstimatedHours(estimateHours(title, body));
            task.setExternalDeleted(false);
            applyGithubState(task, issueNode.path("state").asText());

            TaskAssessmentService.GithubCommitMetrics metrics = assessLinkedCommits(
                    commitSignals, issueNumber, externalTicketRef);
            taskAssessmentService.assess(task, "GITHUB_SYNC", metrics);
            taskRepository.save(task);

            result.addFetched(1);
            if (created) result.addCreated(); else result.addUpdated();
        }
        return result;
    }

    private void syncRepositoryMetadata(WebClient webClient, TaskIntegrationConfig config, String projectKey) {
        try {
            String repositoryJson = webClient.get().uri("/repos/" + projectKey).retrieve().bodyToMono(String.class).block();
            if (repositoryJson == null) return;
            JsonNode repoNode = objectMapper.readTree(repositoryJson);
            String fullName = repoNode.path("full_name").asText(projectKey);
            GithubRepositorySnapshot snapshot = repositorySnapshotRepository
                    .findByConfigIdAndFullNameIgnoreCase(config.getId(), fullName)
                    .orElseGet(GithubRepositorySnapshot::new);
            snapshot.setConfig(config);
            snapshot.setProject(config.getProject());
            snapshot.setTeam(config.getTeam());
            snapshot.setFullName(fullName);
            snapshot.setName(repoNode.path("name").asText(null));
            snapshot.setOwnerLogin(repoNode.path("owner").path("login").asText(null));
            snapshot.setHtmlUrl(repoNode.path("html_url").asText(null));
            snapshot.setDefaultBranch(repoNode.path("default_branch").asText(null));
            snapshot.setPrivateRepository(repoNode.path("private").asBoolean(false));
            snapshot.setStargazersCount(repoNode.path("stargazers_count").asInt(0));
            snapshot.setForksCount(repoNode.path("forks_count").asInt(0));
            snapshot.setOpenIssuesCount(repoNode.path("open_issues_count").asInt(0));
            snapshot.setPushedAt(parseGithubDate(repoNode.path("pushed_at").asText(null)));
            snapshot.setProviderUpdatedAt(parseGithubDate(repoNode.path("updated_at").asText(null)));
            repositorySnapshotRepository.save(snapshot);
        } catch (Exception e) {
            log.warn("Unable to sync GitHub repository metadata for {}, continuing", projectKey);
        }
    }

    private void syncContributors(WebClient webClient, TaskIntegrationConfig config, String projectKey) {
        try {
            String contributorsJson = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/repos/" + projectKey + "/contributors").queryParam("per_page", 100).build())
                    .retrieve().bodyToMono(String.class).block();
            JsonNode contributors = objectMapper.readTree(contributorsJson);
            if (!contributors.isArray()) return;
            for (JsonNode contributorNode : contributors) {
                String login = contributorNode.path("login").asText(null);
                if (login == null || login.isBlank()) continue;
                GithubContributor contributor = contributorRepository
                        .findByConfigIdAndRepositoryFullNameIgnoreCaseAndLoginIgnoreCase(config.getId(), projectKey, login)
                        .orElseGet(GithubContributor::new);
                contributor.setConfig(config);
                contributor.setProject(config.getProject());
                contributor.setTeam(config.getTeam());
                contributor.setRepositoryFullName(projectKey);
                contributor.setLogin(login);
                contributor.setAvatarUrl(contributorNode.path("avatar_url").asText(null));
                contributor.setHtmlUrl(contributorNode.path("html_url").asText(null));
                contributor.setContributions(contributorNode.path("contributions").asInt(0));
                contributorRepository.save(contributor);
            }
        } catch (Exception e) {
            log.warn("Unable to sync GitHub contributors for {}, continuing", projectKey);
        }
    }

    private int syncPullRequests(WebClient webClient, TaskIntegrationConfig config, String projectKey, LocalDateTime lastSyncTime) {
        try {
            String pullsJson = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/" + projectKey + "/pulls")
                            .queryParam("state", "all")
                            .queryParam("sort", "updated")
                            .queryParam("direction", "desc")
                            .queryParam("per_page", 100)
                            .build())
                    .retrieve().bodyToMono(String.class).block();
            JsonNode pullRequests = objectMapper.readTree(pullsJson);
            if (!pullRequests.isArray()) return 0;

            int count = 0;
            for (JsonNode prNode : pullRequests) {
                int number = prNode.path("number").asInt();
                if (number <= 0) continue;
                LocalDateTime providerUpdatedAt = parseGithubDate(prNode.path("updated_at").asText(null));
                if (!isAfterLastSync(providerUpdatedAt, lastSyncTime)) continue;

                GithubPullRequest pullRequest = pullRequestRepository
                        .findByConfigIdAndRepositoryFullNameIgnoreCaseAndNumber(config.getId(), projectKey, number)
                        .orElseGet(GithubPullRequest::new);
                pullRequest.setConfig(config);
                pullRequest.setProject(config.getProject());
                pullRequest.setTeam(config.getTeam());
                pullRequest.setRepositoryFullName(projectKey);
                pullRequest.setNumber(number);
                pullRequest.setTitle(prNode.path("title").asText(null));
                pullRequest.setState(prNode.path("state").asText(null));
                pullRequest.setHtmlUrl(prNode.path("html_url").asText(null));
                pullRequest.setAuthorLogin(prNode.path("user").path("login").asText(null));
                pullRequest.setDraft(prNode.path("draft").asBoolean(false));
                pullRequest.setMerged(!prNode.path("merged_at").isMissingNode() && !prNode.path("merged_at").isNull());
                pullRequest.setProviderCreatedAt(parseGithubDate(prNode.path("created_at").asText(null)));
                pullRequest.setProviderUpdatedAt(providerUpdatedAt);
                pullRequest.setClosedAt(parseGithubDate(prNode.path("closed_at").asText(null)));
                pullRequest.setMergedAt(parseGithubDate(prNode.path("merged_at").asText(null)));
                pullRequestRepository.save(pullRequest);
                count++;
            }
            return count;
        } catch (Exception e) {
            log.warn("Unable to sync GitHub pull requests for {}, continuing", projectKey);
            return 0;
        }
    }

    private List<GithubCommitSignal> fetchCommitSignals(WebClient webClient, TaskIntegrationConfig config, String projectKey, LocalDateTime lastSyncTime) {
        try {
            String commitsJson = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/" + projectKey + "/commits")
                            .queryParam("per_page", 100)
                            .queryParamIfPresent("since", Optional.ofNullable(formatGithubSince(lastSyncTime)))
                            .build())
                    .retrieve().bodyToMono(String.class).block();

            JsonNode commits = objectMapper.readTree(commitsJson);
            if (!commits.isArray()) return List.of();

            List<GithubCommitSignal> signals = new ArrayList<>();
            for (JsonNode commitNode : commits) {
                String sha = commitNode.path("sha").asText();
                String message = commitNode.path("commit").path("message").asText("");
                signals.add(fetchCommitDetail(webClient, config, projectKey, sha, message));
            }
            return signals;
        } catch (Exception e) {
            log.warn("Unable to load GitHub commit metrics for repository {}, continuing without commits", projectKey);
            return List.of();
        }
    }

    private GithubCommitSignal fetchCommitDetail(WebClient webClient, TaskIntegrationConfig config, String projectKey, String sha, String message) {
        try {
            String detailJson = webClient.get().uri("/repos/" + projectKey + "/commits/" + sha).retrieve().bodyToMono(String.class).block();
            JsonNode detail = objectMapper.readTree(detailJson);
            int additions = detail.path("stats").path("additions").asInt(0);
            int deletions = detail.path("stats").path("deletions").asInt(0);
            int files = detail.path("files").isArray() ? detail.path("files").size() : 0;
            saveCommit(config, projectKey, sha, message, detail, additions, deletions, files);
            return new GithubCommitSignal(sha, message, additions, deletions, files);
        } catch (Exception e) {
            return new GithubCommitSignal(sha, message, 0, 0, 0);
        }
    }

    private void saveCommit(TaskIntegrationConfig config, String projectKey, String sha, String message, JsonNode detail, int additions, int deletions, int files) {
        if (sha == null || sha.isBlank()) return;
        GithubCommit commit = commitRepository.findByConfigIdAndRepositoryFullNameIgnoreCaseAndSha(config.getId(), projectKey, sha)
                .orElseGet(GithubCommit::new);
        JsonNode commitNode = detail.path("commit");
        JsonNode authorNode = commitNode.path("author");
        commit.setConfig(config);
        commit.setProject(config.getProject());
        commit.setTeam(config.getTeam());
        commit.setRepositoryFullName(projectKey);
        commit.setSha(sha);
        commit.setMessage(message);
        commit.setAuthorName(authorNode.path("name").asText(null));
        commit.setAuthorEmail(authorNode.path("email").asText(null));
        commit.setHtmlUrl(detail.path("html_url").asText(null));
        commit.setAdditions(additions);
        commit.setDeletions(deletions);
        commit.setChangedFiles(files);
        commit.setCommittedAt(parseGithubDate(authorNode.path("date").asText(null)));
        commitRepository.save(commit);
    }

    private Task upsertGithubTask(TaskIntegrationConfig config, String externalTicketRef) {
        Optional<Task> existingTaskOpt = config.getProject() != null
                ? taskRepository.findByExternalTicketRefAndSourceProviderAndProjectId(externalTicketRef, IntegrationProvider.GITHUB, config.getProject().getId())
                : taskRepository.findByExternalTicketRefAndSourceProvider(externalTicketRef, IntegrationProvider.GITHUB);
        if (existingTaskOpt.isPresent()) return existingTaskOpt.get();
        Task task = new Task();
        task.setExternalTicketRef(externalTicketRef);
        task.setSourceProvider(IntegrationProvider.GITHUB);
        task.setTeam(config.getTeam());
        task.setProject(config.getProject());
        return task;
    }

    private Employee resolveAssignee(JsonNode assigneeNode, TaskIntegrationConfig config) {
        if (assigneeNode == null || assigneeNode.isMissingNode() || assigneeNode.isNull()) return null;
        String assigneeEmail = assigneeNode.path("email").asText();
        if (assigneeEmail == null || assigneeEmail.isBlank()) return null;
        Employee assignee = employeeRepository.findByAccountEmail(assigneeEmail).orElse(null);
        if (assignee != null && !membershipService.hasActiveTeamAccess(assignee.getId(), config.getTeam().getId())) return null;
        return assignee;
    }

    private void applyGithubState(Task task, String state) {
        if ("closed".equalsIgnoreCase(state)) {
            task.setStatus(TaskStatus.DONE);
        } else if (task.getStatus() == null || task.getStatus() == TaskStatus.DONE) {
            task.setStatus(TaskStatus.TODO);
        }
    }

    private TaskAssessmentService.GithubCommitMetrics assessLinkedCommits(List<GithubCommitSignal> commits, String issueNumber, String externalTicketRef) {
        int count = 0;
        int additions = 0;
        int deletions = 0;
        int files = 0;
        String issueMarker = "#" + issueNumber;
        for (GithubCommitSignal commit : commits) {
            String message = commit.message().toUpperCase();
            if (message.contains(issueMarker.toUpperCase()) || message.contains(externalTicketRef.toUpperCase())) {
                count++;
                additions += commit.additions();
                deletions += commit.deletions();
                files += commit.changedFiles();
            }
        }
        return taskAssessmentService.assessGithubCommits(count, additions, deletions, files);
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

    private String formatGithubSince(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC).toString();
    }

    private boolean isAfterLastSync(LocalDateTime providerUpdatedAt, LocalDateTime lastSyncTime) {
        return lastSyncTime == null || providerUpdatedAt == null || !providerUpdatedAt.isBefore(lastSyncTime);
    }

    private LocalDateTime parseGithubDate(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) return null;
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private record GithubCommitSignal(String sha, String message, int additions, int deletions, int changedFiles) {
    }
}