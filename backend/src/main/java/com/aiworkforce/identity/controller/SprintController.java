package com.aiworkforce.identity.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.dto.SprintResponse;
import com.aiworkforce.identity.service.SprintService;
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
@RequestMapping("/api/v1/sprints")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SprintResponse>>> getAllSprints() {
        return ResponseEntity.ok(ApiResponse.success(sprintService.getAllSprints()));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<SprintResponse>> getActiveSprint() {
        return ResponseEntity.ok(ApiResponse.success(sprintService.getActiveSprint()));
    }

    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<SprintResponse>>> getSprintsByOrganization(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(sprintService.getSprintsByOrganization(organizationId)));
    }

    @GetMapping("/organization/{organizationId}/active")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<SprintResponse>> getActiveSprintByOrganization(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(sprintService.getActiveSprintByOrganization(organizationId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SprintResponse>> getSprintById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(sprintService.getSprintById(id)));
    }
}
