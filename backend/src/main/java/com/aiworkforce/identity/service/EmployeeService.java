package com.aiworkforce.identity.service;

import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.dto.EmployeeRequest;
import com.aiworkforce.identity.dto.EmployeeResponse;
import com.aiworkforce.identity.entity.Account;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Team;
import com.aiworkforce.identity.repository.AccountRepository;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final TeamRepository teamRepository;

    public Employee getCurrentEmployee() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found for email: " + email));
        return employeeRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee details not found for account: " + email));
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

    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<EmployeeResponse> getEmployeesByTeam(UUID teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Team not found with id: " + teamId);
        }

        return employeeRepository.findByTeamId(teamId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<EmployeeResponse> getEmployeesByOrganization(UUID organizationId) {
        Map<UUID, Employee> employees = new LinkedHashMap<>();
        for (Employee employee : employeeRepository.findByOrganizationId(organizationId)) {
            employees.put(employee.getId(), employee);
        }
        for (Employee employee : employeeRepository.findByTeamOrganizationId(organizationId)) {
            employees.put(employee.getId(), employee);
        }
        return new ArrayList<>(employees.values()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public EmployeeResponse getEmployeeById(UUID id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return mapToResponse(employee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(UUID id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
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
        employeeRepository.delete(employee);
    }


    private UUID resolveOrganizationId(Employee employee) {
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
