package com.aiworkforce.analytics.controller;

import com.aiworkforce.analytics.dto.AnalyticsDashboardResponse;
import com.aiworkforce.analytics.dto.AnalyticsSummaryResponse;
import com.aiworkforce.analytics.service.AnalyticsDashboardService;
import com.aiworkforce.analytics.service.AnalyticsSummaryService;
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
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsSummaryController {
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AnalyticsDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(analyticsDashboardService.getDashboard()));
    }

    private final AnalyticsSummaryService analyticsSummaryService;
    private final AnalyticsDashboardService analyticsDashboardService;
    private final EmployeeService employeeService;

    @GetMapping("/summary/my-summary")
    public ResponseEntity<ApiResponse<AnalyticsSummaryResponse>> getMySummary() {
        Employee current = employeeService.getCurrentEmployee();
        return ResponseEntity.ok(ApiResponse.success(analyticsSummaryService.getEmployeeSummary(current.getId())));
    }

    @GetMapping("/summary/users/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @employeeService.getCurrentEmployee().getId().equals(#employeeId)")
    public ResponseEntity<ApiResponse<AnalyticsSummaryResponse>> getEmployeeSummary(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(analyticsSummaryService.getEmployeeSummary(employeeId)));
    }

    @GetMapping("/summary/teams/{teamId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AnalyticsSummaryResponse>> getTeamSummary(@PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.success(analyticsSummaryService.getTeamSummary(teamId)));
    }
}
