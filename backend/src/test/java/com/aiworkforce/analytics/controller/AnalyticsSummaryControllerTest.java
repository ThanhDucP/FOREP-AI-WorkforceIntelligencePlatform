package com.aiworkforce.analytics.controller;

import com.aiworkforce.analytics.dto.AnalyticsSummaryResponse;
import com.aiworkforce.analytics.service.AnalyticsDashboardService;
import com.aiworkforce.analytics.service.AnalyticsSummaryService;
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
class AnalyticsSummaryControllerTest {

    @Mock
    private AnalyticsSummaryService analyticsSummaryService;

    @Mock
    private AnalyticsDashboardService analyticsDashboardService;

    @Mock
    private EmployeeService employeeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new AnalyticsSummaryController(analyticsSummaryService, analyticsDashboardService, employeeService)).build();
    }

    @Test
    void getMySummary_ReturnsCurrentEmployeeSummary() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee();
        employee.setId(employeeId);

        when(employeeService.getCurrentEmployee()).thenReturn(employee);
        when(analyticsSummaryService.getEmployeeSummary(employeeId)).thenReturn(employeeSummary(employeeId));

        mockMvc.perform(get("/api/v1/analytics/summary/my-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.scope").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.scopeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.data.completedTasks").value(9))
                .andExpect(jsonPath("$.data.openTasks").value(4))
                .andExpect(jsonPath("$.data.overdueTasks").value(1))
                .andExpect(jsonPath("$.data.overdueRatio").value(0.25))
                .andExpect(jsonPath("$.data.workloadScore").value(68.5))
                .andExpect(jsonPath("$.data.memberCount").value(1));

        verify(employeeService).getCurrentEmployee();
        verify(analyticsSummaryService).getEmployeeSummary(employeeId);
    }

    @Test
    void getEmployeeSummary_ReturnsRequestedEmployeeSummary() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(analyticsSummaryService.getEmployeeSummary(employeeId)).thenReturn(employeeSummary(employeeId));

        mockMvc.perform(get("/api/v1/analytics/summary/users/{employeeId}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scope").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.scopeName").value("Mai Tran"));

        verify(analyticsSummaryService).getEmployeeSummary(employeeId);
    }

    @Test
    void getTeamSummary_ReturnsRequestedTeamSummary() throws Exception {
        UUID teamId = UUID.randomUUID();
        AnalyticsSummaryResponse response = AnalyticsSummaryResponse.builder()
                .scope("TEAM")
                .scopeId(teamId)
                .scopeName("Backend Team")
                .completedTasks(18)
                .openTasks(7)
                .overdueTasks(2)
                .overdueRatio(0.29)
                .workloadScore(74.0)
                .memberCount(3)
                .build();

        when(analyticsSummaryService.getTeamSummary(teamId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/analytics/summary/teams/{teamId}", teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scope").value("TEAM"))
                .andExpect(jsonPath("$.data.scopeId").value(teamId.toString()))
                .andExpect(jsonPath("$.data.scopeName").value("Backend Team"))
                .andExpect(jsonPath("$.data.memberCount").value(3));

        verify(analyticsSummaryService).getTeamSummary(teamId);
    }

    private AnalyticsSummaryResponse employeeSummary(UUID employeeId) {
        return AnalyticsSummaryResponse.builder()
                .scope("EMPLOYEE")
                .scopeId(employeeId)
                .scopeName("Mai Tran")
                .completedTasks(9)
                .openTasks(4)
                .overdueTasks(1)
                .overdueRatio(0.25)
                .workloadScore(68.5)
                .memberCount(1)
                .build();
    }
}