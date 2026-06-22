package com.aiworkforce.analytics.service;

import com.aiworkforce.analytics.dto.AdminDashboardResponse;
import com.aiworkforce.core.enums.BurnoutRisk;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.identity.repository.AccountRepository;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.OrganizationRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminDashboardServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    public void testGetAdminDashboardStats() {
        when(employeeRepository.count()).thenReturn(10L);
        when(accountRepository.countByActive(true)).thenReturn(8L);
        when(teamRepository.count()).thenReturn(3L);
        when(organizationRepository.count()).thenReturn(1L);
        when(taskRepository.count()).thenReturn(50L);
        when(taskRepository.countByStatus(TaskStatus.DONE)).thenReturn(30L);
        when(employeeRepository.countByBurnoutRiskGroup()).thenReturn(List.<Object[]>of(new Object[]{BurnoutRisk.HIGH, 1L}));

        AdminDashboardResponse response = adminDashboardService.getAdminDashboardStats();

        assertNotNull(response);
        assertEquals(10L, response.getTotalUsers());
        assertEquals(8L, response.getActiveUsers());
        assertEquals(3L, response.getTotalTeams());
        assertEquals(1L, response.getTotalOrganizations());
        assertEquals(50L, response.getTotalTasks());
        assertEquals(30L, response.getCompletedTasks());

        assertNotNull(response.getBurnoutRiskDistribution());
        assertEquals(1L, response.getBurnoutRiskDistribution().get("HIGH"));
        assertEquals(0L, response.getBurnoutRiskDistribution().get("MEDIUM"));
        assertEquals(0L, response.getBurnoutRiskDistribution().get("WATCH"));
    }
}