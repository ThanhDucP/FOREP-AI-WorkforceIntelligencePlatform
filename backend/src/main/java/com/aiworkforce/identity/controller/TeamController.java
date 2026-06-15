package com.aiworkforce.identity.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.dto.EmployeeResponse;
import com.aiworkforce.identity.dto.TeamRequest;
import com.aiworkforce.identity.dto.TeamResponse;
import com.aiworkforce.identity.service.TeamService;
import com.aiworkforce.identity.service.EmployeeService;
import com.aiworkforce.identity.service.TeamMembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final TeamMembershipService membershipService;
    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getAllTeams() {
        return ResponseEntity.ok(ApiResponse.success(teamService.getAllTeams()));
    }

    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeamsByOrganization(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeamsByOrganization(organizationId)));
    }

    @GetMapping("/managed-by/{managerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeamsByManager(@PathVariable UUID managerId) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeamsByManager(managerId)));
    }

    @GetMapping("/my-managed-teams")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getMyManagedTeams() {
        return ResponseEntity.ok(ApiResponse.success(teamService.getMyManagedTeams()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeamById(id)));
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getTeamMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeamMembers(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(@Valid @RequestBody TeamRequest request) {
        return ResponseEntity.ok(ApiResponse.success(teamService.createTeam(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(@PathVariable UUID id, @Valid @RequestBody TeamRequest request) {
        return ResponseEntity.ok(ApiResponse.success(teamService.updateTeam(id, request)));
    }

    @PutMapping("/{id}/assign-employee")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> assignEmployeeToTeam(@PathVariable("id") UUID teamId, @RequestParam UUID employeeId) {
        teamService.assignEmployeeToTeam(employeeId, teamId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/members/request")
    public ResponseEntity<ApiResponse<UUID>> requestJoinTeam(@PathVariable("id") UUID teamId) {
        UUID employeeId = employeeService.getCurrentEmployee().getId();
        return ResponseEntity.ok(ApiResponse.success(membershipService.requestJoinTeam(employeeId, teamId).getId()));
    }

    @PostMapping("/memberships/{membershipId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<UUID>> approveMembership(@PathVariable UUID membershipId) {
        UUID leadId = employeeService.getCurrentEmployee().getId();
        return ResponseEntity.ok(ApiResponse.success(membershipService.approveMembership(membershipId, leadId).getId()));
    }

    @PostMapping("/members/{employeeId}/end-active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> endActiveMembership(@PathVariable UUID employeeId) {
        UUID leadId = employeeService.getCurrentEmployee().getId();
        membershipService.endActiveMembership(employeeId, leadId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(@PathVariable UUID id) {
        teamService.deleteTeam(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
