package com.aiworkforce.identity.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.dto.SprintResponse;
import com.aiworkforce.identity.service.SprintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<SprintResponse>>> getSprintsByOrganization(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(sprintService.getSprintsByOrganization(organizationId)));
    }

    @GetMapping("/organization/{organizationId}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<SprintResponse>> getActiveSprintByOrganization(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(sprintService.getActiveSprintByOrganization(organizationId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SprintResponse>> getSprintById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(sprintService.getSprintById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<SprintResponse>> createSprint(@Valid @RequestBody SprintResponse request) {
        return ResponseEntity.ok(ApiResponse.success(sprintService.createSprint(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<SprintResponse>> updateSprint(@PathVariable UUID id, @Valid @RequestBody SprintResponse request) {
        return ResponseEntity.ok(ApiResponse.success(sprintService.updateSprint(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSprint(@PathVariable UUID id) {
        sprintService.deleteSprint(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
