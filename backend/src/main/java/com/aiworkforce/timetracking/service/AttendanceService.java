package com.aiworkforce.timetracking.service;

import com.aiworkforce.core.enums.AttendanceStatus;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Organization;
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

    @Transactional
    public AttendanceResponse checkIn(AttendanceRequest request) {
        Employee employee = employeeService.getCurrentEmployee();
        LocalDate today = LocalDate.now();

        // Check if already checked in today
        if (attendanceRepository.findByEmployeeIdAndCheckInDate(employee.getId(), today).isPresent()) {
            throw new BusinessException("Bạn đã check-in ngày hôm nay rồi!");
        }

        // Validate GPS coordinates
        validateGps(employee, request.getLatitude(), request.getLongitude());

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setCheckInDate(today);
        attendance.setCheckInTime(LocalTime.now());
        
        // Rules: late check-in if after 09:00 AM
        if (LocalTime.now().isAfter(LocalTime.of(9, 0))) {
            attendance.setStatus(AttendanceStatus.LATE);
        } else {
            attendance.setStatus(AttendanceStatus.PRESENT);
        }

        Attendance saved = attendanceRepository.save(attendance);
        log.info("Employee {} successfully checked in at {}", employee.getId(), saved.getCheckInTime());
        return mapToResponse(saved);
    }

    @Transactional
    public AttendanceResponse checkOut(AttendanceRequest request) {
        Employee employee = employeeService.getCurrentEmployee();
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByEmployeeIdAndCheckInDate(employee.getId(), today)
                .orElseThrow(() -> new BusinessException("Hôm nay bạn chưa Check-in, không thể Check-out!"));

        if (attendance.getCheckOutTime() != null) {
            throw new BusinessException("Bạn đã check-out ngày hôm nay rồi!");
        }

        // Validate GPS coordinates
        validateGps(employee, request.getLatitude(), request.getLongitude());

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

    private void validateGps(Employee employee, Double userLat, Double userLon) {
        if (employee.getTeam() == null || employee.getTeam().getOrganization() == null) {
            // If employee does not belong to any organization, skip GPS validation (local testing or loose rules)
            return;
        }

        Organization org = employee.getTeam().getOrganization();
        if (org.getLatitude() == null || org.getLongitude() == null) {
            // Organization location not set, skip GPS validation
            return;
        }

        if (userLat == null || userLon == null) {
            throw new BusinessException("Yêu cầu cung cấp tọa độ GPS để chấm công.");
        }

        double distance = calculateDistance(userLat, userLon, org.getLatitude(), org.getLongitude());
        int allowedRadius = org.getAllowedRadiusMeters() != null ? org.getAllowedRadiusMeters() : 200;

        log.info("Distance between employee {} and office: {} meters (Allowed: {} meters)",
                employee.getId(), distance, allowedRadius);

        if (distance > allowedRadius) {
            throw new BusinessException(String.format(
                    "Bạn đang ở ngoài phạm vi văn phòng công ty (khoảng cách: %d mét, giới hạn: %d mét). Không thể chấm công!",
                    Math.round(distance), allowedRadius));
        }
    }

    // Haversine formula to calculate distance in meters
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distance in meters
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
