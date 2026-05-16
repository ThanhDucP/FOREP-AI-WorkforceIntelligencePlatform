package com.aiworkforce.ai.service;

import com.aiworkforce.ai.client.OllamaClient;
import com.aiworkforce.ai.entity.AIInsight;
import com.aiworkforce.ai.prompt.PromptBuilder;
import com.aiworkforce.ai.repository.AIInsightRepository;
import com.aiworkforce.analytics.dto.DashboardResponse;
import com.aiworkforce.analytics.service.DashboardAnalyticsService;
import com.aiworkforce.core.enums.InsightSeverity;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIInsightService {
    
    private final OllamaClient ollamaClient;
    private final PromptBuilder promptBuilder;
    private final DashboardAnalyticsService analyticsService;
    private final EmployeeRepository employeeRepository;
    private final AIInsightRepository insightRepository;
    
    @Transactional
    public AIInsight generateInsightForEmployee(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
                
        DashboardResponse analytics = analyticsService.getEmployeeDashboard(employeeId);
        
        String prompt = promptBuilder.buildBurnoutPrompt(employee.getFirstName() + " " + employee.getLastName(), analytics);
        String aiResponse = ollamaClient.generateInsight(prompt);
        
        AIInsight insight = new AIInsight();
        insight.setEmployee(employee);
        insight.setFullAnalysis(prompt);
        insight.setSummary(aiResponse);
        insight.setSeverity(InsightSeverity.valueOf(analytics.getBurnoutRiskLevel()));
        
        return insightRepository.save(insight);
    }
    
    public List<AIInsight> getInsightsForEmployee(UUID employeeId) {
        return insightRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
    }
}
