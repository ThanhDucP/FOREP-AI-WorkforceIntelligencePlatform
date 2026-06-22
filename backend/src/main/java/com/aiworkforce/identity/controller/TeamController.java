package com.aiworkforce.identity.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.dto.EmployeeResponse;
import com.aiworkforce.identity.dto.TeamResponse;
import com.aiworkforce.identity.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

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
}
