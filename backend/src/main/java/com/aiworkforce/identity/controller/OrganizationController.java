package com.aiworkforce.identity.controller;

import com.aiworkforce.core.enums.AccountStatus;
import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.dto.EmployeeResponse;
import com.aiworkforce.identity.dto.OrganizationRequest;
import com.aiworkforce.identity.dto.OrganizationResponse;
import com.aiworkforce.identity.service.EmployeeService;
import com.aiworkforce.identity.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final EmployeeService employeeService;

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

    @GetMapping("/{id}/users")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getOrganizationUsers(
            @PathVariable UUID id,
            @RequestParam(required = false) AccountStatus accountStatus
    ) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getEmployeesByOrganization(id, accountStatus)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(@Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.createOrganization(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateOrganization(@PathVariable UUID id, @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.updateOrganization(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(@PathVariable UUID id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}