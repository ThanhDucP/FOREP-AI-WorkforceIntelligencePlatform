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
import com.aiworkforce.core.security.AccessPolicyService;
import com.aiworkforce.core.service.NotificationService;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.ProjectRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import com.aiworkforce.identity.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AIInsightServiceTest {

    @Mock private GeminiClient geminiClient;
    @Mock private PromptBuilder promptBuilder;
    @Mock private RagContextService ragContextService;
    @Mock private DashboardAnalyticsService analyticsService;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private EmployeeService employeeService;
    @Mock private AIInsightRepository insightRepository;
    @Mock private NotificationService notificationService;
    @Mock private AccessPolicyService accessPolicyService;

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
        when(geminiClient.generateInsight("mock-prompt")).thenReturn("""
                {
                  "riskLevel": "LOW",
                  "summary": "Nhan vien lam viec on dinh.",
                  "reasons": ["Phan bo cong viec hop ly"],
                  "recommendations": ["Duy tri nhip lam viec", "Ghi nhan dong gop"]
                }
                """);
        when(insightRepository.save(any(AIInsight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AIInsight result = aiInsightService.generateInsightForEmployee(employeeId);

        assertNotNull(result);
        assertEquals("Nhan vien lam viec on dinh.", result.getSummary());
        assertTrue(result.getFullAnalysis().contains("riskLevel"));
        assertEquals(InsightSeverity.LOW, result.getSeverity());
        assertEquals(InsightType.BURNOUT_WARNING, result.getInsightType());
        assertEquals(0.68, result.getConfidenceScore());
        assertEquals(employee, result.getEmployee());
        verify(insightRepository, times(1)).save(any(AIInsight.class));
    }

    @Test
    public void testGenerateInsightForEmployee_ParseJsonInsideMarkdownFence() {
        mockBaseGeneration(dashboardResponse);
        when(geminiClient.generateInsight("mock-prompt")).thenReturn("""
                ```json
                {
                  "riskLevel": "MEDIUM",
                  "summary": "Nhan vien can duoc theo doi nhe.",
                  "reasons": ["Co dau hieu tang tai"],
                  "recommendations": ["Theo doi workload"]
                }
                ```
                """);
        when(insightRepository.save(any(AIInsight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AIInsight result = aiInsightService.generateInsightForEmployee(employeeId);

        assertEquals("Nhan vien can duoc theo doi nhe.", result.getSummary());
        assertEquals(InsightSeverity.MEDIUM, result.getSeverity());
        assertTrue(result.getFullAnalysis().contains("Theo doi workload"));
    }

    @Test
    public void testGenerateInsightForEmployee_Throws_InvalidJson() {
        mockBaseGeneration(dashboardResponse);
        when(geminiClient.generateInsight("mock-prompt")).thenReturn("This is not a JSON response");

        AiServiceException exception = org.junit.jupiter.api.Assertions.assertThrows(AiServiceException.class, () ->
                aiInsightService.generateInsightForEmployee(employeeId)
        );

        assertTrue(exception.getMessage().contains("AI response is not valid JSON"));
        verify(insightRepository, never()).save(any(AIInsight.class));
    }

    @Test
    public void testGenerateInsightForEmployee_RejectsUnknownRiskLevel() {
        mockBaseGeneration(dashboardResponse);
        when(geminiClient.generateInsight("mock-prompt")).thenReturn("""
                {
                  "riskLevel": "UNKNOWN",
                  "summary": "Nhan vien on dinh.",
                  "reasons": ["Khong co tin hieu qua tai ro rang"],
                  "recommendations": ["Tiep tuc theo doi"]
                }
                """);

        AiServiceException exception = org.junit.jupiter.api.Assertions.assertThrows(AiServiceException.class, () ->
                aiInsightService.generateInsightForEmployee(employeeId)
        );

        assertTrue(exception.getMessage().contains("riskLevel"));
        verify(insightRepository, never()).save(any(AIInsight.class));
    }

    @Test
    public void testGenerateInsightForEmployee_HighSeverityCreatesNotifications() {
        Team team = new Team();
        team.setId(UUID.randomUUID());
        Employee manager = new Employee();
        manager.setId(UUID.randomUUID());
        manager.setFirstName("Manager");
        manager.setLastName("User");
        team.setManager(manager);
        employee.setTeam(team);

        DashboardResponse highRisk = DashboardResponse.builder()
                .totalTasksCompleted(3)
                .totalOverdueTasks(4)
                .currentWorkloadScore(85)
                .burnoutRiskLevel("HIGH")
                .build();
        mockBaseGeneration(highRisk);
        when(projectRepository.findAll()).thenReturn(List.of());
        when(geminiClient.generateInsight("mock-prompt")).thenReturn("""
                {
                  "riskLevel": "HIGH",
                  "summary": "Nhan vien co dau hieu qua tai cao.",
                  "reasons": ["Nhieu task qua han va workload cao"],
                  "recommendations": ["Giam tai ngay", "Manager kiem tra sprint"]
                }
                """);
        when(insightRepository.save(any(AIInsight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AIInsight result = aiInsightService.generateInsightForEmployee(employeeId);

        assertEquals(InsightSeverity.HIGH, result.getSeverity());
        verify(notificationService, times(2)).createNotification(any(), any(), anyString(), anyString(), any(), any());
    }

    private void mockBaseGeneration(DashboardResponse response) {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(analyticsService.getEmployeeDashboard(employeeId)).thenReturn(response);
        when(ragContextService.buildEmployeeContext(employee)).thenReturn("mock-rag-context");
        when(promptBuilder.buildBurnoutPrompt(anyString(), any(DashboardResponse.class), anyString())).thenReturn("mock-prompt");
    }
}