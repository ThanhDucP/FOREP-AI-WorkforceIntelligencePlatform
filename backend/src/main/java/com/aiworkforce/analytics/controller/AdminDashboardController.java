package com.aiworkforce.analytics.controller;

import com.aiworkforce.analytics.dto.AdminDashboardResponse;
import com.aiworkforce.analytics.service.AdminDashboardService;
import com.aiworkforce.core.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboard() {
        AdminDashboardResponse response = adminDashboardService.getAdminDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
