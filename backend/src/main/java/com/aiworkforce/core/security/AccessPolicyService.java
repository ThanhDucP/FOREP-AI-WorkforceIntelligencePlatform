package com.aiworkforce.core.security;

import com.aiworkforce.core.enums.RoleType;
import com.aiworkforce.core.exception.ForbiddenException;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.entity.Project;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.identity.service.EmployeeService;
import com.aiworkforce.identity.service.TeamMembershipService;
import com.aiworkforce.integration.entity.TaskIntegrationConfig;
import com.aiworkforce.task.entity.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessPolicyService {

    private final EmployeeService employeeService;
    private final TeamRepository teamRepository;
    private final TeamMembershipService membershipService;

    public Employee currentEmployee() {
        return employeeService.getCurrentEmployee();
    }

    public boolean isSystemAdmin(Employee employee) {
        RoleType role = roleOf(employee);
        return role == RoleType.SYSTEM_ADMIN || role == RoleType.ADMIN;
    }

    public boolean isAdmin(Employee employee) {
        return isSystemAdmin(employee);
    }

    public boolean isDirector(Employee employee) {
        return roleOf(employee) == RoleType.DIRECTOR;
    }

    public boolean isManager(Employee employee) {
        return roleOf(employee) == RoleType.MANAGER;
    }

    public boolean isTeamLead(Employee employee, Team team) {
        return team != null && team.getManager() != null && team.getManager().getId().equals(employee.getId());
    }

    public boolean sameOrganization(Employee employee, Organization organization) {
        Organization currentOrg = organizationOf(employee);
        return currentOrg != null && organization != null && currentOrg.getId().equals(organization.getId());
    }

    public boolean canAccessOrganization(Employee employee, Organization organization) {
        return isSystemAdmin(employee) || sameOrganization(employee, organization);
    }

    public boolean canManageOrganization(Employee employee, Organization organization) {
        return isSystemAdmin(employee) || (isDirector(employee) && sameOrganization(employee, organization));
    }

    public boolean canAccessTeam(Employee employee, Team team) {
        if (team == null || employee == null) return false;
        UUID teamId = team.getId();
        return (isDirector(employee) && sameOrganization(employee, team.getOrganization()))
                || isTeamLead(employee, team)
                || membershipService.hasActiveTeamAccess(employee.getId(), teamId);
    }

    public boolean canManageTeam(Employee employee, Team team) {
        return team != null && employee != null && ((isDirector(employee) && sameOrganization(employee, team.getOrganization()))
                || isTeamLead(employee, team));
    }

    public boolean canAccessProject(Employee employee, Project project) {
        return project != null && canAccessTeam(employee, project.getTeam());
    }

    public boolean canManageProject(Employee employee, Project project) {
        return project != null && canManageTeam(employee, project.getTeam());
    }

    public boolean canAccessTask(Employee employee, Task task) {
        if (task == null) return false;
        if (task.getAssignee() != null && task.getAssignee().getId().equals(employee.getId())) {
            return true;
        }
        if (task.getProject() != null) {
            return canAccessProject(employee, task.getProject());
        }
        return canAccessTeam(employee, task.getTeam());
    }

    public void ensureOrganizationAccess(Organization organization) {
        if (!canAccessOrganization(currentEmployee(), organization)) {
            throw new ForbiddenException("Current user does not have access to this organization");
        }
    }

    public void ensureOrganizationManage(Organization organization) {
        if (!canManageOrganization(currentEmployee(), organization)) {
            throw new ForbiddenException("Only system admins or directors in this organization can manage it");
        }
    }

    public void ensureTeamAccess(Team team) {
        if (!canAccessTeam(currentEmployee(), team)) {
            throw new ForbiddenException("Current user does not have access to this team");
        }
    }

    public void ensureTeamManage(Team team) {
        if (!canManageTeam(currentEmployee(), team)) {
            throw new ForbiddenException("Only organization directors or the assigned team manager can manage this team");
        }
    }

    public void ensureProjectAccess(Project project) {
        if (!canAccessProject(currentEmployee(), project)) {
            throw new ForbiddenException("Current user does not have access to this project");
        }
    }

    public void ensureProjectManage(Project project) {
        if (!canManageProject(currentEmployee(), project)) {
            throw new ForbiddenException("Only organization directors or the assigned team manager can manage this project");
        }
    }

    public void ensureTaskAccess(Task task) {
        if (!canAccessTask(currentEmployee(), task)) {
            throw new ForbiddenException("Current user does not have access to this task");
        }
    }

    public void ensureIntegrationAccess(TaskIntegrationConfig config) {
        if (config.getProject() != null) {
            ensureProjectAccess(config.getProject());
        } else {
            ensureTeamAccess(config.getTeam());
        }
    }

    public void ensureIntegrationManage(TaskIntegrationConfig config) {
        if (config.getProject() != null) {
            ensureProjectManage(config.getProject());
        } else {
            ensureTeamManage(config.getTeam());
        }
    }

    public boolean managesAnyTeam(Employee employee) {
        return isDirector(employee) || isManager(employee) || !teamRepository.findByManagerId(employee.getId()).isEmpty();
    }

    private RoleType roleOf(Employee employee) {
        return employee != null
                && employee.getAccount() != null
                && employee.getAccount().getRole() != null
                ? employee.getAccount().getRole().getName()
                : null;
    }

    private Organization organizationOf(Employee employee) {
        if (employee == null) return null;
        if (employee.getOrganization() != null) return employee.getOrganization();
        return employee.getTeam() != null ? employee.getTeam().getOrganization() : null;
    }
}