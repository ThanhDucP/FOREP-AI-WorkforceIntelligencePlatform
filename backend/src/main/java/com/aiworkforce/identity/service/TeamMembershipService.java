package com.aiworkforce.identity.service;

import com.aiworkforce.core.enums.TeamMembershipStatus;
import com.aiworkforce.core.enums.RoleType;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.entity.TeamMembership;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.TeamMembershipRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamMembershipService {

    private final TeamMembershipRepository membershipRepository;
    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public TeamMembership requestJoinTeam(UUID employeeId, UUID teamId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        if (membershipRepository.existsByEmployeeIdAndTeamIdAndStatus(employeeId, teamId, TeamMembershipStatus.ACTIVE)
                || membershipRepository.existsByEmployeeIdAndTeamIdAndStatus(employeeId, teamId, TeamMembershipStatus.PENDING_LEAD_APPROVAL)) {
            throw new BusinessException("Employee already has an active or pending membership for this team");
        }

        TeamMembership membership = new TeamMembership();
        membership.setEmployee(employee);
        membership.setTeam(team);
        membership.setStatus(TeamMembershipStatus.PENDING_LEAD_APPROVAL);
        return membershipRepository.save(membership);
    }

    @Transactional
    public TeamMembership approveMembership(UUID membershipId, UUID leadEmployeeId) {
        TeamMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Team membership not found"));
        Employee lead = employeeRepository.findById(leadEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead employee not found"));

        Team team = membership.getTeam();
        if (!hasAdminRole(lead) && (team.getManager() == null || !team.getManager().getId().equals(lead.getId()))) {
            throw new BusinessException("Only the team lead can approve this membership");
        }

        membershipRepository.findByEmployeeIdAndStatus(membership.getEmployee().getId(), TeamMembershipStatus.ACTIVE)
                .ifPresent(active -> {
                    active.setStatus(TeamMembershipStatus.ENDED);
                    active.setEndedAt(LocalDateTime.now());
                    membershipRepository.save(active);
                });

        membership.setStatus(TeamMembershipStatus.ACTIVE);
        membership.setApprovedBy(lead);
        membership.setStartedAt(LocalDateTime.now());
        membership.setEndedAt(null);
        membership.getEmployee().setTeam(team);
        employeeRepository.save(membership.getEmployee());
        return membershipRepository.save(membership);
    }

    @Transactional
    public void endActiveMembership(UUID employeeId, UUID leadEmployeeId) {
        TeamMembership active = membershipRepository.findByEmployeeIdAndStatus(employeeId, TeamMembershipStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active team membership not found"));
        Employee lead = employeeRepository.findById(leadEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead employee not found"));

        if (!hasAdminRole(lead) && (active.getTeam().getManager() == null || !active.getTeam().getManager().getId().equals(lead.getId()))) {
            throw new BusinessException("Only the current team lead can end this membership");
        }

        active.setStatus(TeamMembershipStatus.ENDED);
        active.setEndedAt(LocalDateTime.now());
        active.getEmployee().setTeam(null);
        employeeRepository.save(active.getEmployee());
        membershipRepository.save(active);
    }

    public boolean hasActiveTeamAccess(UUID employeeId, UUID teamId) {
        return membershipRepository.existsByEmployeeIdAndTeamIdAndStatus(employeeId, teamId, TeamMembershipStatus.ACTIVE);
    }

    public Optional<TeamMembership> getActiveMembership(UUID employeeId) {
        return membershipRepository.findByEmployeeIdAndStatus(employeeId, TeamMembershipStatus.ACTIVE);
    }

    private boolean hasAdminRole(Employee employee) {
        return employee.getAccount() != null
                && employee.getAccount().getRole() != null
                && employee.getAccount().getRole().getName() == RoleType.ADMIN;
    }
}
