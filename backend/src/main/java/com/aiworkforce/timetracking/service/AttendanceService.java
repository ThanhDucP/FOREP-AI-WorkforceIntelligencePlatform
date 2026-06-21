package com.aiworkforce.timetracking.service;

import com.aiworkforce.core.enums.AttendanceStatus;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.identity.service.EmployeeService;
import com.aiworkforce.timetracking.dto.AttendanceRequest;
import com.aiworkforce.timetracking.dto.AttendanceResponse;
import com.aiworkforce.timetracking.entity.Attendance;
import com.aiworkforce.timetracking.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeService employeeService;
    private final TeamRepository teamRepository;

    @Transactional
    public AttendanceResponse checkIn(AttendanceRequest request) {
        Employee employee = employeeService.getCurrentEmployee();
        LocalDate today = LocalDate.now();

        if (attendanceRepository.findByEmployeeIdAndCheckInDate(employee.getId(), today).isPresent()) {
            throw new BusinessException("Employee already checked in today");
        }

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setCheckInDate(today);
        attendance.setCheckInTime(LocalTime.now());
        attendance.setStatus(LocalTime.now().isAfter(LocalTime.of(9, 0))
                ? AttendanceStatus.LATE
                : AttendanceStatus.PRESENT);

        Attendance saved = attendanceRepository.save(attendance);
        log.info("Employee {} successfully checked in at {}", employee.getId(), saved.getCheckInTime());
        return mapToResponse(saved);
    }

    @Transactional
    public AttendanceResponse checkOut(AttendanceRequest request) {
        Employee employee = employeeService.getCurrentEmployee();
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByEmployeeIdAndCheckInDate(employee.getId(), today)
                .orElseThrow(() -> new BusinessException("Employee has not checked in today"));

        if (attendance.getCheckOutTime() != null) {
            throw new BusinessException("Employee already checked out today");
        }

        attendance.setCheckOutTime(LocalTime.now());
        Attendance saved = attendanceRepository.save(attendance);
        log.info("Employee {} successfully checked out at {}", employee.getId(), saved.getCheckOutTime());
        return mapToResponse(saved);
    }

    public List<AttendanceResponse> getMyAttendanceHistory() {
        Employee employee = employeeService.getCurrentEmployee();
        return attendanceRepository.findByEmployeeId(employee.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AttendanceResponse> getEmployeeAttendanceHistory(UUID employeeId) {
        return attendanceRepository.findByEmployeeId(employeeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AttendanceResponse> getTeamAttendanceHistory(UUID teamId) {
        return attendanceRepository.findByEmployeeTeamId(teamId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AttendanceResponse> getManagedTeamAttendanceHistory() {
        Employee currentEmployee = employeeService.getCurrentEmployee();
        List<UUID> teamIds = teamRepository.findByManagerId(currentEmployee.getId()).stream()
                .map(Team::getId)
                .collect(Collectors.toList());

        if (teamIds.isEmpty()) {
            return List.of();
        }

        return attendanceRepository.findByEmployeeTeamIdIn(teamIds).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AttendanceResponse> getOrganizationAttendanceHistory(UUID organizationId) {
        return attendanceRepository.findByEmployeeTeamOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AttendanceResponse mapToResponse(Attendance attendance) {
        if (attendance == null) return null;
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .checkInDate(attendance.getCheckInDate())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .status(attendance.getStatus().name())
                .employeeId(attendance.getEmployee().getId())
                .employeeName(attendance.getEmployee().getFirstName() + " " + attendance.getEmployee().getLastName())
                .build();
    }
}