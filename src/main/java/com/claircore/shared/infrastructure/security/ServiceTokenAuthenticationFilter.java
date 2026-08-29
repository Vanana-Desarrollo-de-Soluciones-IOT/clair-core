package com.claircore.shared.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class ServiceTokenAuthenticationFilter extends OncePerRequestFilter {
    private static final String HEADER = "X-Core-Token";
    private static final String[] PATHS = {"/api/v1/edge/**", "/api/v1/evaluations/telemetry/batch"};
    private final String token;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public ServiceTokenAuthenticationFilter(@Value("${EDGE_TO_CORE_TOKEN:}") String token) { this.token = token; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        for (String path : PATHS) if (matcher.match(path, request.getRequestURI())) return false;
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String supplied = request.getHeader(HEADER);
        // The roster shipped in plan 02 used X-Edge-Token; accept it during migration.
        if (supplied == null && request.getRequestURI().equals("/api/v1/edge/devices")) supplied = request.getHeader("X-Edge-Token");
        // A missing application secret must never turn integration endpoints into public endpoints.
        if (token.isBlank() || supplied == null || !MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        chain.doFilter(request, response);
    }
}
