package com.aiworkforce.identity.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.dto.EmployeeInvitationResponse;
import com.aiworkforce.identity.dto.EmployeeRequest;
import com.aiworkforce.identity.dto.EmployeeResponse;
import com.aiworkforce.identity.service.EmployeeAccountService;
import com.aiworkforce.identity.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeAccountService employeeAccountService;

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

    @PostMapping("/{id}/invite")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeInvitationResponse>> invite(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(employeeAccountService.sendInvite(id)));
    }

    @PostMapping("/{id}/reinvite")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeInvitationResponse>> reinvite(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(employeeAccountService.reinvite(id)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeInvitationResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(employeeAccountService.activate(id)));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeInvitationResponse>> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(employeeAccountService.deactivate(id)));
    }

    @PostMapping("/activate-by-token")
    public ResponseEntity<ApiResponse<EmployeeInvitationResponse>> activateByToken(@RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.success(employeeAccountService.activateByToken(token)));
    }
}
