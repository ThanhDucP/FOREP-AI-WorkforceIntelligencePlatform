package com.aiworkforce.identity.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.dto.ProjectRequest;
import com.aiworkforce.identity.dto.ProjectResponse;
import com.aiworkforce.identity.service.ProjectService;
import com.aiworkforce.core.security.ReadOnlyScopeGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ReadOnlyScopeGuard readOnlyScopeGuard;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProject(id)));
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjectsByTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProjectsByTeam(teamId)));
    }

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjectsByOrganization(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProjectsByOrganization(organizationId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(@Valid @RequestBody ProjectRequest request) {
        readOnlyScopeGuard.block("CREATE_PROJECT", "Project", null);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(@PathVariable UUID id, @Valid @RequestBody ProjectRequest request) {
        readOnlyScopeGuard.block("UPDATE_PROJECT", "Project", id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
