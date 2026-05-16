package com.aiworkforce.analytics.service;

import com.aiworkforce.analytics.calculator.WorkloadCalculator;
import com.aiworkforce.analytics.dto.DashboardResponse;
import com.aiworkforce.core.enums.EventType;
import com.aiworkforce.event.entity.WorkloadEvent;
import com.aiworkforce.event.repository.WorkloadEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardAnalyticsService {
    
    private final WorkloadEventRepository workloadEventRepository;
    private final WorkloadCalculator workloadCalculator;

    public DashboardResponse getEmployeeDashboard(UUID employeeId) {
        List<WorkloadEvent> events = workloadEventRepository.findByEmployeeId(employeeId);
        
        int totalCompleted = (int) events.stream()
                .filter(e -> e.getEventType() == EventType.TASK_COMPLETED)
                .count();
                
        int totalOverdue = (int) events.stream()
                .filter(e -> e.getEventType() == EventType.TASK_OVERDUE)
                .count();

        int workloadScore = workloadCalculator.calculateTotalWorkloadScore(events);
        String riskLevel = workloadCalculator.detectBurnoutRiskLevel(workloadScore, totalOverdue);

        return DashboardResponse.builder()
                .totalTasksCompleted(totalCompleted)
                .totalOverdueTasks(totalOverdue)
                .currentWorkloadScore(workloadScore)
                .burnoutRiskLevel(riskLevel)
                .recentTrends(List.of()) // Mock empty trends
                .build();
    }
}
