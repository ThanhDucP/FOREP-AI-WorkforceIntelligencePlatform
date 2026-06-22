package com.aiworkforce.identity.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.dto.ProjectResponse;
import com.aiworkforce.identity.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

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
}
