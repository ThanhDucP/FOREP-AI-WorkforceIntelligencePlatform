package com.aiworkforce.identity.service;

import com.aiworkforce.core.enums.AccountStatus;
import com.aiworkforce.core.enums.RoleType;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.dto.OrganizationRequest;
import com.aiworkforce.identity.dto.OrganizationResponse;
import com.aiworkforce.identity.entity.Account;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.entity.Role;
import com.aiworkforce.identity.repository.AccountRepository;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.OrganizationRepository;
import com.aiworkforce.identity.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<OrganizationResponse> getAllOrganizations() {
        return organizationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public OrganizationResponse getOrganizationById(UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
        return mapToResponse(org);
    }

    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request) {
        Organization org = new Organization();
        applyOrganizationFields(org, request);

        Organization saved = organizationRepository.save(org);
        Employee director = createDirectorIfRequested(saved, request);
        if (director != null) {
            saved.setDirector(director);
            saved = organizationRepository.save(saved);
        }
        return mapToResponse(saved);
    }

    @Transactional
    public OrganizationResponse updateOrganization(UUID id, OrganizationRequest request) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));

        applyOrganizationFields(org, request);
        Organization saved = organizationRepository.save(org);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteOrganization(UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
        organizationRepository.delete(org);
    }

    public OrganizationResponse mapToResponse(Organization org) {
        if (org == null) return null;
        Employee director = org.getDirector();
        Account directorAccount = director != null ? director.getAccount() : null;
        return OrganizationResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .domain(org.getDomain())
                .logoUrl(org.getLogoUrl())
                .address(org.getAddress())
                .directorId(director != null ? director.getId() : null)
                .directorName(director != null ? fullName(director) : null)
                .directorEmail(directorAccount != null ? directorAccount.getEmail() : null)
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }

    private void applyOrganizationFields(Organization org, OrganizationRequest request) {
        org.setName(request.getName());
        org.setDomain(request.getDomain());
        org.setLogoUrl(request.getLogoUrl());
        org.setAddress(request.getAddress());
    }

    private Employee createDirectorIfRequested(Organization organization, OrganizationRequest request) {
        if (request.getDirectorEmail() == null || request.getDirectorEmail().isBlank()) {
            return null;
        }
        String email = request.getDirectorEmail().trim().toLowerCase();
        if (accountRepository.existsByEmail(email)) {
            throw new BusinessException("Director email already exists");
        }

        Role directorRole = roleRepository.findByName(RoleType.DIRECTOR)
                .orElseThrow(() -> new BusinessException("DIRECTOR role not found"));

        Account account = new Account();
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(resolveDirectorPassword(request)));
        account.setRole(directorRole);
        account.setStatus(AccountStatus.ACTIVE);
        account.setActive(true);
        account.setLocked(false);
        Account savedAccount = accountRepository.save(account);

        Employee director = new Employee();
        director.setAccount(savedAccount);
        director.setFirstName(valueOrDefault(request.getDirectorFirstName(), "Organization"));
        director.setLastName(valueOrDefault(request.getDirectorLastName(), "Director"));
        director.setJobTitle("Director");
        director.setOrganization(organization);
        return employeeRepository.save(director);
    }

    private String resolveDirectorPassword(OrganizationRequest request) {
        if (request.getDirectorPassword() != null && !request.getDirectorPassword().isBlank()) {
            return request.getDirectorPassword();
        }
        return UUID.randomUUID().toString();
    }

    private String valueOrDefault(String value, String fallback) {
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }

    private String fullName(Employee employee) {
        String first = employee.getFirstName() != null ? employee.getFirstName() : "";
        String last = employee.getLastName() != null ? employee.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isBlank() ? employee.getId().toString() : name;
    }
}