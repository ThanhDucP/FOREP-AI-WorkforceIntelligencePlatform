package com.aiworkforce.identity.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.dto.TeamRequest;
import com.aiworkforce.identity.dto.TeamResponse;
import com.aiworkforce.identity.service.TeamService;
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

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getAllTeams() {
        return ResponseEntity.ok(ApiResponse.success(teamService.getAllTeams()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeamById(id)));
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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(@PathVariable UUID id) {
        teamService.deleteTeam(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
