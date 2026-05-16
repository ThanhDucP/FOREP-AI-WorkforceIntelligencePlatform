package com.aiworkforce.analytics.controller;

import com.aiworkforce.analytics.dto.DashboardResponse;
import com.aiworkforce.analytics.service.DashboardAnalyticsService;
import com.aiworkforce.core.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardAnalyticsService dashboardAnalyticsService;

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<DashboardResponse>> getEmployeeDashboard(@PathVariable UUID employeeId) {
        DashboardResponse response = dashboardAnalyticsService.getEmployeeDashboard(employeeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
