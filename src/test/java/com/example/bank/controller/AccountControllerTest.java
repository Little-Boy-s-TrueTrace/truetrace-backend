package com.example.bank.controller;

import com.example.bank.model.Account;
import com.example.bank.model.User;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {
    @Mock AccountRepository accounts;
    @Mock UserRepository users;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recipientsAreDynamicAndExcludeCurrentOrInactiveAccounts() {
        User owner = user(1L, "owner", "Owner");
        User activeRecipient = user(2L, "recipient", "Active Recipient");
        User frozenRecipient = user(3L, "frozen", "Frozen Recipient");
        Account current = account("ACC-100001", "ACTIVE", owner);
        Account active = account("ACC-100002", "ACTIVE", activeRecipient);
        Account frozen = account("ACC-100003", "FROZEN", frozenRecipient);
        when(users.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(accounts.findByUser(owner)).thenReturn(Optional.of(current));
        when(accounts.findByAccountNumberNotOrderByAccountNumberAsc("ACC-100001"))
                .thenReturn(List.of(active, frozen));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner", null));

        var response = new AccountController(accounts, users).listRecipients();

        @SuppressWarnings("unchecked")
        List<Map<String, String>> body = (List<Map<String, String>>) response.getBody();
        assertEquals(1, body.size());
        assertEquals("ACC-100002", body.get(0).get("accountNumber"));
        assertEquals("Active Recipient", body.get(0).get("fullName"));
    }

    private User user(Long id, String username, String fullName) {
        return User.builder()
                .id(id)
                .username(username)
                .password("encoded")
                .fullName(fullName)
                .email(username + "@example.com")
                .role("USER")
                .build();
    }

    private Account account(String number, String status, User user) {
        return Account.builder()
                .accountNumber(number)
                .balance(1_000_000.0)
                .currency("VND")
                .status(status)
                .user(user)
                .build();
    }
}
