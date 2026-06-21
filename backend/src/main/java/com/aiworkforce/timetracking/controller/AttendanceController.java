package com.aiworkforce.timetracking.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.timetracking.dto.AttendanceRequest;
import com.aiworkforce.timetracking.dto.AttendanceResponse;
import com.aiworkforce.timetracking.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkIn(@RequestBody AttendanceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.checkIn(request)));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/check-out")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkOut(@RequestBody AttendanceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.checkOut(request)));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getMyAttendanceHistory() {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getMyAttendanceHistory()));
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getEmployeeAttendanceHistory(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getEmployeeAttendanceHistory(employeeId)));
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    @GetMapping("/team/{teamId}")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getTeamAttendanceHistory(@PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getTeamAttendanceHistory(teamId)));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/managed-teams")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getManagedTeamAttendanceHistory() {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getManagedTeamAttendanceHistory()));
    }

    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getOrganizationAttendanceHistory(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getOrganizationAttendanceHistory(organizationId)));
    }
}
