package com.aiworkforce.analytics.calculator;

import com.aiworkforce.event.entity.WorkloadEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkloadCalculator {

    public int calculateTotalWorkloadScore(List<WorkloadEvent> events) {
        return events.stream()
                .mapToInt(WorkloadEvent::getImpactScore)
                .sum();
    }

    public String detectBurnoutRiskLevel(int workloadScore, int overdueCount) {
        if (overdueCount > 5 || workloadScore < -20) {
            return "HIGH";
        } else if (overdueCount > 2 || workloadScore < 0) {
            return "MEDIUM";
        }
        return "LOW";
    }
}
