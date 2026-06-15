package com.aiworkforce.core.security;

import com.aiworkforce.core.enums.RoleType;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.identity.entity.Employee;
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

    public boolean isAdmin(Employee employee) {
        return employee.getAccount() != null
                && employee.getAccount().getRole() != null
                && employee.getAccount().getRole().getName() == RoleType.ADMIN;
    }

    public boolean isTeamLead(Employee employee, Team team) {
        return team != null && team.getManager() != null && team.getManager().getId().equals(employee.getId());
    }

    public boolean canAccessTeam(Employee employee, Team team) {
        if (team == null) return false;
        UUID teamId = team.getId();
        return isAdmin(employee)
                || isTeamLead(employee, team)
                || membershipService.hasActiveTeamAccess(employee.getId(), teamId);
    }

    public boolean canManageTeam(Employee employee, Team team) {
        return isAdmin(employee) || isTeamLead(employee, team);
    }

    public boolean canAccessProject(Employee employee, Project project) {
        return project != null && canAccessTeam(employee, project.getTeam());
    }

    public boolean canManageProject(Employee employee, Project project) {
        return project != null && canManageTeam(employee, project.getTeam());
    }

    public boolean canAccessTask(Employee employee, Task task) {
        if (task == null) return false;
        if (task.getProject() != null) {
            return canAccessProject(employee, task.getProject());
        }
        return canAccessTeam(employee, task.getTeam());
    }

    public void ensureTeamAccess(Team team) {
        if (!canAccessTeam(currentEmployee(), team)) {
            throw new BusinessException("Current user does not have access to this team");
        }
    }

    public void ensureTeamManage(Team team) {
        if (!canManageTeam(currentEmployee(), team)) {
            throw new BusinessException("Only organization admins or the team lead can manage this team");
        }
    }

    public void ensureProjectAccess(Project project) {
        if (!canAccessProject(currentEmployee(), project)) {
            throw new BusinessException("Current user does not have access to this project");
        }
    }

    public void ensureProjectManage(Project project) {
        if (!canManageProject(currentEmployee(), project)) {
            throw new BusinessException("Only organization admins or the team lead can manage this project");
        }
    }

    public void ensureTaskAccess(Task task) {
        if (!canAccessTask(currentEmployee(), task)) {
            throw new BusinessException("Current user does not have access to this task");
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
        return !teamRepository.findByManagerId(employee.getId()).isEmpty();
    }
}
