package com.aiworkforce.integration.controller;

import com.aiworkforce.core.enums.MappingStatus;
import com.aiworkforce.core.pagination.PaginationResponse;
import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.integration.dto.ChangeIdentityMappingRequest;
import com.aiworkforce.integration.dto.ExternalIdentityResponse;
import com.aiworkforce.integration.dto.IdentityMappingResponse;
import com.aiworkforce.integration.dto.IdentityMappingSummaryResponse;
import com.aiworkforce.integration.dto.ManualIdentityMappingRequest;
import com.aiworkforce.integration.service.IdentityMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IdentityMappingController {

    private final IdentityMappingService identityMappingService;

    @GetMapping("/external-members/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<PaginationResponse<ExternalIdentityResponse>>> getExternalMembers(
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(identityMappingService.getExternalMembers(organizationId, page, size)));
    }

    @GetMapping("/identity-mappings/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<PaginationResponse<IdentityMappingResponse>>> getMappings(
            @PathVariable UUID organizationId,
            @RequestParam(required = false) MappingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(identityMappingService.getMappings(organizationId, status, page, size)));
    }

    @GetMapping("/identity-mappings/organization/{organizationId}/summary")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<IdentityMappingSummaryResponse>> getSummary(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(identityMappingService.getSummary(organizationId)));
    }

    @PostMapping("/identity-mappings/{mappingId}/confirm")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<IdentityMappingResponse>> confirm(@PathVariable UUID mappingId) {
        return ResponseEntity.ok(ApiResponse.success(identityMappingService.confirmMapping(mappingId)));
    }

    @PostMapping("/identity-mappings/{mappingId}/change")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<IdentityMappingResponse>> change(
            @PathVariable UUID mappingId,
            @RequestBody ChangeIdentityMappingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(identityMappingService.changeMapping(mappingId, request)));
    }

    @PostMapping("/identity-mappings/{mappingId}/mark-unmatched")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<IdentityMappingResponse>> markUnmatched(@PathVariable UUID mappingId) {
        return ResponseEntity.ok(ApiResponse.success(identityMappingService.markUnmatched(mappingId)));
    }

    @PostMapping("/identity-mappings/{mappingId}/create-employee")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<IdentityMappingResponse>> createEmployee(@PathVariable UUID mappingId) {
        return ResponseEntity.ok(ApiResponse.success(identityMappingService.createEmployeeFromMapping(mappingId)));
    }

    @PostMapping("/identity-mappings/manual")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<IdentityMappingResponse>> manual(@RequestBody ManualIdentityMappingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(identityMappingService.createManualMapping(request)));
    }
}
