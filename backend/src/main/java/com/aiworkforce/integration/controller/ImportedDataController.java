package com.aiworkforce.integration.controller;

import com.aiworkforce.core.pagination.PaginationResponse;
import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.integration.dto.GithubCommitResponse;
import com.aiworkforce.integration.dto.GithubContributorResponse;
import com.aiworkforce.integration.dto.GithubPullRequestResponse;
import com.aiworkforce.integration.dto.GithubRepositoryResponse;
import com.aiworkforce.integration.dto.ImportedIssueResponse;
import com.aiworkforce.integration.dto.ImportedProjectResponse;
import com.aiworkforce.integration.service.ImportedDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ImportedDataController {

    private final ImportedDataService importedDataService;

    @GetMapping("/imported-projects")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<PaginationResponse<ImportedProjectResponse>>> getImportedProjects(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID teamId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(importedDataService.getImportedProjects(organizationId, teamId, projectId, page, size)));
    }

    @GetMapping("/imported-issues")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<PaginationResponse<ImportedIssueResponse>>> getImportedIssues(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID teamId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(importedDataService.getImportedIssues(organizationId, teamId, projectId, page, size)));
    }

    @GetMapping("/github/repositories")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<PaginationResponse<GithubRepositoryResponse>>> getGithubRepositories(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID teamId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(importedDataService.getGithubRepositories(organizationId, teamId, projectId, page, size)));
    }

    @GetMapping("/github/commits")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<PaginationResponse<GithubCommitResponse>>> getGithubCommits(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID teamId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(importedDataService.getGithubCommits(organizationId, teamId, projectId, page, size)));
    }

    @GetMapping("/github/pull-requests")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<PaginationResponse<GithubPullRequestResponse>>> getGithubPullRequests(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID teamId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(importedDataService.getGithubPullRequests(organizationId, teamId, projectId, page, size)));
    }

    @GetMapping("/github/contributors")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<PaginationResponse<GithubContributorResponse>>> getGithubContributors(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID teamId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(importedDataService.getGithubContributors(organizationId, teamId, projectId, page, size)));
    }
}
