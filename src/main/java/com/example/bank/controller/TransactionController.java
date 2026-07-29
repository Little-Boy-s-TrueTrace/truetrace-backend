package com.example.bank.controller;

import com.example.bank.model.Account;
import com.example.bank.model.KycStatus;
import com.example.bank.model.Transaction;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.KycSessionRepository;
import com.example.bank.repository.TransactionRepository;
import com.example.bank.service.TransactionMonitorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionMonitorService transactionMonitorService;

    @Autowired
    private KycSessionRepository kycSessionRepository;

    @org.springframework.beans.factory.annotation.Value("${truetrace.kyc.required-for-transfers:false}")
    private boolean kycRequiredForTransfers;

    @PostMapping("/transfer")
    @Transactional
    public ResponseEntity<?> transferMoney(@jakarta.validation.Valid @RequestBody com.example.bank.model.TransferRequest payload, HttpServletRequest servletRequest) {
        String sourceAccountNumber = payload.getSourceAccountNumber();
        String targetAccountNumber = payload.getTargetAccountNumber();
        Double amount = payload.getAmount();
        String description = payload.getDescription();

        if (sourceAccountNumber != null && sourceAccountNumber.equalsIgnoreCase(targetAccountNumber)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot transfer to the same account."));
        }

        Account sourceAccount;
        Account targetAccount;
        if (sourceAccountNumber != null && targetAccountNumber != null) {
            if (sourceAccountNumber.compareTo(targetAccountNumber) < 0) {
                sourceAccount = accountRepository.findByAccountNumberWithLock(sourceAccountNumber).orElse(null);
                targetAccount = accountRepository.findByAccountNumberWithLock(targetAccountNumber).orElse(null);
            } else {
                targetAccount = accountRepository.findByAccountNumberWithLock(targetAccountNumber).orElse(null);
                sourceAccount = accountRepository.findByAccountNumberWithLock(sourceAccountNumber).orElse(null);
            }
        } else {
            sourceAccount = accountRepository.findByAccountNumber(sourceAccountNumber).orElse(null);
            targetAccount = accountRepository.findByAccountNumber(targetAccountNumber).orElse(null);
        }

        if (targetAccount == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Target account not found"));
        }

        if (sourceAccount == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Source account not found"));
        }

        if ("FROZEN".equalsIgnoreCase(sourceAccount.getStatus()) || "FROZEN".equalsIgnoreCase(targetAccount.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "One of the accounts is frozen. Transfer denied."));
        }

        // Verify source account ownership
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!sourceAccount.getUser().getUsername().equalsIgnoreCase(currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden: You do not own the source account."));
        }

        if (kycRequiredForTransfers) {
            String customerId = sourceAccount.getUser().getId().toString();
            boolean approved = kycSessionRepository.existsByAccountIdAndStatus(
                    sourceAccount.getAccountNumber(), KycStatus.APPROVED)
                    || kycSessionRepository.existsByCustomerIdAndStatus(customerId, KycStatus.APPROVED);
            if (!approved) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "KYC approval is required before transfers."));
            }
        }

        if (amount == null || Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Amount must be a valid positive number"));
        }

        // Minimum amount check (at least 0.01)
        if (amount < 0.01) {
            return ResponseEntity.badRequest().body(Map.of("error", "Amount too small. Minimum is 0.01"));
        }

        // Maximum single transfer limit (500 million VND)
        if (amount > 500000000.0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Amount exceeds the maximum single transfer limit."));
        }

        // Decimal precision check (max 2 decimal places)
        double scaled = amount * 100;
        if (Math.abs(scaled - Math.round(scaled)) > 0.001) {
            return ResponseEntity.badRequest().body(Map.of("error", "Amount must have at most 2 decimal places."));
        }

        if (sourceAccount.getBalance() < amount) {
            return ResponseEntity.badRequest().body(Map.of("error", "Insufficient balance."));
        }

        sourceAccount.setBalance(sourceAccount.getBalance() - amount);
        targetAccount.setBalance(targetAccount.getBalance() + amount);

        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        Transaction tx = Transaction.builder()
                .sourceAccountNumber(sourceAccountNumber)
                .targetAccountNumber(targetAccountNumber)
                .amount(amount)
                .description(description)
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        tx = transactionRepository.save(tx);

        transactionMonitorService.monitorTransfer(tx);

        return ResponseEntity.ok(Map.of(
                "message", "Transfer completed successfully",
                "transactionId", tx.getId(),
                "sourceBalance", sourceAccount.getBalance()
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getTransactionHistory(
            @RequestParam String accountNumber,
            @RequestParam(required = false) String search,
            HttpServletRequest servletRequest) {

        Account myAccount = accountRepository.findByAccountNumber(accountNumber).orElse(null);
        if (myAccount == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Account not found"));
        }

        // Verify ownership
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!myAccount.getUser().getUsername().equalsIgnoreCase(currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied."));
        }

        List<Transaction> transactions;

        if (search == null || search.trim().isEmpty()) {
            transactions = transactionRepository
                    .findBySourceAccountNumberOrTargetAccountNumberOrderByTimestampDesc(accountNumber, accountNumber);
        } else {
            transactions = transactionRepository.searchTransactionsSecure(accountNumber, search);
        }

        return ResponseEntity.ok(transactions);
    }
}
