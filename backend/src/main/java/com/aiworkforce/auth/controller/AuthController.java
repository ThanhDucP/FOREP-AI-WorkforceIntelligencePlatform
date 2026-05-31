package com.aiworkforce.auth.controller;

import com.aiworkforce.auth.dto.AuthResponse;
import com.aiworkforce.auth.dto.LoginRequest;
import com.aiworkforce.auth.dto.OAuth2LoginLinksResponse;
import com.aiworkforce.auth.dto.RegisterRequest;
import com.aiworkforce.auth.service.AuthService;
import com.aiworkforce.core.response.ApiResponse;
import com.aiworkforce.identity.dto.EmployeeResponse;
import com.aiworkforce.identity.service.EmployeeService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmployeeService employeeService;

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
                .google(baseUrl + "/api/v1/auth/oauth2/google")
                .github(baseUrl + "/api/v1/auth/oauth2/github")
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "OAuth2 login links"));
    }

    @GetMapping("/oauth2/google")
    public void googleLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/oauth2/github")
    public void githubLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/github");
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
