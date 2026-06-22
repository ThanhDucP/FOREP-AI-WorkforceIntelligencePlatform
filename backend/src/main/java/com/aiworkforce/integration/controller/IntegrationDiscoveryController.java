package com.aiworkforce.integration.controller;

import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.integration.dto.GithubRepositoryDiscoveryRequest;
import com.aiworkforce.integration.dto.GithubRepositoryOptionResponse;
import com.aiworkforce.integration.dto.JiraProjectDiscoveryRequest;
import com.aiworkforce.integration.dto.JiraProjectOptionResponse;
import com.aiworkforce.integration.service.IntegrationDiscoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/integrations/discovery")
@RequiredArgsConstructor
public class IntegrationDiscoveryController {

    private final IntegrationDiscoveryService integrationDiscoveryService;

    @PostMapping("/jira/projects")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<JiraProjectOptionResponse>>> discoverJiraProjects(
            @Valid @RequestBody JiraProjectDiscoveryRequest request) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(integrationDiscoveryService.discoverJiraProjects(request)));
    }

    @PostMapping("/github/repositories")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<GithubRepositoryOptionResponse>>> discoverGithubRepositories(
            @Valid @RequestBody GithubRepositoryDiscoveryRequest request) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(integrationDiscoveryService.discoverGithubRepositories(request)));
    }
}