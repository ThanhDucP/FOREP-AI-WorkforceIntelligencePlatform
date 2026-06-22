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
import com.aiworkforce.integration.dto.ProjectSourceLinkRequest;
import com.aiworkforce.integration.dto.ProjectSourceLinkResponse;
import com.aiworkforce.integration.entity.GithubRepositorySnapshot;
import com.aiworkforce.integration.entity.JiraProjectSnapshot;
import com.aiworkforce.integration.entity.ProjectSourceLink;
import com.aiworkforce.integration.repository.GithubRepositorySnapshotRepository;
import com.aiworkforce.integration.repository.JiraProjectSnapshotRepository;
import com.aiworkforce.integration.repository.ProjectSourceLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectSourceLinkService {

    private final ProjectSourceLinkRepository linkRepository;
    private final JiraProjectSnapshotRepository jiraProjectRepository;
    private final GithubRepositorySnapshotRepository githubRepository;
    private final OrganizationRepository organizationRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final AccessPolicyService accessPolicyService;

    @Transactional
    public ProjectSourceLinkResponse link(ProjectSourceLinkRequest request) {
        JiraProjectSnapshot jiraProject = jiraProjectRepository.findById(request.getJiraProjectSnapshotId())
                .orElseThrow(() -> new ResourceNotFoundException("Jira project snapshot not found"));
        GithubRepositorySnapshot githubRepositorySnapshot = githubRepository.findById(request.getGithubRepositorySnapshotId())
                .orElseThrow(() -> new ResourceNotFoundException("GitHub repository snapshot not found"));

        validateSameScope(jiraProject, githubRepositorySnapshot);
        if (jiraProject.getProject() != null) {
            accessPolicyService.ensureProjectManage(jiraProject.getProject());
        } else if (jiraProject.getTeam() != null) {
            accessPolicyService.ensureTeamManage(jiraProject.getTeam());
        } else {
            throw new BusinessException("Jira project snapshot has no team scope");
        }

        ProjectSourceLink link = linkRepository
                .findByJiraProjectIdAndGithubRepositoryId(jiraProject.getId(), githubRepositorySnapshot.getId())
                .orElseGet(ProjectSourceLink::new);
        link.setJiraProject(jiraProject);
        link.setGithubRepository(githubRepositorySnapshot);
        link.setProject(jiraProject.getProject() != null ? jiraProject.getProject() : githubRepositorySnapshot.getProject());
        link.setTeam(jiraProject.getTeam());
        link.setOrganization(jiraProject.getTeam().getOrganization());
        link.setNote(request.getNote());
        return map(linkRepository.save(link));
    }

    @Transactional(readOnly = true)
    public List<ProjectSourceLinkResponse> list(UUID organizationId, UUID teamId, UUID projectId) {
        int selected = (organizationId != null ? 1 : 0) + (teamId != null ? 1 : 0) + (projectId != null ? 1 : 0);
        if (selected != 1) {
            throw new BusinessException("Select exactly one scope: organizationId, teamId, or projectId");
        }
        if (organizationId != null) {
            Organization organization = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
            accessPolicyService.ensureOrganizationAccess(organization);
            return linkRepository.findByOrganizationIdOrderByUpdatedAtDesc(organizationId).stream().map(this::map).toList();
        }
        if (teamId != null) {
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
            accessPolicyService.ensureTeamAccess(team);
            return linkRepository.findByTeamIdOrderByUpdatedAtDesc(teamId).stream().map(this::map).toList();
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        accessPolicyService.ensureProjectAccess(project);
        return linkRepository.findByProjectIdOrderByUpdatedAtDesc(projectId).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public PaginationResponse<ProjectSourceLinkResponse> list(UUID organizationId, UUID teamId, UUID projectId, int page, int size) {
        return paginate(list(organizationId, teamId, projectId), page, size);
    }

    @Transactional
    public void delete(UUID id) {
        ProjectSourceLink link = linkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project source link not found"));
        if (link.getProject() != null) {
            accessPolicyService.ensureProjectManage(link.getProject());
        } else if (link.getTeam() != null) {
            accessPolicyService.ensureTeamManage(link.getTeam());
        } else {
            accessPolicyService.ensureOrganizationManage(link.getOrganization());
        }
        linkRepository.delete(link);
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

    private void validateSameScope(JiraProjectSnapshot jiraProject, GithubRepositorySnapshot githubRepositorySnapshot) {
        if (jiraProject.getTeam() == null || githubRepositorySnapshot.getTeam() == null) {
            throw new BusinessException("Both sources must belong to a team scope");
        }
        if (!jiraProject.getTeam().getId().equals(githubRepositorySnapshot.getTeam().getId())) {
            throw new BusinessException("Jira project and GitHub repository must belong to the same team scope");
        }
    }

    private ProjectSourceLinkResponse map(ProjectSourceLink link) {
        JiraProjectSnapshot jira = link.getJiraProject();
        GithubRepositorySnapshot github = link.getGithubRepository();
        return ProjectSourceLinkResponse.builder()
                .id(link.getId())
                .organizationId(link.getOrganization() != null ? link.getOrganization().getId() : null)
                .teamId(link.getTeam() != null ? link.getTeam().getId() : null)
                .projectId(link.getProject() != null ? link.getProject().getId() : null)
                .jiraProjectSnapshotId(jira != null ? jira.getId() : null)
                .jiraProjectName(jira != null ? jira.getName() : null)
                .jiraProjectKey(jira != null ? jira.getProjectKey() : null)
                .githubRepositorySnapshotId(github != null ? github.getId() : null)
                .githubRepositoryFullName(github != null ? github.getFullName() : null)
                .note(link.getNote())
                .createdAt(link.getCreatedAt())
                .updatedAt(link.getUpdatedAt())
                .build();
    }
}
