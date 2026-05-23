package com.aiworkforce.security.filter;

import com.aiworkforce.security.jwt.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();

        // Public endpoints only
        return path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/register")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars")
                || path.equals("/swagger-ui.html")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // No Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {

            log.debug("No Bearer token found for request: {}", request.getRequestURI());

            filterChain.doFilter(request, response);
            return;
        }

        try {

            final String jwt = authHeader.substring(7).trim();

            log.debug("JWT received for request: {}", request.getRequestURI());

            final String userEmail = jwtService.extractUsername(jwt);

            log.debug("Extracted user email: {}", userEmail);

            if (userEmail != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authToken);

                    log.debug("User authenticated successfully: {}", userEmail);

                } else {

                    log.warn("Invalid JWT token for user: {}", userEmail);
                }
            }

        } catch (ExpiredJwtException ex) {

            log.warn("JWT token expired: {}", ex.getMessage());

            SecurityContextHolder.clearContext();

        } catch (JwtException ex) {

            log.warn("JWT token invalid: {}", ex.getMessage());

            SecurityContextHolder.clearContext();

        } catch (Exception ex) {

            log.error("JWT authentication failed", ex);

            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}