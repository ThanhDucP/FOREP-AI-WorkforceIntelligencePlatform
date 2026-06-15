package com.aiworkforce.auth.controller;

import com.aiworkforce.auth.dto.AuthResponse;
import com.aiworkforce.auth.dto.LoginRequest;
import com.aiworkforce.auth.dto.OAuth2LoginLinksResponse;
import com.aiworkforce.auth.dto.RegisterRequest;
import com.aiworkforce.auth.service.AuthService;
import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.dto.EmployeeResponse;
import com.aiworkforce.identity.service.EmployeeService;
import com.aiworkforce.security.oauth2.OAuth2Properties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmployeeService employeeService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;
    private final OAuth2Properties oauth2Properties;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @GetMapping("/oauth2/links")
    public ResponseEntity<ApiResponse<OAuth2LoginLinksResponse>> getOAuth2Links(HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());

        OAuth2LoginLinksResponse response = OAuth2LoginLinksResponse.builder()
                .google(isOAuth2ProviderConfigured("google") ? baseUrl + "/api/v1/auth/oauth2/google" : null)
                .github(isOAuth2ProviderConfigured("github") ? baseUrl + "/api/v1/auth/oauth2/github" : null)
                .jira(isOAuth2ProviderConfigured("jira") ? baseUrl + "/api/v1/auth/oauth2/jira" : null)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "OAuth2 login links"));
    }

    @GetMapping("/oauth2/google")
    public void googleLogin(HttpServletResponse response) throws IOException {
        if (!isOAuth2ProviderConfigured("google")) {
            response.sendError(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Google OAuth2 is not configured. Set SPRING_PROFILES_ACTIVE=dev,oauth2-google and provide GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET."
            );
            return;
        }

        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/oauth2/github")
    public void githubLogin(HttpServletResponse response) throws IOException {
        if (!isOAuth2ProviderConfigured("github")) {
            response.sendError(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "GitHub OAuth2 is not configured. Set SPRING_PROFILES_ACTIVE=dev,oauth2-github and provide GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET."
            );
            return;
        }

        response.sendRedirect("/oauth2/authorization/github");
    }

    @GetMapping("/oauth2/jira")
    public void jiraLogin(HttpServletResponse response) throws IOException {
        if (!isOAuth2ProviderConfigured("jira")) {
            response.sendError(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Jira OAuth2 is not configured. Set SPRING_PROFILES_ACTIVE=dev,oauth2-jira and provide JIRA_CLIENT_ID and JIRA_CLIENT_SECRET."
            );
            return;
        }

        response.sendRedirect("/oauth2/authorization/jira");
    }

    private boolean isOAuth2ProviderConfigured(String registrationId) {
        if (!oauth2Properties.isEnabled()) {
            return false;
        }

        ClientRegistrationRepository repository = clientRegistrationRepository.getIfAvailable();

        return repository != null && repository.findByRegistrationId(registrationId) != null;
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getMe() {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getCurrentEmployeeProfile(), "Current User Details"));
    }
}
