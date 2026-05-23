package com.aiworkforce.timetracking.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.core.enums.LeaveStatus;
import com.aiworkforce.timetracking.dto.LeaveRequestRequest;
import com.aiworkforce.timetracking.dto.LeaveRequestResponse;
import com.aiworkforce.timetracking.service.LeaveRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> createLeaveRequest(@Valid @RequestBody LeaveRequestRequest request) {
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.createLeaveRequest(request)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> approveLeaveRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.approveLeaveRequest(id)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> rejectLeaveRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.rejectLeaveRequest(id)));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getMyLeaveRequests() {
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.getMyLeaveRequests()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @employeeService.getCurrentEmployee().getId().equals(#employeeId)")
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getEmployeeLeaveRequests(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.getEmployeeLeaveRequests(employeeId)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/team/{teamId}")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getTeamLeaveRequests(@PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.getTeamLeaveRequests(teamId)));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/managed-teams")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getManagedTeamLeaveRequests() {
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.getManagedTeamLeaveRequests()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getOrganizationLeaveRequests(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.getOrganizationLeaveRequests(organizationId)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getLeaveRequestsByStatus(@PathVariable LeaveStatus status) {
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.getLeaveRequestsByStatus(status)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getAllLeaveRequests() {
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.getAllLeaveRequests()));
    }
}
