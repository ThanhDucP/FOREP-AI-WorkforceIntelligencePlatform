package com.aiworkforce.platform.auth.service;

import com.aiworkforce.platform.account.entity.Account;
import com.aiworkforce.platform.account.repository.AccountRepository;
import com.aiworkforce.platform.auth.dto.AuthDto.*;
import com.aiworkforce.platform.common.exception.BusinessException;
import com.aiworkforce.platform.security.entity.Role;
import com.aiworkforce.platform.security.jwt.JwtService;
import com.aiworkforce.platform.security.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username is already taken");
        }
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email is already in use");
        }

        Role role = roleRepository.findByName("EMPLOYEE")
                .orElseThrow(() -> new BusinessException("Default role not found"));

        Account account = Account.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .isActive(true)
                .build();

        accountRepository.save(account);

        String jwtToken = jwtService.generateToken(account);
        String refreshToken = jwtService.generateRefreshToken(account);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .username(account.getUsername())
                .role(role.getName())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        Account account = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!account.isActive()) {
            throw new BusinessException("Account is inactive");
        }

        String jwtToken = jwtService.generateToken(account);
        String refreshToken = jwtService.generateRefreshToken(account);

        account.setRefreshToken(refreshToken);
        account.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        account.setLastLoginAt(LocalDateTime.now());
        accountRepository.save(account);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .username(account.getUsername())
                .role(account.getRole().getName())
                .build();
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String username = jwtService.extractUsername(request.getRefreshToken());
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (jwtService.isTokenValid(request.getRefreshToken(), account)) {
            String accessToken = jwtService.generateToken(account);
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(request.getRefreshToken())
                    .username(account.getUsername())
                    .role(account.getRole().getName())
                    .build();
        }
        throw new BusinessException("Session expired");
    }
}
