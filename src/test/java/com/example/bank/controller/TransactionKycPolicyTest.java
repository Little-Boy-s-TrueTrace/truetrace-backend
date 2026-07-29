package com.example.bank.controller;

import com.example.bank.model.Account;
import com.example.bank.model.KycStatus;
import com.example.bank.model.Transaction;
import com.example.bank.model.TransferRequest;
import com.example.bank.model.User;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.KycSessionRepository;
import com.example.bank.repository.TransactionRepository;
import com.example.bank.service.EventPublisher;
import com.example.bank.service.TransactionMonitorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionKycPolicyTest {
    @Mock TransactionRepository transactions;
    @Mock AccountRepository accounts;
    @Mock KycSessionRepository kycSessions;

    private TransactionController controller;
    private Account source;
    private Account target;
    private CapturingTransactionMonitor monitor;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .id(42L)
                .username("owner")
                .password("encoded")
                .fullName("Owner")
                .email("owner@example.com")
                .role("USER")
                .build();
        User recipient = User.builder()
                .id(43L)
                .username("recipient")
                .password("encoded")
                .fullName("Recipient")
                .email("recipient@example.com")
                .role("USER")
                .build();
        source = Account.builder()
                .id(1L)
                .accountNumber("ACC-100001")
                .balance(1_000_000.0)
                .currency("VND")
                .status("ACTIVE")
                .user(owner)
                .build();
        target = Account.builder()
                .id(2L)
                .accountNumber("ACC-100002")
                .balance(1_000_000.0)
                .currency("VND")
                .status("ACTIVE")
                .user(recipient)
                .build();
        controller = new TransactionController();
        monitor = new CapturingTransactionMonitor();
        ReflectionTestUtils.setField(controller, "transactionRepository", transactions);
        ReflectionTestUtils.setField(controller, "accountRepository", accounts);
        ReflectionTestUtils.setField(controller, "transactionMonitorService", monitor);
        ReflectionTestUtils.setField(controller, "kycSessionRepository", kycSessions);
        ReflectionTestUtils.setField(controller, "kycRequiredForTransfers", true);
        when(accounts.findByAccountNumberWithLock("ACC-100001")).thenReturn(Optional.of(source));
        when(accounts.findByAccountNumberWithLock("ACC-100002")).thenReturn(Optional.of(target));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner", null));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksTransferWhenCustomerHasNoApprovedKyc() {
        var response = controller.transferMoney(request(), null);

        assertEquals(403, response.getStatusCode().value());
        verify(transactions, never()).save(any());
    }

    @Test
    void allowsTransferWhenCustomerHasApprovedKyc() {
        when(kycSessions.existsByAccountIdAndStatus("ACC-100001", KycStatus.APPROVED))
                .thenReturn(true);
        when(transactions.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(99L);
            return transaction;
        });

        var response = controller.transferMoney(request(), null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(99L, monitor.lastTransaction.getId());
    }

    private TransferRequest request() {
        return new TransferRequest(
                "ACC-100001",
                "ACC-100002",
                100_000.0,
                "KYC policy test");
    }

    private static final class CapturingTransactionMonitor extends TransactionMonitorService {
        private Transaction lastTransaction;

        private CapturingTransactionMonitor() {
            super(new EventPublisher(null));
        }

        @Override
        public void monitorTransfer(Transaction tx) {
            lastTransaction = tx;
        }
    }
}
