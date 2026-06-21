package com.aiworkforce.integration.service;

import com.aiworkforce.core.enums.AuditActionType;
import com.aiworkforce.core.enums.MappingStatus;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.core.security.AccessPolicyService;
import com.aiworkforce.core.service.AuditLogService;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.OrganizationRepository;
import com.aiworkforce.integration.dto.ExternalIdentityResponse;
import com.aiworkforce.integration.dto.IdentityMappingResponse;
import com.aiworkforce.integration.dto.ManualIdentityMappingRequest;
import com.aiworkforce.integration.entity.ExternalIdentity;
import com.aiworkforce.integration.entity.IdentityMapping;
import com.aiworkforce.integration.repository.ExternalIdentityRepository;
import com.aiworkforce.integration.repository.IdentityMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityMappingService {

    private final ExternalIdentityRepository externalIdentityRepository;
    private final IdentityMappingRepository identityMappingRepository;
    private final EmployeeRepository employeeRepository;
    private final OrganizationRepository organizationRepository;
    private final AccessPolicyService accessPolicyService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<ExternalIdentityResponse> getExternalMembers(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        accessPolicyService.ensureOrganizationAccess(organization);
        return externalIdentityRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapExternalIdentity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IdentityMappingResponse> getMappings(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        accessPolicyService.ensureOrganizationAccess(organization);
        return identityMappingRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapMapping)
                .toList();
    }

    @Transactional
    public IdentityMappingResponse confirmMapping(UUID mappingId) {
        IdentityMapping mapping = identityMappingRepository.findById(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException("Identity mapping not found"));
        accessPolicyService.ensureOrganizationManage(mapping.getOrganization());
        Employee confirmer = accessPolicyService.currentEmployee();
        mapping.setStatus(MappingStatus.MATCHED);
        mapping.setConfirmedAt(LocalDateTime.now());
        mapping.setConfirmedBy(confirmer);
        IdentityMapping saved = identityMappingRepository.save(mapping);
        auditLogService.record(AuditActionType.CONFIRM_IDENTITY_MAPPING, "IdentityMapping", saved.getId(), null, Map.of("status", "MATCHED"));
        return mapMapping(saved);
    }

    @Transactional
    public IdentityMappingResponse createManualMapping(ManualIdentityMappingRequest request) {
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        accessPolicyService.ensureOrganizationManage(organization);
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        IdentityMapping mapping = new IdentityMapping();
        mapping.setOrganization(organization);
        mapping.setEmployee(employee);
        mapping.setStatus(MappingStatus.MATCHED);
        mapping.setConfidenceScore(1.0);
        mapping.setEvidenceSummary(request.getEvidenceSummary());
        mapping.setConfirmedAt(LocalDateTime.now());
        mapping.setConfirmedBy(accessPolicyService.currentEmployee());
        if (request.getJiraIdentityId() != null) {
            mapping.setJiraIdentity(externalIdentityRepository.findById(request.getJiraIdentityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Jira identity not found")));
        }
        if (request.getGithubIdentityId() != null) {
            mapping.setGithubIdentity(externalIdentityRepository.findById(request.getGithubIdentityId())
                    .orElseThrow(() -> new ResourceNotFoundException("GitHub identity not found")));
        }
        IdentityMapping saved = identityMappingRepository.save(mapping);
        auditLogService.record(AuditActionType.CONFIRM_IDENTITY_MAPPING, "IdentityMapping", saved.getId(), null, Map.of("source", "manual"));
        return mapMapping(saved);
    }

    private ExternalIdentityResponse mapExternalIdentity(ExternalIdentity identity) {
        return ExternalIdentityResponse.builder()
                .id(identity.getId())
                .provider(identity.getProvider())
                .externalId(identity.getExternalId())
                .username(identity.getUsername())
                .displayName(identity.getDisplayName())
                .email(identity.getEmail())
                .avatarUrl(identity.getAvatarUrl())
                .organizationId(identity.getOrganization() != null ? identity.getOrganization().getId() : null)
                .teamId(identity.getTeam() != null ? identity.getTeam().getId() : null)
                .build();
    }

    private IdentityMappingResponse mapMapping(IdentityMapping mapping) {
        Employee employee = mapping.getEmployee();
        return IdentityMappingResponse.builder()
                .id(mapping.getId())
                .organizationId(mapping.getOrganization() != null ? mapping.getOrganization().getId() : null)
                .employeeId(employee != null ? employee.getId() : null)
                .employeeName(employee != null ? ((employee.getFirstName() + " " + employee.getLastName()).trim()) : null)
                .jiraIdentityId(mapping.getJiraIdentity() != null ? mapping.getJiraIdentity().getId() : null)
                .githubIdentityId(mapping.getGithubIdentity() != null ? mapping.getGithubIdentity().getId() : null)
                .status(mapping.getStatus())
                .confidenceScore(mapping.getConfidenceScore())
                .evidenceSummary(mapping.getEvidenceSummary())
                .confirmedAt(mapping.getConfirmedAt())
                .confirmedByEmployeeId(mapping.getConfirmedBy() != null ? mapping.getConfirmedBy().getId() : null)
                .build();
    }
}