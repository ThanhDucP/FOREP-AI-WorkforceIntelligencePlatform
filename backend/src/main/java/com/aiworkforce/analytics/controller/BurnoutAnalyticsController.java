package com.aiworkforce.analytics.controller;

import com.aiworkforce.analytics.dto.BurnoutResponse;
import com.aiworkforce.analytics.service.BurnoutAnalyticsService;
import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/burnout")
@RequiredArgsConstructor
public class BurnoutAnalyticsController {
    private final BurnoutAnalyticsService burnoutAnalyticsService;
    private final EmployeeService employeeService;

    @GetMapping("/my-burnout")
    public ResponseEntity<ApiResponse<BurnoutResponse>> getMyBurnout() {
        Employee current = employeeService.getCurrentEmployee();
        return ResponseEntity.ok(ApiResponse.success(burnoutAnalyticsService.getEmployeeBurnout(current.getId())));
    }

    @GetMapping("/users/{employeeId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER') or @employeeService.getCurrentEmployee().getId().equals(#employeeId)")
    public ResponseEntity<ApiResponse<BurnoutResponse>> getEmployeeBurnout(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(burnoutAnalyticsService.getEmployeeBurnout(employeeId)));
    }

    @GetMapping("/teams/{teamId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<BurnoutResponse>> getTeamBurnout(@PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.success(burnoutAnalyticsService.getTeamBurnout(teamId)));
    }
}
