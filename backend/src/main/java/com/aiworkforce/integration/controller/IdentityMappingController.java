package com.aiworkforce.integration.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.integration.dto.ExternalIdentityResponse;
import com.aiworkforce.integration.dto.IdentityMappingResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IdentityMappingController {

    private final IdentityMappingService identityMappingService;

    @GetMapping("/external-members/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<ExternalIdentityResponse>>> getExternalMembers(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(identityMappingService.getExternalMembers(organizationId)));
    }

    @GetMapping("/identity-mappings/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<IdentityMappingResponse>>> getMappings(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(identityMappingService.getMappings(organizationId)));
    }

    @PostMapping("/identity-mappings/{mappingId}/confirm")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<IdentityMappingResponse>> confirm(@PathVariable UUID mappingId) {
        return ResponseEntity.ok(ApiResponse.success(identityMappingService.confirmMapping(mappingId)));
    }

    @PostMapping("/identity-mappings/manual")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<IdentityMappingResponse>> manual(@RequestBody ManualIdentityMappingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(identityMappingService.createManualMapping(request)));
    }
}