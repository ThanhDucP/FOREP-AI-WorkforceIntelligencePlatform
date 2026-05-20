package com.aiworkforce.timetracking.service;

import com.aiworkforce.core.enums.AttendanceStatus;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.service.EmployeeService;
import com.aiworkforce.timetracking.dto.AttendanceRequest;
import com.aiworkforce.timetracking.dto.AttendanceResponse;
import com.aiworkforce.timetracking.entity.Attendance;
import com.aiworkforce.timetracking.repository.AttendanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private AttendanceService attendanceService;

    private Employee employee;
    private Organization organization;
    private Team team;

    @BeforeEach
    public void setUp() {
        organization = new Organization();
        organization.setId(UUID.randomUUID());
        organization.setName("Mock Org");
        // Coordinates for Hoan Kiem Lake, Hanoi
        organization.setLatitude(21.0285);
        organization.setLongitude(105.8521);
        organization.setAllowedRadiusMeters(200);

        team = new Team();
        team.setId(UUID.randomUUID());
        team.setOrganization(organization);

        employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setTeam(team);
    }

    @Test
    public void testCheckIn_Success_WithinRadius() {
        when(employeeService.getCurrentEmployee()).thenReturn(employee);
        when(attendanceRepository.findByEmployeeIdAndCheckInDate(any(UUID.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        Attendance savedAttendance = new Attendance();
        savedAttendance.setId(UUID.randomUUID());
        savedAttendance.setEmployee(employee);
        savedAttendance.setCheckInDate(LocalDate.now());
        savedAttendance.setStatus(AttendanceStatus.PRESENT);

        when(attendanceRepository.save(any(Attendance.class))).thenReturn(savedAttendance);

        // Geolocation at a distance of ~50 meters from Hoan Kiem Lake
        AttendanceRequest request = new AttendanceRequest();
        request.setLatitude(21.0287);
        request.setLongitude(105.8524);

        AttendanceResponse response = attendanceService.checkIn(request);

        assertNotNull(response);
        verify(attendanceRepository, times(1)).save(any(Attendance.class));
    }

    @Test
    public void testCheckIn_Failed_OutsideRadius() {
        when(employeeService.getCurrentEmployee()).thenReturn(employee);
        when(attendanceRepository.findByEmployeeIdAndCheckInDate(any(UUID.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // Geolocation in Ho Chi Minh City (far away from Hanoi)
        AttendanceRequest request = new AttendanceRequest();
        request.setLatitude(10.8231);
        request.setLongitude(106.6297);

        assertThrows(BusinessException.class, () -> {
            attendanceService.checkIn(request);
        });

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    public void testCheckIn_Failed_NullGPS() {
        when(employeeService.getCurrentEmployee()).thenReturn(employee);
        when(attendanceRepository.findByEmployeeIdAndCheckInDate(any(UUID.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        AttendanceRequest request = new AttendanceRequest();
        request.setLatitude(null);
        request.setLongitude(null);

        assertThrows(BusinessException.class, () -> {
            attendanceService.checkIn(request);
        });

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    public void testCheckIn_Failed_AlreadyCheckedIn() {
        when(employeeService.getCurrentEmployee()).thenReturn(employee);
        
        Attendance existing = new Attendance();
        existing.setId(UUID.randomUUID());

        when(attendanceRepository.findByEmployeeIdAndCheckInDate(any(UUID.class), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));

        AttendanceRequest request = new AttendanceRequest();
        request.setLatitude(21.0287);
        request.setLongitude(105.8524);

        assertThrows(BusinessException.class, () -> {
            attendanceService.checkIn(request);
        });

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }
}
