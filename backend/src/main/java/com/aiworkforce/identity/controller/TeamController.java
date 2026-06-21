package com.aiworkforce.identity.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.dto.EmployeeResponse;
import com.aiworkforce.identity.dto.TeamRequest;
import com.aiworkforce.identity.dto.TeamResponse;
import com.aiworkforce.identity.service.TeamService;
import com.aiworkforce.identity.service.EmployeeService;
import com.aiworkforce.identity.service.TeamMembershipService;
import com.aiworkforce.core.security.ReadOnlyScopeGuard;
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
    private final ReadOnlyScopeGuard readOnlyScopeGuard;

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getAllTeams() {
        return ResponseEntity.ok(ApiResponse.success(teamService.getAllTeams()));
    }

    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeamsByOrganization(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeamsByOrganization(organizationId)));
    }

    @GetMapping("/managed-by/{managerId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeamsByManager(@PathVariable UUID managerId) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeamsByManager(managerId)));
    }

    @GetMapping("/my-managed-teams")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getMyManagedTeams() {
        return ResponseEntity.ok(ApiResponse.success(teamService.getMyManagedTeams()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeamById(id)));
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getTeamMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeamMembers(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(@Valid @RequestBody TeamRequest request) {
        readOnlyScopeGuard.block("CREATE_TEAM", "Team", null);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(@PathVariable UUID id, @Valid @RequestBody TeamRequest request) {
        readOnlyScopeGuard.block("UPDATE_TEAM", "Team", id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{id}/assign-employee")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> assignEmployeeToTeam(@PathVariable("id") UUID teamId, @RequestParam UUID employeeId) {
        readOnlyScopeGuard.block("ASSIGN_EMPLOYEE_TO_TEAM", "Team", teamId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/members/request")
    public ResponseEntity<ApiResponse<UUID>> requestJoinTeam(@PathVariable("id") UUID teamId) {
        readOnlyScopeGuard.block("REQUEST_JOIN_TEAM", "Team", teamId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/memberships/{membershipId}/approve")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<UUID>> approveMembership(@PathVariable UUID membershipId) {
        readOnlyScopeGuard.block("APPROVE_TEAM_MEMBERSHIP", "TeamMembership", membershipId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/members/{employeeId}/end-active")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> endActiveMembership(@PathVariable UUID employeeId) {
        readOnlyScopeGuard.block("END_ACTIVE_TEAM_MEMBERSHIP", "Employee", employeeId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(@PathVariable UUID id) {
        readOnlyScopeGuard.block("DELETE_TEAM", "Team", id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}


