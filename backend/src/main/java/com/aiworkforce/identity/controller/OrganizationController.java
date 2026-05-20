package com.aiworkforce.identity.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.dto.OrganizationRequest;
import com.aiworkforce.identity.dto.OrganizationResponse;
import com.aiworkforce.identity.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> getAllOrganizations() {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getAllOrganizations()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganizationById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getOrganizationById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(@Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.createOrganization(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateOrganization(@PathVariable UUID id, @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.updateOrganization(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(@PathVariable UUID id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
