package com.example.bank.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

@Component
public class InternalApiTokenFilter extends OncePerRequestFilter {
    private static final Set<String> AGENT_MUTATION_PREFIXES = Set.of(
            "/api/aml/freeze/",
            "/api/aml/alerts",
            "/api/str/reports",
            "/api/kyc/sessions/"
    );
    private final String expectedToken = System.getenv("TRUETRACE_SECURITY_SYNC_TOKEN");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (expectedToken == null || expectedToken.isBlank()) {
            return true;
        }
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("GET".equals(method)) {
            return true;
        }
        if (path.startsWith("/api/kyc/sessions/") && !path.endsWith("/status")) {
            return true;
        }
        return AGENT_MUTATION_PREFIXES.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String actual = request.getHeader("X-TrueTrace-Internal-Token");
        boolean matches = actual != null && MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
        if (!matches) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal service token");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
