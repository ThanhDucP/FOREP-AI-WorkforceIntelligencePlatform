package com.aiworkforce.ai.service;

import com.aiworkforce.ai.client.GeminiClient;
import com.aiworkforce.ai.entity.AIInsight;
import com.aiworkforce.ai.prompt.PromptBuilder;
import com.aiworkforce.ai.repository.AIInsightRepository;
import com.aiworkforce.analytics.dto.DashboardResponse;
import com.aiworkforce.analytics.service.DashboardAnalyticsService;
import com.aiworkforce.core.enums.InsightSeverity;
import com.aiworkforce.core.enums.InsightType;
import com.aiworkforce.core.exception.AiServiceException;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.identity.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AIInsightServiceTest {

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private PromptBuilder promptBuilder;

    @Mock
    private DashboardAnalyticsService analyticsService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private AIInsightRepository insightRepository;

    @InjectMocks
    private AIInsightService aiInsightService;

    private Employee employee;
    private DashboardResponse dashboardResponse;
    private UUID employeeId;

    @BeforeEach
    public void setUp() {
        employeeId = UUID.randomUUID();
        employee = new Employee();
        employee.setId(employeeId);
        employee.setFirstName("Thanh");
        employee.setLastName("Duc");

        dashboardResponse = DashboardResponse.builder()
                .totalTasksCompleted(5)
                .totalOverdueTasks(1)
                .currentWorkloadScore(15)
                .burnoutRiskLevel("LOW")
                .build();
    }

    @Test
    public void testGenerateInsightForEmployee_Success_ValidJson() {
        mockBaseGeneration(dashboardResponse);
        String mockJsonResponse = """
                {
                  "status_evaluation": "Nhân viên làm việc ổn định.",
                  "primary_reason": "Phân bổ công việc hợp lý.",
                  "recommendations": ["Duy trì nhịp làm việc", "Ghi nhận đóng góp"]
                }
                """;
        when(geminiClient.generateInsight("mock-prompt")).thenReturn(mockJsonResponse);
        when(insightRepository.save(any(AIInsight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AIInsight result = aiInsightService.generateInsightForEmployee(employeeId);

        assertNotNull(result);
        assertEquals("Nhân viên làm việc ổn định.", result.getSummary());
        assertTrue(result.getFullAnalysis().contains("primary_reason"));
        assertEquals(InsightSeverity.LOW, result.getSeverity());
        assertEquals(InsightType.BURNOUT_WARNING, result.getInsightType());
        assertEquals(0.68, result.getConfidenceScore());
        assertEquals(employee, result.getEmployee());
        verify(insightRepository, times(1)).save(any(AIInsight.class));
    }

    @Test
    public void testGenerateInsightForEmployee_ParseJsonInsideMarkdownFence() {
        mockBaseGeneration(dashboardResponse);
        String fencedResponse = """
                ```json
                {
                  "status_evaluation": "Nhân viên cần được theo dõi nhẹ.",
                  "primary_reason": "Có dấu hiệu tăng tải.",
                  "recommendations": ["Theo dõi workload"]
                }
                ```
                """;
        when(geminiClient.generateInsight("mock-prompt")).thenReturn(fencedResponse);
        when(insightRepository.save(any(AIInsight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AIInsight result = aiInsightService.generateInsightForEmployee(employeeId);

        assertEquals("Nhân viên cần được theo dõi nhẹ.", result.getSummary());
        assertTrue(result.getFullAnalysis().contains("Theo dõi workload"));
    }

    @Test
    public void testGenerateInsightForEmployee_Throws_InvalidJson() {
        mockBaseGeneration(dashboardResponse);
        when(geminiClient.generateInsight("mock-prompt")).thenReturn("This is not a JSON response");

        AiServiceException exception = org.junit.jupiter.api.Assertions.assertThrows(AiServiceException.class, () ->
                aiInsightService.generateInsightForEmployee(employeeId)
        );

        assertTrue(exception.getMessage().contains("Gemini response is not valid JSON"));
        verify(insightRepository, never()).save(any(AIInsight.class));
    }

    @Test
    public void testGenerateInsightForEmployee_UnknownRiskLevelFallsBackToLowSeverity() {
        DashboardResponse unknownRisk = DashboardResponse.builder()
                .totalTasksCompleted(2)
                .totalOverdueTasks(0)
                .currentWorkloadScore(10)
                .burnoutRiskLevel("UNKNOWN")
                .build();
        mockBaseGeneration(unknownRisk);
        when(geminiClient.generateInsight("mock-prompt")).thenReturn("""
                {
                  "status_evaluation": "Nhân viên ổn định.",
                  "primary_reason": "Không có tín hiệu quá tải rõ ràng.",
                  "recommendations": ["Tiếp tục theo dõi"]
                }
                """);
        when(insightRepository.save(any(AIInsight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AIInsight result = aiInsightService.generateInsightForEmployee(employeeId);

        assertEquals(InsightSeverity.LOW, result.getSeverity());
        assertTrue(result.getFullAnalysis().contains("status_evaluation"));
        assertTrue(result.getFullAnalysis().contains("recommendations"));
    }

    private void mockBaseGeneration(DashboardResponse response) {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(analyticsService.getEmployeeDashboard(employeeId)).thenReturn(response);
        when(promptBuilder.buildBurnoutPrompt(anyString(), any(DashboardResponse.class))).thenReturn("mock-prompt");
    }
}
