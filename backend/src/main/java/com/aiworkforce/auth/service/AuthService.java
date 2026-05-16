package com.aiworkforce.auth.service;

import com.aiworkforce.identity.entity.Account;
import com.aiworkforce.identity.entity.Role;
import com.aiworkforce.identity.repository.AccountRepository;
import com.aiworkforce.identity.repository.RoleRepository;
import com.aiworkforce.auth.dto.AuthResponse;
import com.aiworkforce.auth.dto.LoginRequest;
import com.aiworkforce.auth.dto.RegisterRequest;
import com.aiworkforce.core.enums.RoleType;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already in use");
        }

        Role employeeRole = roleRepository.findByName(RoleType.EMPLOYEE)
                .orElseThrow(() -> new BusinessException("Default role not found"));

        Account account = new Account();
        account.setEmail(request.getEmail());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setRole(employeeRole);
        account.setActive(true);
        account.setLocked(false);
        Account savedAccount = accountRepository.save(account);

        Employee employee = new Employee();
        employee.setAccount(savedAccount);
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employeeRepository.save(employee);

        UserDetails userDetails = new User(account.getEmail(), account.getPassword(), Collections.emptyList());
        String jwtToken = jwtService.generateToken(userDetails);
        
        return AuthResponse.builder()
                .token(jwtToken)
                .refreshToken(jwtToken) // Optional: Generate distinct refresh token
                .type("Bearer")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("User not found"));
                
        // Reset failed attempts upon successful login
        if (account.getFailedLoginAttempts() > 0) {
            account.setFailedLoginAttempts(0);
            accountRepository.save(account);
        }

        UserDetails userDetails = new User(account.getEmail(), account.getPassword(), Collections.emptyList());
        String jwtToken = jwtService.generateToken(userDetails);
        
        return AuthResponse.builder()
                .token(jwtToken)
                .refreshToken(jwtToken)
                .type("Bearer")
                .build();
    }
}
