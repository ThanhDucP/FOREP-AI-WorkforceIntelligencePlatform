package com.aiworkforce.ai.controller;

import com.aiworkforce.ai.dto.AiRuntimeStatusResponse;
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

    @GetMapping("/runtime-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AiRuntimeStatusResponse>> getRuntimeStatus() {
        return ResponseEntity.ok(ApiResponse.success(aiInsightService.getRuntimeStatus()));
    }

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

    @GetMapping("/insights/my-insights")
    public ResponseEntity<ApiResponse<List<AIInsight>>> getMyInsights() {
        return ResponseEntity.ok(ApiResponse.success(aiInsightService.getMyInsights()));
    }

    @GetMapping("/insights/team/{teamId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AIInsight>>> getTeamInsights(@PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.success(aiInsightService.getInsightsForTeam(teamId)));
    }

    @GetMapping("/insights/managed-teams")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<AIInsight>>> getManagedTeamInsights() {
        return ResponseEntity.ok(ApiResponse.success(aiInsightService.getInsightsForManagedTeams()));
    }

    @GetMapping("/insights/project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AIInsight>>> getProjectInsights(@PathVariable UUID projectId) {
        return ResponseEntity.ok(ApiResponse.success(aiInsightService.getInsightsForProject(projectId)));
    }

    @GetMapping("/insights/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AIInsight>>> getOrganizationInsights(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(aiInsightService.getInsightsForOrganization(organizationId)));
    }
}
