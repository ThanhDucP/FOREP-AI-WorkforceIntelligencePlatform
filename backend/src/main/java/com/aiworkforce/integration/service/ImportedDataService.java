package com.aiworkforce.integration.service;

import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.core.pagination.PaginationResponse;
import com.aiworkforce.core.security.AccessPolicyService;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.entity.Project;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.OrganizationRepository;
import com.aiworkforce.identity.repository.ProjectRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.integration.dto.GithubCommitResponse;
import com.aiworkforce.integration.dto.GithubContributorResponse;
import com.aiworkforce.integration.dto.GithubPullRequestResponse;
import com.aiworkforce.integration.dto.GithubRepositoryResponse;
import com.aiworkforce.integration.dto.ImportedIssueResponse;
import com.aiworkforce.integration.dto.ImportedProjectResponse;
import com.aiworkforce.integration.entity.GithubCommit;
import com.aiworkforce.integration.entity.GithubContributor;
import com.aiworkforce.integration.entity.GithubPullRequest;
import com.aiworkforce.integration.entity.GithubRepositorySnapshot;
import com.aiworkforce.integration.entity.JiraIssueSnapshot;
import com.aiworkforce.integration.entity.JiraProjectSnapshot;
import com.aiworkforce.integration.repository.GithubCommitRepository;
import com.aiworkforce.integration.repository.GithubContributorRepository;
import com.aiworkforce.integration.repository.GithubPullRequestRepository;
import com.aiworkforce.integration.repository.GithubRepositorySnapshotRepository;
import com.aiworkforce.integration.repository.JiraIssueSnapshotRepository;
import com.aiworkforce.integration.repository.JiraProjectSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImportedDataService {

    private final JiraProjectSnapshotRepository jiraProjectRepository;
    private final JiraIssueSnapshotRepository jiraIssueRepository;
    private final GithubRepositorySnapshotRepository githubRepository;
    private final GithubCommitRepository githubCommitRepository;
    private final GithubPullRequestRepository githubPullRequestRepository;
    private final GithubContributorRepository githubContributorRepository;
    private final OrganizationRepository organizationRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final AccessPolicyService accessPolicyService;

    @Transactional(readOnly = true)
    public List<ImportedProjectResponse> getImportedProjects(UUID organizationId, UUID teamId, UUID projectId) {
        return switch (resolveScope(organizationId, teamId, projectId)) {
            case ORGANIZATION -> jiraProjectRepository.findByTeamOrganizationIdOrderByUpdatedAtDesc(organizationId).stream().map(this::mapProject).toList();
            case TEAM -> jiraProjectRepository.findByTeamIdOrderByUpdatedAtDesc(teamId).stream().map(this::mapProject).toList();
            case PROJECT -> jiraProjectRepository.findByProjectIdOrderByUpdatedAtDesc(projectId).stream().map(this::mapProject).toList();
        };
    }

    @Transactional(readOnly = true)
    public PaginationResponse<ImportedProjectResponse> getImportedProjects(UUID organizationId, UUID teamId, UUID projectId, int page, int size) {
        return paginate(getImportedProjects(organizationId, teamId, projectId), page, size);
    }

    @Transactional(readOnly = true)
    public List<ImportedIssueResponse> getImportedIssues(UUID organizationId, UUID teamId, UUID projectId) {
        return switch (resolveScope(organizationId, teamId, projectId)) {
            case ORGANIZATION -> jiraIssueRepository.findByTeamOrganizationIdOrderByUpdatedAtDesc(organizationId).stream().map(this::mapIssue).toList();
            case TEAM -> jiraIssueRepository.findByTeamIdOrderByUpdatedAtDesc(teamId).stream().map(this::mapIssue).toList();
            case PROJECT -> jiraIssueRepository.findByProjectIdOrderByUpdatedAtDesc(projectId).stream().map(this::mapIssue).toList();
        };
    }

    @Transactional(readOnly = true)
    public PaginationResponse<ImportedIssueResponse> getImportedIssues(UUID organizationId, UUID teamId, UUID projectId, int page, int size) {
        return paginate(getImportedIssues(organizationId, teamId, projectId), page, size);
    }

    @Transactional(readOnly = true)
    public List<GithubRepositoryResponse> getGithubRepositories(UUID organizationId, UUID teamId, UUID projectId) {
        return switch (resolveScope(organizationId, teamId, projectId)) {
            case ORGANIZATION -> githubRepository.findByTeamOrganizationIdOrderByProviderUpdatedAtDesc(organizationId).stream().map(this::mapRepository).toList();
            case TEAM -> githubRepository.findByTeamIdOrderByProviderUpdatedAtDesc(teamId).stream().map(this::mapRepository).toList();
            case PROJECT -> githubRepository.findByProjectIdOrderByProviderUpdatedAtDesc(projectId).stream().map(this::mapRepository).toList();
        };
    }

    @Transactional(readOnly = true)
    public PaginationResponse<GithubRepositoryResponse> getGithubRepositories(UUID organizationId, UUID teamId, UUID projectId, int page, int size) {
        return paginate(getGithubRepositories(organizationId, teamId, projectId), page, size);
    }

    @Transactional(readOnly = true)
    public List<GithubCommitResponse> getGithubCommits(UUID organizationId, UUID teamId, UUID projectId) {
        return switch (resolveScope(organizationId, teamId, projectId)) {
            case ORGANIZATION -> githubCommitRepository.findByTeamOrganizationIdOrderByCommittedAtDesc(organizationId).stream().map(this::mapCommit).toList();
            case TEAM -> githubCommitRepository.findByTeamIdOrderByCommittedAtDesc(teamId).stream().map(this::mapCommit).toList();
            case PROJECT -> githubCommitRepository.findByProjectIdOrderByCommittedAtDesc(projectId).stream().map(this::mapCommit).toList();
        };
    }

    @Transactional(readOnly = true)
    public PaginationResponse<GithubCommitResponse> getGithubCommits(UUID organizationId, UUID teamId, UUID projectId, int page, int size) {
        return paginate(getGithubCommits(organizationId, teamId, projectId), page, size);
    }

    @Transactional(readOnly = true)
    public List<GithubPullRequestResponse> getGithubPullRequests(UUID organizationId, UUID teamId, UUID projectId) {
        return switch (resolveScope(organizationId, teamId, projectId)) {
            case ORGANIZATION -> githubPullRequestRepository.findByTeamOrganizationIdOrderByProviderUpdatedAtDesc(organizationId).stream().map(this::mapPullRequest).toList();
            case TEAM -> githubPullRequestRepository.findByTeamIdOrderByProviderUpdatedAtDesc(teamId).stream().map(this::mapPullRequest).toList();
            case PROJECT -> githubPullRequestRepository.findByProjectIdOrderByProviderUpdatedAtDesc(projectId).stream().map(this::mapPullRequest).toList();
        };
    }

    @Transactional(readOnly = true)
    public PaginationResponse<GithubPullRequestResponse> getGithubPullRequests(UUID organizationId, UUID teamId, UUID projectId, int page, int size) {
        return paginate(getGithubPullRequests(organizationId, teamId, projectId), page, size);
    }

    @Transactional(readOnly = true)
    public List<GithubContributorResponse> getGithubContributors(UUID organizationId, UUID teamId, UUID projectId) {
        return switch (resolveScope(organizationId, teamId, projectId)) {
            case ORGANIZATION -> githubContributorRepository.findByTeamOrganizationIdOrderByContributionsDesc(organizationId).stream().map(this::mapContributor).toList();
            case TEAM -> githubContributorRepository.findByTeamIdOrderByContributionsDesc(teamId).stream().map(this::mapContributor).toList();
            case PROJECT -> githubContributorRepository.findByProjectIdOrderByContributionsDesc(projectId).stream().map(this::mapContributor).toList();
        };
    }

    @Transactional(readOnly = true)
    public PaginationResponse<GithubContributorResponse> getGithubContributors(UUID organizationId, UUID teamId, UUID projectId, int page, int size) {
        return paginate(getGithubContributors(organizationId, teamId, projectId), page, size);
    }

    private <T> PaginationResponse<T> paginate(List<T> items, int page, int size) {
        if (page < 0) {
            throw new BusinessException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new BusinessException("size must be between 1 and 100");
        }
        int totalElements = items.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        return PaginationResponse.<T>builder()
                .content(items.subList(fromIndex, toIndex))
                .pageNumber(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .isLast(totalPages == 0 || page >= totalPages - 1)
                .build();
    }

    private Scope resolveScope(UUID organizationId, UUID teamId, UUID projectId) {
        int selected = (organizationId != null ? 1 : 0) + (teamId != null ? 1 : 0) + (projectId != null ? 1 : 0);
        if (selected != 1) {
            throw new BusinessException("Select exactly one scope: organizationId, teamId, or projectId");
        }
        if (organizationId != null) {
            Organization organization = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
            accessPolicyService.ensureOrganizationAccess(organization);
            return Scope.ORGANIZATION;
        }
        if (teamId != null) {
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
            accessPolicyService.ensureTeamAccess(team);
            return Scope.TEAM;
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        accessPolicyService.ensureProjectAccess(project);
        return Scope.PROJECT;
    }

    private ImportedProjectResponse mapProject(JiraProjectSnapshot snapshot) {
        return ImportedProjectResponse.builder()
                .id(snapshot.getId())
                .configId(snapshot.getConfig() != null ? snapshot.getConfig().getId() : null)
                .projectId(snapshot.getProject() != null ? snapshot.getProject().getId() : null)
                .teamId(snapshot.getTeam() != null ? snapshot.getTeam().getId() : null)
                .jiraDomain(snapshot.getJiraDomain())
                .projectKey(snapshot.getProjectKey())
                .providerProjectId(snapshot.getProviderProjectId())
                .name(snapshot.getName())
                .projectTypeKey(snapshot.getProjectTypeKey())
                .leadAccountId(snapshot.getLeadAccountId())
                .leadDisplayName(snapshot.getLeadDisplayName())
                .sprintDataAvailable(snapshot.getSprintDataAvailable())
                .storyPointsAvailable(snapshot.getStoryPointsAvailable())
                .epicDataAvailable(snapshot.getEpicDataAvailable())
                .versionDataAvailable(snapshot.getVersionDataAvailable())
                .componentDataAvailable(snapshot.getComponentDataAvailable())
                .createdAt(snapshot.getCreatedAt())
                .updatedAt(snapshot.getUpdatedAt())
                .build();
    }

    private ImportedIssueResponse mapIssue(JiraIssueSnapshot issue) {
        return ImportedIssueResponse.builder()
                .id(issue.getId())
                .configId(issue.getConfig() != null ? issue.getConfig().getId() : null)
                .projectId(issue.getProject() != null ? issue.getProject().getId() : null)
                .teamId(issue.getTeam() != null ? issue.getTeam().getId() : null)
                .assigneeId(issue.getAssignee() != null ? issue.getAssignee().getId() : null)
                .jiraDomain(issue.getJiraDomain())
                .projectKey(issue.getProjectKey())
                .issueKey(issue.getIssueKey())
                .providerIssueId(issue.getProviderIssueId())
                .summary(issue.getSummary())
                .statusName(issue.getStatusName())
                .issueType(issue.getIssueType())
                .priorityName(issue.getPriorityName())
                .externalUrl(issue.getExternalUrl())
                .assigneeAccountId(issue.getAssigneeAccountId())
                .assigneeEmail(issue.getAssigneeEmail())
                .assigneeDisplayName(issue.getAssigneeDisplayName())
                .reporterAccountId(issue.getReporterAccountId())
                .reporterEmail(issue.getReporterEmail())
                .reporterDisplayName(issue.getReporterDisplayName())
                .storyPoints(issue.getStoryPoints())
                .sprintId(issue.getSprintId())
                .sprintName(issue.getSprintName())
                .labels(issue.getLabels())
                .epicKey(issue.getEpicKey())
                .fixVersions(issue.getFixVersions())
                .components(issue.getComponents())
                .providerCreatedAt(issue.getProviderCreatedAt())
                .providerUpdatedAt(issue.getProviderUpdatedAt())
                .dueDate(issue.getDueDate())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }

    private GithubRepositoryResponse mapRepository(GithubRepositorySnapshot repository) {
        return GithubRepositoryResponse.builder()
                .id(repository.getId())
                .configId(repository.getConfig() != null ? repository.getConfig().getId() : null)
                .projectId(repository.getProject() != null ? repository.getProject().getId() : null)
                .teamId(repository.getTeam() != null ? repository.getTeam().getId() : null)
                .fullName(repository.getFullName())
                .name(repository.getName())
                .ownerLogin(repository.getOwnerLogin())
                .htmlUrl(repository.getHtmlUrl())
                .defaultBranch(repository.getDefaultBranch())
                .privateRepository(repository.getPrivateRepository())
                .stargazersCount(repository.getStargazersCount())
                .forksCount(repository.getForksCount())
                .openIssuesCount(repository.getOpenIssuesCount())
                .pushedAt(repository.getPushedAt())
                .providerUpdatedAt(repository.getProviderUpdatedAt())
                .createdAt(repository.getCreatedAt())
                .updatedAt(repository.getUpdatedAt())
                .build();
    }

    private GithubCommitResponse mapCommit(GithubCommit commit) {
        return GithubCommitResponse.builder()
                .id(commit.getId())
                .configId(commit.getConfig() != null ? commit.getConfig().getId() : null)
                .projectId(commit.getProject() != null ? commit.getProject().getId() : null)
                .teamId(commit.getTeam() != null ? commit.getTeam().getId() : null)
                .repositoryFullName(commit.getRepositoryFullName())
                .sha(commit.getSha())
                .message(commit.getMessage())
                .authorName(commit.getAuthorName())
                .authorEmail(commit.getAuthorEmail())
                .htmlUrl(commit.getHtmlUrl())
                .additions(commit.getAdditions())
                .deletions(commit.getDeletions())
                .changedFiles(commit.getChangedFiles())
                .committedAt(commit.getCommittedAt())
                .createdAt(commit.getCreatedAt())
                .updatedAt(commit.getUpdatedAt())
                .build();
    }

    private GithubPullRequestResponse mapPullRequest(GithubPullRequest pullRequest) {
        return GithubPullRequestResponse.builder()
                .id(pullRequest.getId())
                .configId(pullRequest.getConfig() != null ? pullRequest.getConfig().getId() : null)
                .projectId(pullRequest.getProject() != null ? pullRequest.getProject().getId() : null)
                .teamId(pullRequest.getTeam() != null ? pullRequest.getTeam().getId() : null)
                .repositoryFullName(pullRequest.getRepositoryFullName())
                .number(pullRequest.getNumber())
                .title(pullRequest.getTitle())
                .state(pullRequest.getState())
                .htmlUrl(pullRequest.getHtmlUrl())
                .authorLogin(pullRequest.getAuthorLogin())
                .draft(pullRequest.getDraft())
                .merged(pullRequest.getMerged())
                .providerCreatedAt(pullRequest.getProviderCreatedAt())
                .providerUpdatedAt(pullRequest.getProviderUpdatedAt())
                .closedAt(pullRequest.getClosedAt())
                .mergedAt(pullRequest.getMergedAt())
                .reviewDelayHours(reviewDelayHours(pullRequest))
                .createdAt(pullRequest.getCreatedAt())
                .updatedAt(pullRequest.getUpdatedAt())
                .build();
    }

    private Long reviewDelayHours(GithubPullRequest pullRequest) {
        if (pullRequest.getProviderCreatedAt() == null) return null;
        if (Boolean.TRUE.equals(pullRequest.getMerged()) && pullRequest.getMergedAt() != null) {
            return Duration.between(pullRequest.getProviderCreatedAt(), pullRequest.getMergedAt()).toHours();
        }
        if (pullRequest.getClosedAt() != null) {
            return Duration.between(pullRequest.getProviderCreatedAt(), pullRequest.getClosedAt()).toHours();
        }
        return Duration.between(pullRequest.getProviderCreatedAt(), java.time.LocalDateTime.now()).toHours();
    }

    private GithubContributorResponse mapContributor(GithubContributor contributor) {
        return GithubContributorResponse.builder()
                .id(contributor.getId())
                .configId(contributor.getConfig() != null ? contributor.getConfig().getId() : null)
                .projectId(contributor.getProject() != null ? contributor.getProject().getId() : null)
                .teamId(contributor.getTeam() != null ? contributor.getTeam().getId() : null)
                .repositoryFullName(contributor.getRepositoryFullName())
                .login(contributor.getLogin())
                .avatarUrl(contributor.getAvatarUrl())
                .htmlUrl(contributor.getHtmlUrl())
                .contributions(contributor.getContributions())
                .createdAt(contributor.getCreatedAt())
                .updatedAt(contributor.getUpdatedAt())
                .build();
    }

    private enum Scope {
        ORGANIZATION,
        TEAM,
        PROJECT
    }
}

