package com.example.bank.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class InternalApiTokenFilter extends OncePerRequestFilter {

    private final String expectedToken;

    public InternalApiTokenFilter(
            @Value("${truetrace.security.sync-token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (expectedToken == null || expectedToken.isBlank()) {
            return true;
        }
        if (request.getHeader("X-TrueTrace-Internal-Token") != null) {
            return false;
        }
        String method = request.getMethod();
        String path = request.getRequestURI();
        boolean customerKycList = "GET".equals(method)
                && path.equals("/api/kyc/sessions")
                && "true".equalsIgnoreCase(request.getParameter("mine"));
        boolean requiresInternalToken = path.startsWith("/api/aml/")
                || path.equals("/api/aml")
                || path.startsWith("/api/str/")
                || path.equals("/api/str")
                || path.startsWith("/api/compliance/")
                || path.startsWith("/api/agents/")
                || ("GET".equals(method)
                    && path.equals("/api/kyc/sessions")
                    && !customerKycList)
                || ("POST".equals(method)
                    && path.startsWith("/api/kyc/sessions/")
                    && (path.endsWith("/approve") || path.endsWith("/reject")))
                || ("PUT".equals(method)
                    && path.startsWith("/api/kyc/sessions/")
                    && path.endsWith("/status"));
        return !requiresInternalToken;
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
        String operator = request.getHeader("X-TrueTrace-Operator");
        String principal = operator != null && operator.matches("[A-Za-z0-9._-]{1,64}")
                ? operator
                : "truetrace-internal-service";
        var authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
