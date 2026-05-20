package com.aiworkforce.ai.controller;

import com.aiworkforce.ai.entity.AIInsight;
import com.aiworkforce.ai.service.AIInsightService;
import com.aiworkforce.core.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIInsightController {

    private final AIInsightService aiInsightService;

    @PostMapping("/generate/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AIInsight>> generateInsight(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(aiInsightService.generateInsightForEmployee(employeeId)));
    }

    @GetMapping("/insights/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @employeeService.getCurrentEmployee().getId().equals(#employeeId)")
    public ResponseEntity<ApiResponse<List<AIInsight>>> getInsights(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(aiInsightService.getInsightsForEmployee(employeeId)));
    }
}
