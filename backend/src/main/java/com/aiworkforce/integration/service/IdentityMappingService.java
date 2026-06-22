package com.aiworkforce.integration.service;

import com.aiworkforce.core.enums.AccountStatus;
import com.aiworkforce.core.enums.AuditActionType;
import com.aiworkforce.core.enums.ExternalIdentityProvider;
import com.aiworkforce.core.enums.MappingStatus;
import com.aiworkforce.core.enums.RoleType;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.core.pagination.PaginationResponse;
import com.aiworkforce.core.security.AccessPolicyService;
import com.aiworkforce.core.service.AuditLogService;
import com.aiworkforce.identity.entity.Account;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.entity.Role;
import com.aiworkforce.identity.repository.AccountRepository;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.OrganizationRepository;
import com.aiworkforce.identity.repository.RoleRepository;
import com.aiworkforce.integration.dto.ChangeIdentityMappingRequest;
import com.aiworkforce.integration.dto.ExternalIdentityResponse;
import com.aiworkforce.integration.dto.IdentityMappingResponse;
import com.aiworkforce.integration.dto.IdentityMappingSummaryResponse;
import com.aiworkforce.integration.dto.ManualIdentityMappingRequest;
import com.aiworkforce.integration.entity.ExternalIdentity;
import com.aiworkforce.integration.entity.IdentityMapping;
import com.aiworkforce.integration.repository.ExternalIdentityRepository;
import com.aiworkforce.integration.repository.IdentityMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
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
    public PaginationResponse<ExternalIdentityResponse> getExternalMembers(UUID organizationId, int page, int size) {
        return paginate(getExternalMembers(organizationId), page, size);
    }

    @Transactional(readOnly = true)
    public List<IdentityMappingResponse> getMappings(UUID organizationId) {
        return getMappings(organizationId, null);
    }

    @Transactional(readOnly = true)
    public List<IdentityMappingResponse> getMappings(UUID organizationId, MappingStatus status) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        accessPolicyService.ensureOrganizationAccess(organization);
        return identityMappingRepository.findByOrganizationId(organizationId).stream()
                .filter(mapping -> status == null || mapping.getStatus() == status)
                .map(this::mapMapping)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaginationResponse<IdentityMappingResponse> getMappings(UUID organizationId, MappingStatus status, int page, int size) {
        return paginate(getMappings(organizationId, status), page, size);
    }

    @Transactional(readOnly = true)
    public IdentityMappingSummaryResponse getSummary(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        accessPolicyService.ensureOrganizationAccess(organization);
        List<IdentityMapping> mappings = identityMappingRepository.findByOrganizationId(organizationId);
        long matched = countStatus(mappings, MappingStatus.MATCHED);
        long possible = countStatus(mappings, MappingStatus.POSSIBLE_MATCH);
        long unmatched = countStatus(mappings, MappingStatus.UNMATCHED);
        long conflict = countStatus(mappings, MappingStatus.CONFLICT);
        long externalMemberCount = externalIdentityRepository.findByOrganizationId(organizationId).size();
        return IdentityMappingSummaryResponse.builder()
                .totalMembers(Math.max(externalMemberCount, mappings.size()))
                .matched(matched)
                .possibleMatch(possible)
                .unmatched(unmatched)
                .conflict(conflict)
                .build();
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
    public IdentityMappingResponse changeMapping(UUID mappingId, ChangeIdentityMappingRequest request) {
        IdentityMapping mapping = identityMappingRepository.findById(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException("Identity mapping not found"));
        accessPolicyService.ensureOrganizationManage(mapping.getOrganization());
        applyMappingTargets(mapping, request.getEmployeeId(), request.getJiraIdentityId(), request.getGithubIdentityId());
        mapping.setStatus(MappingStatus.POSSIBLE_MATCH);
        mapping.setConfidenceScore(null);
        mapping.setEvidenceSummary(request.getEvidenceSummary());
        mapping.setConfirmedAt(null);
        mapping.setConfirmedBy(null);
        IdentityMapping saved = identityMappingRepository.save(mapping);
        auditLogService.record(AuditActionType.CONFIRM_IDENTITY_MAPPING, "IdentityMapping", saved.getId(), null, Map.of("action", "CHANGE", "status", "POSSIBLE_MATCH"));
        return mapMapping(saved);
    }

    @Transactional
    public IdentityMappingResponse markUnmatched(UUID mappingId) {
        IdentityMapping mapping = identityMappingRepository.findById(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException("Identity mapping not found"));
        accessPolicyService.ensureOrganizationManage(mapping.getOrganization());
        mapping.setStatus(MappingStatus.UNMATCHED);
        mapping.setEmployee(null);
        mapping.setConfidenceScore(null);
        mapping.setEvidenceSummary("Marked as unmatched by manager");
        mapping.setConfirmedAt(LocalDateTime.now());
        mapping.setConfirmedBy(accessPolicyService.currentEmployee());
        IdentityMapping saved = identityMappingRepository.save(mapping);
        auditLogService.record(AuditActionType.CONFIRM_IDENTITY_MAPPING, "IdentityMapping", saved.getId(), null, Map.of("action", "MARK_UNMATCHED"));
        return mapMapping(saved);
    }


    @Transactional
    public IdentityMappingResponse createEmployeeFromMapping(UUID mappingId) {
        IdentityMapping mapping = identityMappingRepository.findById(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException("Identity mapping not found"));
        accessPolicyService.ensureOrganizationManage(mapping.getOrganization());
        if (mapping.getEmployee() != null) {
            return mapMapping(mapping);
        }

        String email = preferredEmail(mapping);
        if (email == null || email.isBlank()) {
            throw new BusinessException("Cannot create employee account because the mapping has no email");
        }
        email = email.trim().toLowerCase();
        if (accountRepository.existsByEmail(email)) {
            throw new BusinessException("An account with this email already exists");
        }

        Role employeeRole = roleRepository.findByName(RoleType.EMPLOYEE)
                .orElseThrow(() -> new BusinessException("EMPLOYEE role not found"));

        Account account = new Account();
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        account.setRole(employeeRole);
        account.setStatus(AccountStatus.PENDING);
        account.setActive(false);
        account.setLocked(false);
        Account savedAccount = accountRepository.save(account);

        Employee employee = new Employee();
        employee.setAccount(savedAccount);
        employee.setOrganization(mapping.getOrganization());
        applyEmployeeName(employee, preferredDisplayName(mapping));
        Employee savedEmployee = employeeRepository.save(employee);

        mapping.setEmployee(savedEmployee);
        mapping.setStatus(MappingStatus.POSSIBLE_MATCH);
        mapping.setEvidenceSummary(appendEvidence(mapping.getEvidenceSummary(), "Employee account created from identity mapping"));
        IdentityMapping savedMapping = identityMappingRepository.save(mapping);
        auditLogService.record(AuditActionType.INVITE_EMPLOYEE, "Employee", savedEmployee.getId(), null, Map.of("source", "identity_mapping", "mappingId", mappingId.toString()));
        return mapMapping(savedMapping);
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
            mapping.setJiraIdentity(resolveIdentity(request.getJiraIdentityId(), organization, ExternalIdentityProvider.JIRA));
        }
        if (request.getGithubIdentityId() != null) {
            mapping.setGithubIdentity(resolveIdentity(request.getGithubIdentityId(), organization, ExternalIdentityProvider.GITHUB));
        }
        IdentityMapping saved = identityMappingRepository.save(mapping);
        auditLogService.record(AuditActionType.CONFIRM_IDENTITY_MAPPING, "IdentityMapping", saved.getId(), null, Map.of("source", "manual"));
        return mapMapping(saved);
    }


    private <T> PaginationResponse<T> paginate(List<T> items, int page, int size) {
        if (page < 0) {
            throw new BusinessException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new BusinessException("size must be between 1 and 100");
        }
        int totalElements = items.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        return PaginationResponse.<T>builder()
                .content(items.subList(fromIndex, toIndex))
                .pageNumber(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .isLast(totalPages == 0 || page >= totalPages - 1)
                .build();
    }

    private String preferredEmail(IdentityMapping mapping) {
        if (mapping.getJiraIdentity() != null && mapping.getJiraIdentity().getEmail() != null && !mapping.getJiraIdentity().getEmail().isBlank()) {
            return mapping.getJiraIdentity().getEmail();
        }
        if (mapping.getGithubIdentity() != null && mapping.getGithubIdentity().getEmail() != null && !mapping.getGithubIdentity().getEmail().isBlank()) {
            return mapping.getGithubIdentity().getEmail();
        }
        return null;
    }

    private String preferredDisplayName(IdentityMapping mapping) {
        if (mapping.getJiraIdentity() != null && mapping.getJiraIdentity().getDisplayName() != null && !mapping.getJiraIdentity().getDisplayName().isBlank()) {
            return mapping.getJiraIdentity().getDisplayName();
        }
        if (mapping.getGithubIdentity() != null) {
            if (mapping.getGithubIdentity().getDisplayName() != null && !mapping.getGithubIdentity().getDisplayName().isBlank()) {
                return mapping.getGithubIdentity().getDisplayName();
            }
            return mapping.getGithubIdentity().getUsername();
        }
        return "Pending Employee";
    }

    private void applyEmployeeName(Employee employee, String displayName) {
        String normalized = displayName != null && !displayName.isBlank() ? displayName.trim() : "Pending Employee";
        String[] parts = normalized.split("\\s+", 2);
        employee.setFirstName(parts[0]);
        employee.setLastName(parts.length > 1 ? parts[1] : "");
    }

    private String appendEvidence(String current, String extra) {
        if (current == null || current.isBlank()) return extra;
        return current + "; " + extra;
    }
    private void applyMappingTargets(IdentityMapping mapping, UUID employeeId, UUID jiraIdentityId, UUID githubIdentityId) {
        Organization organization = mapping.getOrganization();
        if (employeeId != null) {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
            mapping.setEmployee(employee);
        }
        if (jiraIdentityId != null) {
            mapping.setJiraIdentity(resolveIdentity(jiraIdentityId, organization, ExternalIdentityProvider.JIRA));
        }
        if (githubIdentityId != null) {
            mapping.setGithubIdentity(resolveIdentity(githubIdentityId, organization, ExternalIdentityProvider.GITHUB));
        }
    }

    private ExternalIdentity resolveIdentity(UUID identityId, Organization organization, ExternalIdentityProvider expectedProvider) {
        ExternalIdentity identity = externalIdentityRepository.findById(identityId)
                .orElseThrow(() -> new ResourceNotFoundException(expectedProvider + " identity not found"));
        if (identity.getProvider() != expectedProvider) {
            throw new BusinessException("External identity provider mismatch");
        }
        if (identity.getOrganization() == null || organization == null || !identity.getOrganization().getId().equals(organization.getId())) {
            throw new BusinessException("External identity is outside the mapping organization");
        }
        return identity;
    }

    private long countStatus(List<IdentityMapping> mappings, MappingStatus status) {
        return mappings.stream().filter(mapping -> mapping.getStatus() == status).count();
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
