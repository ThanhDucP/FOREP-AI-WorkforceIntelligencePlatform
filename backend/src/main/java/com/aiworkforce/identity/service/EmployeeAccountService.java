package com.aiworkforce.identity.service;

import com.aiworkforce.core.email.EmailDeliveryService;
import com.aiworkforce.core.enums.AccountStatus;
import com.aiworkforce.core.enums.RoleType;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.core.exception.ForbiddenException;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.core.security.AccessPolicyService;
import com.aiworkforce.identity.dto.EmployeeInvitationResponse;
import com.aiworkforce.identity.entity.Account;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.repository.AccountRepository;
import com.aiworkforce.identity.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeAccountService {

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final AccessPolicyService accessPolicyService;
    private final EmailDeliveryService emailDeliveryService;

    @Value("${app.frontend.activation-url:http://localhost:3000/activate}")
    private String activationBaseUrl;

    @Transactional
    public EmployeeInvitationResponse sendInvite(UUID employeeId) {
        Employee employee = getEmployeeForManage(employeeId);
        Account account = requireAccount(employee);
        account.setStatus(AccountStatus.INVITED);
        account.setActive(false);
        account.setActivationToken(generateToken());
        account.setInvitationSentAt(LocalDateTime.now());
        account.setActivatedAt(null);
        EmployeeInvitationResponse response = map(employee, accountRepository.save(account));
        emailDeliveryService.sendEmployeeInvitation(response);
        return response;
    }

    @Transactional
    public EmployeeInvitationResponse reinvite(UUID employeeId) {
        return sendInvite(employeeId);
    }

    @Transactional
    public EmployeeInvitationResponse activate(UUID employeeId) {
        Employee employee = getEmployeeForManage(employeeId);
        Account account = requireAccount(employee);
        account.setStatus(AccountStatus.ACTIVE);
        account.setActive(true);
        account.setActivationToken(null);
        account.setActivatedAt(LocalDateTime.now());
        return map(employee, accountRepository.save(account));
    }

    @Transactional
    public EmployeeInvitationResponse deactivate(UUID employeeId) {
        Employee employee = getEmployeeForManage(employeeId);
        Account account = requireAccount(employee);
        account.setStatus(AccountStatus.DISABLED);
        account.setActive(false);
        account.setActivationToken(null);
        return map(employee, accountRepository.save(account));
    }

    @Transactional
    public EmployeeInvitationResponse activateByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("Activation token is required");
        }
        Account account = accountRepository.findByActivationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Activation token not found"));
        Employee employee = employeeRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found for activation token"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setActive(true);
        account.setActivationToken(null);
        account.setActivatedAt(LocalDateTime.now());
        return map(employee, accountRepository.save(account));
    }

    private Employee getEmployeeForManage(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        if (currentAccountIsAdmin()) {
            return employee;
        }
        if (employee.getTeam() != null) {
            accessPolicyService.ensureTeamManage(employee.getTeam());
        } else if (employee.getOrganization() != null) {
            accessPolicyService.ensureOrganizationManage(employee.getOrganization());
        } else {
            throw new BusinessException("Employee has no organization scope");
        }
        return employee;
    }


    private boolean currentAccountIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return false;
        }
        Account account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ForbiddenException("Current account was not found"));
        RoleType role = account.getRole() != null ? account.getRole().getName() : null;
        return role == RoleType.SYSTEM_ADMIN || role == RoleType.ADMIN;
    }
    private Account requireAccount(Employee employee) {
        if (employee.getAccount() == null) {
            throw new BusinessException("Employee has no account");
        }
        return employee.getAccount();
    }

    private String generateToken() {
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private EmployeeInvitationResponse map(Employee employee, Account account) {
        return EmployeeInvitationResponse.builder()
                .employeeId(employee.getId())
                .email(account.getEmail())
                .accountStatus(account.getStatus() != null ? account.getStatus().name() : null)
                .activationToken(account.getActivationToken())
                .activationLink(account.getActivationToken() != null ? activationBaseUrl + "?token=" + account.getActivationToken() : null)
                .invitationSentAt(account.getInvitationSentAt())
                .activatedAt(account.getActivatedAt())
                .build();
    }
}
