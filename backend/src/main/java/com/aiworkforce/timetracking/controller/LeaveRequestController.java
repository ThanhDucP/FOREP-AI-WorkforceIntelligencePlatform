package com.aiworkforce.timetracking.controller;

import com.aiworkforce.core.response.ApiResponse;
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

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getAllLeaveRequests() {
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.getAllLeaveRequests()));
    }
}
