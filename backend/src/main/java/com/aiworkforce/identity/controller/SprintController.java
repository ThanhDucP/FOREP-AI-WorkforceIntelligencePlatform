package com.aiworkforce.identity.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.dto.SprintResponse;
import com.aiworkforce.identity.service.SprintService;
import com.aiworkforce.core.security.ReadOnlyScopeGuard;
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
    private final ReadOnlyScopeGuard readOnlyScopeGuard;

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

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<SprintResponse>> createSprint(@Valid @RequestBody SprintResponse request) {
        readOnlyScopeGuard.block("CREATE_SPRINT", "Sprint", null);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<SprintResponse>> updateSprint(@PathVariable UUID id, @Valid @RequestBody SprintResponse request) {
        readOnlyScopeGuard.block("UPDATE_SPRINT", "Sprint", id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteSprint(@PathVariable UUID id) {
        readOnlyScopeGuard.block("DELETE_SPRINT", "Sprint", id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
