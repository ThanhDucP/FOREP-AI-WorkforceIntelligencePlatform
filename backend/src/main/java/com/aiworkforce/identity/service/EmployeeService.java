package com.aiworkforce.identity.service;

import com.aiworkforce.core.enums.AccountStatus;
import com.aiworkforce.core.enums.RoleType;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.core.exception.ForbiddenException;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.core.pagination.PaginationResponse;
import com.aiworkforce.identity.dto.EmployeeRequest;
import com.aiworkforce.identity.dto.EmployeeResponse;
import com.aiworkforce.identity.entity.Account;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.AccountRepository;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.OrganizationRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final TeamRepository teamRepository;
    private final OrganizationRepository organizationRepository;

    public Employee getCurrentEmployee() {
        return requireCurrentEmployee();
    }

    public EmployeeResponse getCurrentEmployeeProfile() {
        return mapToResponse(getCurrentEmployee());
    }

    @Transactional
    public EmployeeResponse updateProfile(EmployeeRequest request) {
        Employee employee = getCurrentEmployee();
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setJobTitle(request.getJobTitle());
        employee.setPhoneNumber(request.getPhoneNumber());
        employee.setDepartment(request.getDepartment());
        employee.setAvatarInitials(request.getAvatarInitials());

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
            employee.setTeam(team);
        } else {
            employee.setTeam(null);
        }

        return mapToResponse(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {
        if (currentAccountIsAdmin()) {
            return employeeRepository.findAll().stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        UUID organizationId = resolveOrganizationId(requireCurrentEmployee());
        if (organizationId == null) {
            return List.of();
        }
        return employeeRepository.findDistinctByOrganizationScopeAndAccountStatus(organizationId, null).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaginationResponse<EmployeeResponse> getAllEmployees(int page, int size) {
        validatePagination(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Employee> employees;

        if (currentAccountIsAdmin()) {
            employees = employeeRepository.findAll(pageable);
        } else {
            UUID organizationId = resolveOrganizationId(requireCurrentEmployee());
            List<Employee> scopedEmployees = organizationId == null
                    ? List.of()
                    : employeeRepository.findDistinctByOrganizationScopeAndAccountStatus(organizationId, null);
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), scopedEmployees.size());
            List<Employee> pageContent = start >= scopedEmployees.size() ? List.of() : scopedEmployees.subList(start, end);
            employees = new PageImpl<>(pageContent, pageable, scopedEmployees.size());
        }

        return PaginationResponse.<EmployeeResponse>builder()
                .content(employees.getContent().stream().map(this::mapToResponse).toList())
                .pageNumber(employees.getNumber())
                .pageSize(employees.getSize())
                .totalElements(employees.getTotalElements())
                .totalPages(employees.getTotalPages())
                .isLast(employees.isLast())
                .build();
    }

    public List<EmployeeResponse> getEmployeesByTeam(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));
        if (!currentAccountIsAdmin()) {
            ensureCurrentUserCanAccessOrganization(team.getOrganization());
        }
        return employeeRepository.findByTeamId(teamId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getEmployeesByOrganization(UUID organizationId) {
        return getEmployeesByOrganization(organizationId, null);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getEmployeesByOrganization(UUID organizationId, AccountStatus accountStatus) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));
        ensureCurrentUserCanAccessOrganization(organization);
        return employeeRepository.findDistinctByOrganizationScopeAndAccountStatus(organizationId, accountStatus).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public PaginationResponse<EmployeeResponse> getEmployeesByOrganization(UUID organizationId, AccountStatus accountStatus, int page, int size) {
        validatePagination(page, size);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));
        ensureCurrentUserCanAccessOrganization(organization);
        Page<Employee> employees = employeeRepository.findPageByOrganizationScopeAndAccountStatus(
                organizationId,
                accountStatus,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return toPaginationResponse(employees);
    }

    public EmployeeResponse getEmployeeById(UUID id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        if (!currentAccountIsAdmin()) {
            ensureCurrentUserCanAccessEmployee(employee);
        }
        return mapToResponse(employee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(UUID id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        ensureCurrentUserCanAccessEmployee(employee);
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setJobTitle(request.getJobTitle());
        employee.setPhoneNumber(request.getPhoneNumber());
        employee.setDepartment(request.getDepartment());
        employee.setAvatarInitials(request.getAvatarInitials());

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
            employee.setTeam(team);
        } else {
            employee.setTeam(null);
        }

        return mapToResponse(employeeRepository.save(employee));
    }

    @Transactional
    public void deleteEmployee(UUID id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        ensureCurrentUserCanAccessEmployee(employee);
        employeeRepository.delete(employee);
    }

    private PaginationResponse<EmployeeResponse> toPaginationResponse(Page<Employee> employees) {
        return PaginationResponse.<EmployeeResponse>builder()
                .content(employees.getContent().stream().map(this::mapToResponse).toList())
                .pageNumber(employees.getNumber())
                .pageSize(employees.getSize())
                .totalElements(employees.getTotalElements())
                .totalPages(employees.getTotalPages())
                .isLast(employees.isLast())
                .build();
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BusinessException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new BusinessException("size must be between 1 and 100");
        }
    }

    private void ensureCurrentUserCanAccessOrganization(Organization organization) {
        if (currentAccountIsAdmin()) {
            return;
        }
        if (organization == null) {
            throw new ForbiddenException("Current user does not have access to this organization");
        }

        UUID currentOrganizationId = resolveOrganizationId(requireCurrentEmployee());
        if (currentOrganizationId == null || organization.getId() == null || !currentOrganizationId.equals(organization.getId())) {
            throw new ForbiddenException("Current user does not have access to this organization");
        }
    }

    private void ensureCurrentUserCanAccessEmployee(Employee employee) {
        if (currentAccountIsAdmin()) {
            return;
        }
        UUID employeeOrganizationId = resolveOrganizationId(employee);
        UUID currentOrganizationId = resolveOrganizationId(requireCurrentEmployee());
        if (employeeOrganizationId == null || currentOrganizationId == null || !employeeOrganizationId.equals(currentOrganizationId)) {
            throw new ForbiddenException("Current user does not have access to this employee");
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
        return employee.getTeam() != null && employee.getTeam().getOrganization() != null ? employee.getTeam().getOrganization().getId() : null;
    }

    private String resolveOrganizationName(Employee employee) {
        if (employee.getOrganization() != null) return employee.getOrganization().getName();
        return employee.getTeam() != null && employee.getTeam().getOrganization() != null ? employee.getTeam().getOrganization().getName() : null;
    }

    public EmployeeResponse mapToResponse(Employee employee) {
        if (employee == null) return null;
        return EmployeeResponse.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .jobTitle(employee.getJobTitle())
                .phoneNumber(employee.getPhoneNumber())
                .email(employee.getAccount() != null ? employee.getAccount().getEmail() : null)
                .avatarUrl(employee.getAccount() != null ? employee.getAccount().getAvatarUrl() : null)
                .teamId(employee.getTeam() != null ? employee.getTeam().getId() : null)
                .teamName(employee.getTeam() != null ? employee.getTeam().getName() : null)
                .organizationId(resolveOrganizationId(employee))
                .organizationName(resolveOrganizationName(employee))
                .role(employee.getAccount() != null && employee.getAccount().getRole() != null ? employee.getAccount().getRole().getName().name() : null)
                .accountStatus(employee.getAccount() != null && employee.getAccount().getStatus() != null ? employee.getAccount().getStatus().name() : null)
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