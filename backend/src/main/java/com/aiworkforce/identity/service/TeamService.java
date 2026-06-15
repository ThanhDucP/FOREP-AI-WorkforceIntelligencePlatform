package com.aiworkforce.identity.service;

import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.dto.EmployeeResponse;
import com.aiworkforce.identity.dto.TeamRequest;
import com.aiworkforce.identity.dto.TeamResponse;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.OrganizationRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final EmployeeRepository employeeRepository;
    private final OrganizationRepository organizationRepository;
    private final EmployeeService employeeService;
    private final TeamMembershipService membershipService;

    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TeamResponse> getTeamsByOrganization(UUID organizationId) {
        return teamRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TeamResponse> getTeamsByManager(UUID managerId) {
        if (!employeeRepository.existsById(managerId)) {
            throw new ResourceNotFoundException("Manager not found with id: " + managerId);
        }

        return teamRepository.findByManagerId(managerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TeamResponse> getMyManagedTeams() {
        Employee currentEmployee = employeeService.getCurrentEmployee();
        return getTeamsByManager(currentEmployee.getId());
    }

    public TeamResponse getTeamById(UUID id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + id));
        return mapToResponse(team);
    }

    public List<EmployeeResponse> getTeamMembers(UUID id) {
        if (!teamRepository.existsById(id)) {
            throw new ResourceNotFoundException("Team not found with id: " + id);
        }

        return employeeRepository.findByTeamId(id).stream()
                .map(employeeService::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TeamResponse createTeam(TeamRequest request) {
        Team team = new Team();
        team.setName(request.getName());
        team.setDescription(request.getDescription());

        if (request.getOrganizationId() != null) {
            Organization org = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
            team.setOrganization(org);
        }

        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager employee not found"));
            team.setManager(manager);
        }

        Team savedTeam = teamRepository.save(team);
        
        // If manager is set, update manager's team to the newly created team
        if (savedTeam.getManager() != null) {
            Employee manager = savedTeam.getManager();
            manager.setTeam(savedTeam);
            employeeRepository.save(manager);
        }

        return mapToResponse(savedTeam);
    }

    @Transactional
    public TeamResponse updateTeam(UUID id, TeamRequest request) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        team.setName(request.getName());
        team.setDescription(request.getDescription());

        if (request.getOrganizationId() != null) {
            Organization org = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
            team.setOrganization(org);
        } else {
            team.setOrganization(null);
        }

        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager employee not found"));
            team.setManager(manager);
            
            // Auto update manager's team
            manager.setTeam(team);
            employeeRepository.save(manager);
        } else {
            team.setManager(null);
        }

        return mapToResponse(teamRepository.save(team));
    }

    @Transactional
    public void assignEmployeeToTeam(UUID employeeId, UUID teamId) {
        if (teamId == null) {
            Employee currentLead = employeeService.getCurrentEmployee();
            membershipService.endActiveMembership(employeeId, currentLead.getId());
            return;
        }

        membershipService.requestJoinTeam(employeeId, teamId);
    }

    @Transactional
    public void deleteTeam(UUID id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        
        // Clear references from employees belonging to this team
        List<Employee> employees = employeeRepository.findAll().stream()
                .filter(e -> e.getTeam() != null && e.getTeam().getId().equals(id))
                .collect(Collectors.toList());
        for (Employee emp : employees) {
            emp.setTeam(null);
            employeeRepository.save(emp);
        }

        teamRepository.delete(team);
    }

    public TeamResponse mapToResponse(Team team) {
        if (team == null) return null;
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .organizationId(team.getOrganization() != null ? team.getOrganization().getId() : null)
                .organizationName(team.getOrganization() != null ? team.getOrganization().getName() : null)
                .managerId(team.getManager() != null ? team.getManager().getId() : null)
                .managerName(team.getManager() != null ? (team.getManager().getFirstName() + " " + team.getManager().getLastName()) : null)
                .build();
    }
}
