package com.aiworkforce.analytics.controller;

import com.aiworkforce.analytics.dto.BurnoutResponse;
import com.aiworkforce.analytics.service.BurnoutAnalyticsService;
import com.aiworkforce.core.enums.BurnoutRisk;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class BurnoutAnalyticsControllerTest {

    @Mock
    private BurnoutAnalyticsService burnoutAnalyticsService;

    @Mock
    private EmployeeService employeeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new BurnoutAnalyticsController(burnoutAnalyticsService, employeeService)).build();
    }

    @Test
    void getMyBurnout_ReturnsCurrentEmployeeBurnout() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee();
        employee.setId(employeeId);

        when(employeeService.getCurrentEmployee()).thenReturn(employee);
        when(burnoutAnalyticsService.getEmployeeBurnout(employeeId)).thenReturn(employeeBurnout(employeeId));

        mockMvc.perform(get("/api/v1/burnout/my-burnout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.scope").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.scopeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.data.burnoutScore").value(82.0))
                .andExpect(jsonPath("$.data.burnoutRisk").value("HIGH"))
                .andExpect(jsonPath("$.data.workloadScore").value(91.0))
                .andExpect(jsonPath("$.data.overdueRatio").value(0.5))
                .andExpect(jsonPath("$.data.outOfHoursPct").value(42.0))
                .andExpect(jsonPath("$.data.avgCycleTimeDays").value(6.5))
                .andExpect(jsonPath("$.data.openTasks").value(12))
                .andExpect(jsonPath("$.data.overdueTasks").value(6))
                .andExpect(jsonPath("$.data.recommendation").value("Reduce active workload and rebalance overdue work."));

        verify(employeeService).getCurrentEmployee();
        verify(burnoutAnalyticsService).getEmployeeBurnout(employeeId);
    }

    @Test
    void getEmployeeBurnout_ReturnsRequestedEmployeeBurnout() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(burnoutAnalyticsService.getEmployeeBurnout(employeeId)).thenReturn(employeeBurnout(employeeId));

        mockMvc.perform(get("/api/v1/burnout/users/{employeeId}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scope").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.scopeName").value("Linh Pham"));

        verify(burnoutAnalyticsService).getEmployeeBurnout(employeeId);
    }

    @Test
    void getTeamBurnout_ReturnsRequestedTeamBurnout() throws Exception {
        UUID teamId = UUID.randomUUID();
        BurnoutResponse response = BurnoutResponse.builder()
                .scope("TEAM")
                .scopeId(teamId)
                .scopeName("Backend Team")
                .burnoutScore(63.0)
                .burnoutRisk(BurnoutRisk.MEDIUM)
                .workloadScore(77.0)
                .overdueRatio(0.3)
                .outOfHoursPct(24.0)
                .avgCycleTimeDays(4.2)
                .openTasks(20)
                .overdueTasks(6)
                .recommendation("Watch workload trend and protect focus time.")
                .build();

        when(burnoutAnalyticsService.getTeamBurnout(teamId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/burnout/teams/{teamId}", teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scope").value("TEAM"))
                .andExpect(jsonPath("$.data.scopeId").value(teamId.toString()))
                .andExpect(jsonPath("$.data.scopeName").value("Backend Team"))
                .andExpect(jsonPath("$.data.burnoutRisk").value("MEDIUM"));

        verify(burnoutAnalyticsService).getTeamBurnout(teamId);
    }

    private BurnoutResponse employeeBurnout(UUID employeeId) {
        return BurnoutResponse.builder()
                .scope("EMPLOYEE")
                .scopeId(employeeId)
                .scopeName("Linh Pham")
                .burnoutScore(82.0)
                .burnoutRisk(BurnoutRisk.HIGH)
                .workloadScore(91.0)
                .overdueRatio(0.5)
                .outOfHoursPct(42.0)
                .avgCycleTimeDays(6.5)
                .openTasks(12)
                .overdueTasks(6)
                .recommendation("Reduce active workload and rebalance overdue work.")
                .build();
    }
}