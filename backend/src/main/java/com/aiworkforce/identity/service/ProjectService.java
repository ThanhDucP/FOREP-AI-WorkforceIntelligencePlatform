package com.aiworkforce.identity.service;

import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.core.security.AccessPolicyService;
import com.aiworkforce.identity.dto.ProjectRequest;
import com.aiworkforce.identity.dto.ProjectResponse;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.entity.Project;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.OrganizationRepository;
import com.aiworkforce.identity.repository.ProjectRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final TeamRepository teamRepository;
    private final AccessPolicyService accessPolicyService;

    public List<ProjectResponse> getProjectsByTeam(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        accessPolicyService.ensureTeamAccess(team);
        return projectRepository.findByTeamId(teamId).stream().map(this::mapToResponse).toList();
    }

    public List<ProjectResponse> getProjectsByOrganization(UUID organizationId) {
        if (!accessPolicyService.isAdmin(accessPolicyService.currentEmployee())) {
            throw new BusinessException("Only admins can view all organization projects");
        }
        return projectRepository.findByOrganizationId(organizationId).stream().map(this::mapToResponse).toList();
    }

    public ProjectResponse getProject(UUID id) {
        Project project = findProject(id);
        accessPolicyService.ensureProjectAccess(project);
        return mapToResponse(project);
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        Project project = new Project();
        apply(project, request);
        accessPolicyService.ensureProjectManage(project);
        return mapToResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse updateProject(UUID id, ProjectRequest request) {
        Project project = findProject(id);
        apply(project, request);
        accessPolicyService.ensureProjectManage(project);
        return mapToResponse(projectRepository.save(project));
    }

    private Project findProject(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private void apply(Project project, ProjectRequest request) {
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        if (team.getOrganization() == null || !team.getOrganization().getId().equals(organization.getId())) {
            throw new BusinessException("Project team must belong to the selected organization");
        }

        if (request.getGithubRepository() != null && !request.getGithubRepository().isBlank()) {
            String owner = request.getGithubRepository().split("/")[0];
            if (organization.getGithubOrganization() != null
                    && !organization.getGithubOrganization().isBlank()
                    && !organization.getGithubOrganization().equalsIgnoreCase(owner)) {
                throw new BusinessException("GitHub repository must belong to the organization's GitHub account");
            }
        }

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setActive(request.isActive());
        project.setOrganization(organization);
        project.setTeam(team);
        project.setGithubRepository(blankToNull(request.getGithubRepository()));
        project.setJiraDomain(normalizeDomain(request.getJiraDomain()));
        project.setJiraProjectKey(blankToNull(request.getJiraProjectKey()));
    }

    private String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank()) return null;
        return domain.replace("https://", "").replace("http://", "").replaceAll("/+$", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .active(project.isActive())
                .organizationId(project.getOrganization().getId())
                .organizationName(project.getOrganization().getName())
                .teamId(project.getTeam().getId())
                .teamName(project.getTeam().getName())
                .githubRepository(project.getGithubRepository())
                .jiraDomain(project.getJiraDomain())
                .jiraProjectKey(project.getJiraProjectKey())
                .build();
    }
}
