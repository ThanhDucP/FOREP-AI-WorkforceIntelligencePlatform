package com.aiworkforce.analytics.service;

import com.aiworkforce.analytics.dto.AnalyticsSummaryResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnalyticsSummaryServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private AnalyticsSummaryService analyticsSummaryService;

    @Test
    void getEmployeeSummary_ReturnsCompletedOverdueAndWorkloadMetrics() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setFirstName("Mai");
        employee.setLastName("Tran");
        employee.setWorkloadScore(72.4);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(taskRepository.countByAssigneeIdAndStatus(employeeId, TaskStatus.DONE)).thenReturn(8L);
        when(taskRepository.countByAssigneeIdAndStatusNot(employeeId, TaskStatus.DONE)).thenReturn(5L);
        when(taskRepository.countByAssigneeIdAndStatusNotAndDueDateBefore(eq(employeeId), eq(TaskStatus.DONE), any(LocalDateTime.class)))
                .thenReturn(2L);

        AnalyticsSummaryResponse response = analyticsSummaryService.getEmployeeSummary(employeeId);

        assertEquals("EMPLOYEE", response.getScope());
        assertEquals(employeeId, response.getScopeId());
        assertEquals("Mai Tran", response.getScopeName());
        assertEquals(8L, response.getCompletedTasks());
        assertEquals(5L, response.getOpenTasks());
        assertEquals(2L, response.getOverdueTasks());
        assertEquals(0.4, response.getOverdueRatio());
        assertEquals(72.4, response.getWorkloadScore());
        assertEquals(1, response.getMemberCount());
    }

    @Test
    void getTeamSummary_AggregatesTeamTaskCountsAndAverageWorkload() {
        UUID teamId = UUID.randomUUID();
        Team team = new Team();
        team.setId(teamId);
        team.setName("Backend Team");

        Employee first = new Employee();
        first.setWorkloadScore(60.0);
        Employee second = new Employee();
        second.setWorkloadScore(80.0);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(employeeRepository.findByTeamId(teamId)).thenReturn(List.of(first, second));
        when(taskRepository.countByTeamIdAndStatus(teamId, TaskStatus.DONE)).thenReturn(14L);
        when(taskRepository.countByTeamIdAndStatusNot(teamId, TaskStatus.DONE)).thenReturn(10L);
        when(taskRepository.countByTeamIdAndStatusNotAndDueDateBefore(eq(teamId), eq(TaskStatus.DONE), any(LocalDateTime.class)))
                .thenReturn(3L);

        AnalyticsSummaryResponse response = analyticsSummaryService.getTeamSummary(teamId);

        assertEquals("TEAM", response.getScope());
        assertEquals(teamId, response.getScopeId());
        assertEquals("Backend Team", response.getScopeName());
        assertEquals(14L, response.getCompletedTasks());
        assertEquals(10L, response.getOpenTasks());
        assertEquals(3L, response.getOverdueTasks());
        assertEquals(0.3, response.getOverdueRatio());
        assertEquals(70.0, response.getWorkloadScore());
        assertEquals(2, response.getMemberCount());
    }
}
