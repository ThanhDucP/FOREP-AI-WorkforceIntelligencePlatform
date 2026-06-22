package com.aiworkforce.core.controller;

import com.aiworkforce.core.dto.FileUploadResponse;
import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.core.security.AccessPolicyService;
import com.aiworkforce.core.storage.LocalFileStorageService;
import com.aiworkforce.identity.entity.Account;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.repository.AccountRepository;
import com.aiworkforce.identity.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileStorageController {

    private final LocalFileStorageService fileStorageService;
    private final AccessPolicyService accessPolicyService;
    private final OrganizationRepository organizationRepository;
    private final AccountRepository accountRepository;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'DIRECTOR', 'MANAGER')")
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "document") String category) {
        return ResponseEntity.ok(ApiResponse.success(fileStorageService.store(file, category)));
    }

    @PostMapping("/organizations/{organizationId}/logo")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ADMIN', 'DIRECTOR')")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadOrganizationLogo(
            @PathVariable UUID organizationId,
            @RequestParam("file") MultipartFile file) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new com.aiworkforce.core.exception.ResourceNotFoundException("Organization not found"));
        accessPolicyService.ensureOrganizationManage(organization);
        FileUploadResponse response = fileStorageService.store(file, "organization-logo");
        organization.setLogoUrl(response.getDownloadUrl());
        organizationRepository.save(organization);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/me/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadMyAvatar(@RequestParam("file") MultipartFile file) {
        Employee current = accessPolicyService.currentEmployee();
        Account account = current.getAccount();
        if (account == null) {
            throw new com.aiworkforce.core.exception.BusinessException("Current employee has no account");
        }
        FileUploadResponse response = fileStorageService.store(file, "avatar");
        account.setAvatarUrl(response.getDownloadUrl());
        accountRepository.save(account);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{category}/{filename:.+}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(
            @PathVariable String category,
            @PathVariable String filename) {
        Resource resource = fileStorageService.load(category, filename);
        MediaType mediaType = MediaType.parseMediaType(fileStorageService.contentType(category, filename));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .contentType(mediaType)
                .body(resource);
    }
}
