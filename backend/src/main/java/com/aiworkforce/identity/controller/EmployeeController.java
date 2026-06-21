package com.aiworkforce.identity.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.dto.EmployeeRequest;
import com.aiworkforce.identity.dto.EmployeeResponse;
import com.aiworkforce.identity.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getCurrentEmployeeProfile()));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateProfile(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.updateProfile(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getAllEmployees() {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getAllEmployees()));
    }

    @GetMapping("/team/{teamId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getEmployeesByTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getEmployeesByTeam(teamId)));
    }

    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getEmployeesByOrganization(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getEmployeesByOrganization(organizationId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER') or @employeeService.getCurrentEmployee().getId().equals(#id)")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getEmployeeById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(@PathVariable UUID id, @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.updateEmployee(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable UUID id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
