package com.example.bank.controller;

import com.example.bank.model.Account;
import com.example.bank.model.KycSession;
import com.example.bank.model.User;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.KycSessionRepository;
import com.example.bank.repository.UserRepository;
import com.example.bank.service.EventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycControllerTest {
    @Mock KycSessionRepository kycSessions;
    @Mock UserRepository users;
    @Mock AccountRepository accounts;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createSessionBindsCurrentCustomerAndPublishesAllEvidence() throws Exception {
        User user = User.builder()
                .id(42L)
                .username("demo-user")
                .password("encoded")
                .fullName("Nguyen Van A")
                .email("demo@example.com")
                .role("USER")
                .build();
        Account account = Account.builder()
                .id(7L)
                .accountNumber("ACC-424242")
                .balance(500_000_000.0)
                .currency("VND")
                .status("ACTIVE")
                .user(user)
                .build();
        when(users.findByUsername("demo-user")).thenReturn(Optional.of(user));
        when(accounts.findByUser(user)).thenReturn(Optional.of(account));
        when(kycSessions.save(any(KycSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("demo-user", null));
        CapturingEventPublisher events = new CapturingEventPublisher();
        KycController controller = new KycController(kycSessions, users, accounts, events);

        var response = controller.createSession(
                "Spoofed Name",
                "001200000001",
                image("selfie", "selfie-bytes"),
                image("front", "front-bytes"),
                image("back", "back-bytes"));

        KycSession saved = (KycSession) response.getBody();
        assertNotNull(saved);
        assertEquals("42", saved.getCustomerId());
        assertEquals("ACC-424242", saved.getAccountId());
        assertEquals("Nguyen Van A", saved.getCustomerName());
        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) events.lastKycEvent;
        assertEquals("42", event.get("customer_id"));
        assertEquals("ACC-424242", event.get("account_id"));
        assertEquals("selfie.png", event.get("selfie_filename"));
        assertNotNull(event.get("face_image_base64"));
        assertNotNull(event.get("id_front_image_base64"));
        assertNotNull(event.get("id_back_image_base64"));
    }

    private MockMultipartFile image(String name, String content) {
        return new MockMultipartFile(
                name,
                name + ".png",
                "image/png",
                content.getBytes(StandardCharsets.UTF_8));
    }

    private static final class CapturingEventPublisher extends EventPublisher {
        private Object lastKycEvent;

        private CapturingEventPublisher() {
            super(null);
        }

        @Override
        public void publishKycEvent(Object event) {
            lastKycEvent = event;
        }
    }
}
