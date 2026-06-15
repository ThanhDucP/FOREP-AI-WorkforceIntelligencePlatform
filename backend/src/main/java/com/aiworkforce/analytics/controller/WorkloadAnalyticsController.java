package com.aiworkforce.analytics.controller;

import com.aiworkforce.analytics.dto.WorkloadHistoryResponse;
import com.aiworkforce.analytics.service.WorkloadSnapshotService;
import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class WorkloadAnalyticsController {

    private final WorkloadSnapshotService workloadSnapshotService;
    private final EmployeeService employeeService;

    @GetMapping("/workload-history/{employeeId}")
    public ResponseEntity<ApiResponse<List<WorkloadHistoryResponse>>> getWorkloadHistory(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        return ResponseEntity.ok(ApiResponse.success(workloadSnapshotService.getWorkloadHistory(employeeId, startDate, endDate)));
    }

    @GetMapping("/workload-history/my-history")
    public ResponseEntity<ApiResponse<List<WorkloadHistoryResponse>>> getMyWorkloadHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Employee current = employeeService.getCurrentEmployee();
        return ResponseEntity.ok(ApiResponse.success(workloadSnapshotService.getWorkloadHistory(current.getId(), startDate, endDate)));
    }

    @GetMapping("/workload-history/team/{teamId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<WorkloadHistoryResponse>>> getTeamWorkloadHistory(
            @PathVariable UUID teamId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(ApiResponse.success(workloadSnapshotService.getTeamWorkloadHistory(teamId, startDate, endDate)));
    }

    @GetMapping("/workload-history/managed-teams")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<WorkloadHistoryResponse>>> getManagedTeamWorkloadHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(ApiResponse.success(workloadSnapshotService.getManagedTeamWorkloadHistory(startDate, endDate)));
    }

    @GetMapping("/workload-history/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<WorkloadHistoryResponse>>> getOrganizationWorkloadHistory(
            @PathVariable UUID organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(ApiResponse.success(workloadSnapshotService.getOrganizationWorkloadHistory(organizationId, startDate, endDate)));
    }

}
