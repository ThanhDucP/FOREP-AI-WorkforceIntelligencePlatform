package com.aiworkforce.security.oauth2;

import com.aiworkforce.auth.dto.AuthResponse;
import com.aiworkforce.auth.service.OAuth2SocialAuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2SocialAuthService socialAuthService;
    private final OAuth2Properties oauth2Properties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        AuthResponse authResponse = socialAuthService.loginWithOAuth2(
                oauth2Token.getAuthorizedClientRegistrationId(),
                oauth2Token.getPrincipal().getAttributes()
        );

        String redirectUrl = UriComponentsBuilder
                .fromUriString(oauth2Properties.getSuccessRedirectUri())
                .queryParam("token", authResponse.getToken())
                .queryParam("type", authResponse.getType())
                .queryParam("role", authResponse.getRole())
                .build(true)
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}