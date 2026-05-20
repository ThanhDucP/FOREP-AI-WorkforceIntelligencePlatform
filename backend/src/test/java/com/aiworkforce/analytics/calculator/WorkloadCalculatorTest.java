package com.aiworkforce.analytics.calculator;

import com.aiworkforce.core.enums.EventType;
import com.aiworkforce.event.entity.WorkloadEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorkloadCalculatorTest {

    private WorkloadCalculator calculator;

    @BeforeEach
    public void setUp() {
        calculator = new WorkloadCalculator();
    }

    @Test
    public void testCalculateTotalWorkloadScore() {
        List<WorkloadEvent> events = new ArrayList<>();
        
        WorkloadEvent e1 = new WorkloadEvent();
        e1.setEventType(EventType.TASK_CREATED);
        e1.setImpactScore(2);
        
        WorkloadEvent e2 = new WorkloadEvent();
        e2.setEventType(EventType.TASK_COMPLETED);
        e2.setImpactScore(10);
        
        WorkloadEvent e3 = new WorkloadEvent();
        e3.setEventType(EventType.TASK_OVERDUE);
        e3.setImpactScore(-5);

        events.add(e1);
        events.add(e2);
        events.add(e3);

        int totalScore = calculator.calculateTotalWorkloadScore(events);
        assertEquals(7, totalScore);
    }

    @Test
    public void testDetectBurnoutRiskLevel_Low() {
        String risk = calculator.detectBurnoutRiskLevel(10, 1);
        assertEquals("LOW", risk);
    }

    @Test
    public void testDetectBurnoutRiskLevel_Medium_Overdue() {
        String risk = calculator.detectBurnoutRiskLevel(10, 3);
        assertEquals("MEDIUM", risk);
    }

    @Test
    public void testDetectBurnoutRiskLevel_Medium_LowScore() {
        String risk = calculator.detectBurnoutRiskLevel(-5, 0);
        assertEquals("MEDIUM", risk);
    }

    @Test
    public void testDetectBurnoutRiskLevel_High_Overdue() {
        String risk = calculator.detectBurnoutRiskLevel(10, 6);
        assertEquals("HIGH", risk);
    }

    @Test
    public void testDetectBurnoutRiskLevel_High_LowScore() {
        String risk = calculator.detectBurnoutRiskLevel(-25, 0);
        assertEquals("HIGH", risk);
    }
}
