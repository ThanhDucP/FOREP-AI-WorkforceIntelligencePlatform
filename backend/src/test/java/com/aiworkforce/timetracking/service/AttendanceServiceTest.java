package com.aiworkforce.timetracking.service;

import com.aiworkforce.core.enums.AttendanceStatus;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.TeamRepository;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    private Employee employee;

    @BeforeEach
    public void setUp() {
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());
        organization.setName("Mock Org");
        organization.setAddress("Hoan Kiem, Hanoi");

        Team team = new Team();
        team.setId(UUID.randomUUID());
        team.setOrganization(organization);

        employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setTeam(team);
    }

    @Test
    public void testCheckIn_Success_WithoutGpsValidation() {
        when(employeeService.getCurrentEmployee()).thenReturn(employee);
        when(attendanceRepository.findByEmployeeIdAndCheckInDate(any(UUID.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        Attendance savedAttendance = new Attendance();
        savedAttendance.setId(UUID.randomUUID());
        savedAttendance.setEmployee(employee);
        savedAttendance.setCheckInDate(LocalDate.now());
        savedAttendance.setStatus(AttendanceStatus.PRESENT);

        when(attendanceRepository.save(any(Attendance.class))).thenReturn(savedAttendance);

        AttendanceResponse response = attendanceService.checkIn(new AttendanceRequest());

        assertNotNull(response);
        verify(attendanceRepository, times(1)).save(any(Attendance.class));
    }

    @Test
    public void testCheckIn_Failed_AlreadyCheckedIn() {
        when(employeeService.getCurrentEmployee()).thenReturn(employee);

        Attendance existing = new Attendance();
        existing.setId(UUID.randomUUID());

        when(attendanceRepository.findByEmployeeIdAndCheckInDate(any(UUID.class), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));

        assertThrows(BusinessException.class, () -> attendanceService.checkIn(new AttendanceRequest()));

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }
}