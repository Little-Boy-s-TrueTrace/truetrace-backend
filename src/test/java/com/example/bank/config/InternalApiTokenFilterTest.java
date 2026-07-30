package com.example.bank.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class InternalApiTokenFilterTest {

    private final InternalApiTokenFilter filter = new InternalApiTokenFilter("service-token");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsCustomerAccessToComplianceDataWithoutInternalToken() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/aml/alerts");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void allowsCustomerToListOnlyTheirOwnKycSessions() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/kyc/sessions");
        request.setParameter("mine", "true");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsCustomerKycApprovalWithoutInternalToken() throws Exception {
        var request = new MockHttpServletRequest(
                "POST", "/api/kyc/sessions/session-1/approve");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void authenticatesTrustedDashboardOperator() throws Exception {
        var request = new MockHttpServletRequest("PUT", "/api/str/reports/report-1/status");
        request.addHeader("X-TrueTrace-Internal-Token", "service-token");
        request.addHeader("X-TrueTrace-Operator", "operator.demo");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("operator.demo");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_INTERNAL_SERVICE");
    }
}
