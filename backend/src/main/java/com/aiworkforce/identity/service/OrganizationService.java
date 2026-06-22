package com.aiworkforce.identity.service;

import com.aiworkforce.core.enums.AccountStatus;
import com.aiworkforce.core.enums.ContractStatus;
import com.aiworkforce.core.enums.RoleType;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.core.exception.ForbiddenException;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.dto.EmployeeResponse;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<OrganizationResponse> getAllOrganizations() {
        if (currentAccountIsAdmin()) {
            return organizationRepository.findAll().stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        Employee currentEmployee = requireCurrentEmployee();
        UUID organizationId = resolveOrganizationId(currentEmployee);
        if (organizationId == null) {
            return List.of();
        }

        return organizationRepository.findById(organizationId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationById(UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
        ensureCurrentUserCanAccess(org);
        return mapToDetailResponse(org);
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
        return mapToDetailResponse(saved);
    }

    @Transactional
    public OrganizationResponse updateOrganization(UUID id, OrganizationRequest request) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));

        applyOrganizationFields(org, request);
        Organization saved = organizationRepository.save(org);
        return mapToDetailResponse(saved);
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
        UUID organizationId = org.getId();
        return OrganizationResponse.builder()
                .id(organizationId)
                .name(org.getName())
                .domain(org.getDomain())
                .logoUrl(org.getLogoUrl())
                .address(org.getAddress())
                .contractStatus(org.getContractStatus())
                .contractStartDate(org.getContractStartDate())
                .contractEndDate(org.getContractEndDate())
                .adminNote(org.getAdminNote())
                .maxUsers(org.getMaxUsers())
                .userCount(organizationId != null ? employeeRepository.countDistinctByOrganizationScope(organizationId) : 0)
                .directorId(director != null ? director.getId() : null)
                .directorName(director != null ? fullName(director) : null)
                .directorEmail(directorAccount != null ? directorAccount.getEmail() : null)
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }

    private OrganizationResponse mapToDetailResponse(Organization org) {
        OrganizationResponse response = mapToResponse(org);
        if (response != null && org.getId() != null) {
            response.setUsers(employeeRepository.findDistinctByOrganizationScopeAndAccountStatus(org.getId(), null).stream()
                    .map(this::mapEmployeeToResponse)
                    .toList());
        }
        return response;
    }

    private void applyOrganizationFields(Organization org, OrganizationRequest request) {
        validateContractDates(request);
        org.setName(request.getName());
        org.setDomain(request.getDomain());
        org.setAddress(request.getAddress());
        org.setContractStatus(request.getContractStatus() != null ? request.getContractStatus() : ContractStatus.ACTIVE);
        org.setContractStartDate(request.getContractStartDate());
        org.setContractEndDate(request.getContractEndDate());
        org.setMaxUsers(request.getMaxUsers());
        org.setAdminNote(request.getAdminNote());
    }

    private void validateContractDates(OrganizationRequest request) {
        if (request.getContractStartDate() != null
                && request.getContractEndDate() != null
                && request.getContractEndDate().isBefore(request.getContractStartDate())) {
            throw new BusinessException("contractEndDate must be greater than or equal to contractStartDate");
        }
    }

    private void ensureCurrentUserCanAccess(Organization organization) {
        if (currentAccountIsAdmin()) {
            return;
        }

        Employee currentEmployee = requireCurrentEmployee();
        UUID currentOrganizationId = resolveOrganizationId(currentEmployee);
        if (currentOrganizationId == null || organization.getId() == null || !currentOrganizationId.equals(organization.getId())) {
            throw new ForbiddenException("Current user does not have access to this organization");
        }
    }

    private Employee requireCurrentEmployee() {
        Account account = currentAccountIfAuthenticated();
        if (account == null) {
            throw new ForbiddenException("Authentication is required");
        }
        return employeeRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ForbiddenException("Current account is not linked to an employee profile"));
    }

    private Account currentAccountIfAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        String email = authentication.getName();
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found for email: " + email));
    }

    private boolean currentAccountIsAdmin() {
        Account account = currentAccountIfAuthenticated();
        return isAdmin(account);
    }

    private boolean isAdmin(Account account) {
        RoleType role = roleOf(account);
        return role == RoleType.SYSTEM_ADMIN || role == RoleType.ADMIN;
    }

    private RoleType roleOf(Account account) {
        return account != null && account.getRole() != null ? account.getRole().getName() : null;
    }

    private UUID resolveOrganizationId(Employee employee) {
        if (employee == null) return null;
        if (employee.getOrganization() != null) return employee.getOrganization().getId();
        return employee.getTeam() != null && employee.getTeam().getOrganization() != null
                ? employee.getTeam().getOrganization().getId()
                : null;
    }

    private String resolveOrganizationName(Employee employee) {
        if (employee.getOrganization() != null) return employee.getOrganization().getName();
        return employee.getTeam() != null && employee.getTeam().getOrganization() != null
                ? employee.getTeam().getOrganization().getName()
                : null;
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

    private EmployeeResponse mapEmployeeToResponse(Employee employee) {
        if (employee == null) return null;
        Account account = employee.getAccount();
        return EmployeeResponse.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .jobTitle(employee.getJobTitle())
                .phoneNumber(employee.getPhoneNumber())
                .email(account != null ? account.getEmail() : null)
                .avatarUrl(account != null ? account.getAvatarUrl() : null)
                .teamId(employee.getTeam() != null ? employee.getTeam().getId() : null)
                .teamName(employee.getTeam() != null ? employee.getTeam().getName() : null)
                .organizationId(resolveOrganizationId(employee))
                .organizationName(resolveOrganizationName(employee))
                .role(account != null && account.getRole() != null ? account.getRole().getName().name() : null)
                .accountStatus(account != null && account.getStatus() != null ? account.getStatus().name() : null)
                .department(employee.getDepartment())
                .avatarInitials(employee.getAvatarInitials())
                .workloadScore(employee.getWorkloadScore())
                .burnoutRisk(employee.getBurnoutRisk() != null ? employee.getBurnoutRisk().name() : null)
                .contributionScore(employee.getContributionScore())
                .overdueRatio(employee.getOverdueRatio())
                .outOfHoursPct(employee.getOutOfHoursPct())
                .avgCycleTimeDays(employee.getAvgCycleTimeDays())
                .tasksShippedThisMonth(employee.getTasksShippedThisMonth())
                .streakDays(employee.getStreakDays())
                .focusScore(employee.getFocusScore())
                .build();
    }
}