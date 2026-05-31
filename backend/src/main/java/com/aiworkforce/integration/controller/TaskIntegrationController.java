package com.aiworkforce.integration.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.integration.dto.TaskIntegrationConfigRequest;
import com.aiworkforce.integration.dto.TaskIntegrationConfigResponse;
import com.aiworkforce.integration.service.TaskIntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.aiworkforce.integration.dto.IntegrationConnectRequest;
import com.aiworkforce.integration.dto.IntegrationConnectResponse;

@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
public class TaskIntegrationController {

    private final TaskIntegrationService integrationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TaskIntegrationConfigResponse>> createConfig(
            @Valid @RequestBody TaskIntegrationConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.success(integrationService.createConfig(request)));
    }

    @PostMapping("/connect")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<IntegrationConnectResponse>> connect(
            @Valid @RequestBody IntegrationConnectRequest request) {
        IntegrationConnectResponse resp = integrationService.connectWithKey(request);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @GetMapping("/team/{teamId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<TaskIntegrationConfigResponse>>> getConfigsByTeam(
            @PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.success(integrationService.getConfigsByTeam(teamId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TaskIntegrationConfigResponse>> updateConfig(
            @PathVariable UUID id, 
            @RequestBody TaskIntegrationConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.success(integrationService.updateConfig(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable UUID id) {
        integrationService.deleteConfig(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/sync")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<String>> syncTasks(@PathVariable UUID id) {
        integrationService.syncTasks(id);
        return ResponseEntity.ok(ApiResponse.success("Sync completed successfully"));
    }
}
