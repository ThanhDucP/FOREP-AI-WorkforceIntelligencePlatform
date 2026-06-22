package com.aiworkforce.integration.controller;

import com.aiworkforce.core.pagination.PaginationResponse;
import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.integration.dto.ProjectSourceLinkRequest;
import com.aiworkforce.integration.dto.ProjectSourceLinkResponse;
import com.aiworkforce.integration.service.ProjectSourceLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations/source-links")
@RequiredArgsConstructor
public class ProjectSourceLinkController {

    private final ProjectSourceLinkService projectSourceLinkService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProjectSourceLinkResponse>> link(@Valid @RequestBody ProjectSourceLinkRequest request) {
        return ResponseEntity.ok(ApiResponse.success(projectSourceLinkService.link(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<PaginationResponse<ProjectSourceLinkResponse>>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID teamId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(projectSourceLinkService.list(organizationId, teamId, projectId, page, size)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        projectSourceLinkService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
