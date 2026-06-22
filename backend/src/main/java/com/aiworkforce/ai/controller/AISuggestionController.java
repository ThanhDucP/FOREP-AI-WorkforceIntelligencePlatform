package com.aiworkforce.ai.controller;

import com.aiworkforce.ai.dto.AISuggestionResponse;
import com.aiworkforce.ai.service.AISuggestionService;
import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.dto.SprintResponse;
import com.aiworkforce.identity.service.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/suggestions")
@RequiredArgsConstructor
public class AISuggestionController {

    private final AISuggestionService aiSuggestionService;
    private final SprintService sprintService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AISuggestionResponse>>> getSuggestions(
            @RequestParam(required = false) Integer sprintNumber) {
        Integer targetSprintNumber = sprintNumber;
        if (targetSprintNumber == null) {
            SprintResponse activeSprint = sprintService.getActiveSprint();
            targetSprintNumber = (activeSprint != null) ? activeSprint.getSprintNumber() : 1;
        }
        return ResponseEntity.ok(ApiResponse.success(aiSuggestionService.getSuggestionsForSprint(targetSprintNumber)));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER') or @employeeService.getCurrentEmployee().getId().equals(#employeeId)")
    public ResponseEntity<ApiResponse<List<AISuggestionResponse>>> getSuggestionsForEmployee(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(aiSuggestionService.getSuggestionsForEmployee(employeeId)));
    }

    @GetMapping("/team/{teamId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AISuggestionResponse>>> getSuggestionsForTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.success(aiSuggestionService.getSuggestionsForTeam(teamId)));
    }

    @GetMapping("/managed-teams")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<AISuggestionResponse>>> getSuggestionsForManagedTeams() {
        return ResponseEntity.ok(ApiResponse.success(aiSuggestionService.getSuggestionsForManagedTeams()));
    }

    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AISuggestionResponse>>> getSuggestionsForOrganization(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success(aiSuggestionService.getSuggestionsForOrganization(organizationId)));
    }
}
