package com.example.bank.controller;

import com.example.bank.model.Account;
import com.example.bank.repository.AccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountRepository accountRepository;

    @GetMapping("/{accountNumber}/details")
    public ResponseEntity<?> getAccountDetails(@PathVariable String accountNumber, HttpServletRequest servletRequest) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        Account account = accountRepository.findByAccountNumber(accountNumber).orElse(null);
        if (account == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Account not found"));
        }

        // Secure Mode: Verify if the logged-in user owns the requested account
        if (!account.getUser().getUsername().equalsIgnoreCase(currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Access denied. You do not own this account."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "accountNumber", account.getAccountNumber(),
                "fullName", account.getUser().getFullName(),
                "balance", account.getBalance(),
                "currency", account.getCurrency(),
                "email", account.getUser().getEmail(),
                "status", account.getStatus() != null ? account.getStatus() : "ACTIVE"
        ));
    }
}
