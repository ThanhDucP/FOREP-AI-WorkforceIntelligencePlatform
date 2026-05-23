package com.aiworkforce.auth.service;

import com.aiworkforce.auth.dto.AuthResponse;
import com.aiworkforce.auth.dto.LoginRequest;
import com.aiworkforce.auth.dto.RegisterRequest;
import com.aiworkforce.core.enums.RoleType;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.identity.entity.Account;
import com.aiworkforce.identity.entity.Role;
import com.aiworkforce.identity.repository.AccountRepository;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.RoleRepository;
import com.aiworkforce.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private Role defaultRole;

    @BeforeEach
    public void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@forep.local");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        defaultRole = new Role();
        defaultRole.setName(RoleType.EMPLOYEE);
    }

    @Test
    public void testRegister_Success() {
        when(accountRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleType.EMPLOYEE)).thenReturn(Optional.of(defaultRole));
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashedPassword");
        
        Account savedAccount = new Account();
        savedAccount.setEmail(registerRequest.getEmail());
        savedAccount.setPassword("hashedPassword");
        savedAccount.setRole(defaultRole);
        
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("EMPLOYEE", response.getRole());
        verify(accountRepository, times(1)).save(any(Account.class));
        verify(employeeRepository, times(1)).save(any());
    }

    @Test
    public void testRegister_CustomRole_Success() {
        registerRequest.setRole("ADMIN");
        Role adminRole = new Role();
        adminRole.setName(RoleType.ADMIN);

        when(accountRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleType.ADMIN)).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashedPassword");
        
        Account savedAccount = new Account();
        savedAccount.setEmail(registerRequest.getEmail());
        savedAccount.setPassword("hashedPassword");
        savedAccount.setRole(adminRole);
        
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("ADMIN", response.getRole());
        verify(accountRepository, times(1)).save(any(Account.class));
        verify(employeeRepository, times(1)).save(any());
    }

    @Test
    public void testRegister_Failed_EmailExists() {
        when(accountRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        assertThrows(BusinessException.class, () -> {
            authService.register(registerRequest);
        });

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    public void testLogin_Success() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@forep.local");
        loginRequest.setPassword("password123");

        Account account = new Account();
        account.setEmail("test@forep.local");
        account.setPassword("hashedPassword");
        account.setRole(defaultRole);
        account.setFailedLoginAttempts(2);

        when(accountRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(account));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("EMPLOYEE", response.getRole());
        assertEquals(0, account.getFailedLoginAttempts()); // verified failed attempts reset
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(accountRepository, times(1)).save(account);
    }
}

