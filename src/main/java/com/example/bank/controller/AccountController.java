package com.example.bank.controller;

import com.example.bank.model.Account;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountController(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/recipients")
    public ResponseEntity<?> listRecipients() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        var currentUser = userRepository.findByUsername(currentUsername).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authenticated user not found"));
        }
        var currentAccount = accountRepository.findByUser(currentUser).orElse(null);
        if (currentAccount == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Current account not found"));
        }

        List<Map<String, String>> recipients = accountRepository
                .findByAccountNumberNotOrderByAccountNumberAsc(currentAccount.getAccountNumber())
                .stream()
                .filter(account -> "ACTIVE".equalsIgnoreCase(account.getStatus()))
                .map(account -> Map.of(
                        "accountNumber", account.getAccountNumber(),
                        "fullName", account.getUser().getFullName()))
                .toList();
        return ResponseEntity.ok(recipients);
    }

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
