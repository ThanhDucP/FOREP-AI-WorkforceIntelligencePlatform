package com.aiworkforce.analytics.service;

import com.aiworkforce.analytics.dto.BurnoutResponse;
import com.aiworkforce.core.enums.BurnoutRisk;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BurnoutAnalyticsServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private BurnoutAnalyticsService burnoutAnalyticsService;

    @Test
    void getEmployeeBurnout_ReturnsHighRiskForHighWorkloadAndOverdueRatio() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setFirstName("Linh");
        employee.setLastName("Pham");
        employee.setWorkloadScore(100.0);
        employee.setOutOfHoursPct(80.0);
        employee.setAvgCycleTimeDays(10.0);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(taskRepository.countByAssigneeIdAndStatusNot(employeeId, TaskStatus.DONE)).thenReturn(13L);
        when(taskRepository.countByAssigneeIdAndStatusNotAndDueDateBefore(eq(employeeId), eq(TaskStatus.DONE), any(LocalDateTime.class)))
                .thenReturn(13L);

        BurnoutResponse response = burnoutAnalyticsService.getEmployeeBurnout(employeeId);

        assertEquals("EMPLOYEE", response.getScope());
        assertEquals(employeeId, response.getScopeId());
        assertEquals("Linh Pham", response.getScopeName());
        assertEquals(BurnoutRisk.HIGH, response.getBurnoutRisk());
        assertTrue(response.getBurnoutScore() >= 75.0);
        assertEquals(1.0, response.getOverdueRatio());
        assertEquals(13L, response.getOpenTasks());
        assertEquals(13L, response.getOverdueTasks());
    }

    @Test
    void getTeamBurnout_AggregatesTeamSignalsIntoMediumRisk() {
        UUID teamId = UUID.randomUUID();
        Team team = new Team();
        team.setId(teamId);
        team.setName("AI Team");

        Employee first = new Employee();
        first.setWorkloadScore(90.0);
        first.setOutOfHoursPct(35.0);
        first.setAvgCycleTimeDays(6.0);
        Employee second = new Employee();
        second.setWorkloadScore(80.0);
        second.setOutOfHoursPct(25.0);
        second.setAvgCycleTimeDays(4.0);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(employeeRepository.findByTeamId(teamId)).thenReturn(List.of(first, second));
        when(taskRepository.countByTeamIdAndStatusNot(teamId, TaskStatus.DONE)).thenReturn(10L);
        when(taskRepository.countByTeamIdAndStatusNotAndDueDateBefore(eq(teamId), eq(TaskStatus.DONE), any(LocalDateTime.class)))
                .thenReturn(5L);

        BurnoutResponse response = burnoutAnalyticsService.getTeamBurnout(teamId);

        assertEquals("TEAM", response.getScope());
        assertEquals(teamId, response.getScopeId());
        assertEquals("AI Team", response.getScopeName());
        assertEquals(BurnoutRisk.MEDIUM, response.getBurnoutRisk());
        assertEquals(0.5, response.getOverdueRatio());
        assertEquals(10L, response.getOpenTasks());
        assertEquals(5L, response.getOverdueTasks());
    }
}

