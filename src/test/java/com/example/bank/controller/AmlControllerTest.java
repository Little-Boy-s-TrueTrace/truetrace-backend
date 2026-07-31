package com.example.bank.controller;

import com.example.bank.model.Account;
import com.example.bank.model.AmlAlert;
import com.example.bank.model.AlertStatus;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.AmlAlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AmlControllerTest {

    @Mock
    private AmlAlertRepository amlAlertRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AmlController amlController;

    @Test
    void listAlertsPopulatesAccountStatus() {
        AmlAlert alert = new AmlAlert();
        alert.setAlertId("alert-001");
        alert.setPrimaryAccountNumber("ACC-10001");
        alert.setStatus(AlertStatus.OPEN);

        Account account = Account.builder()
                .accountNumber("ACC-10001")
                .status("FROZEN")
                .build();

        when(amlAlertRepository.findAll()).thenReturn(List.of(alert));
        when(accountRepository.findByAccountNumber("ACC-10001")).thenReturn(Optional.of(account));

        ResponseEntity<List<AmlAlert>> response = amlController.listAlerts(null);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("FROZEN", response.getBody().get(0).getAccountStatus());
    }

    @Test
    void freezeAccountUpdatesStatusToFrozen() {
        Account account = Account.builder()
                .accountNumber("ACC-10002")
                .status("ACTIVE")
                .build();

        when(accountRepository.findByAccountNumber("ACC-10002")).thenReturn(Optional.of(account));

        ResponseEntity<?> response = amlController.freezeAccount("ACC-10002");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("FROZEN", account.getStatus());
        verify(accountRepository).save(account);
    }

    @Test
    void unfreezeAccountUpdatesStatusToActive() {
        Account account = Account.builder()
                .accountNumber("ACC-10003")
                .status("FROZEN")
                .build();

        when(accountRepository.findByAccountNumber("ACC-10003")).thenReturn(Optional.of(account));

        ResponseEntity<?> response = amlController.unfreezeAccount("ACC-10003");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("ACTIVE", account.getStatus());
        verify(accountRepository).save(account);
    }

    @Test
    void escalateAlertUpdatesStatusToEscalated() {
        AmlAlert alert = new AmlAlert();
        alert.setAlertId("alert-002");
        alert.setStatus(AlertStatus.OPEN);

        when(amlAlertRepository.findByAlertId("alert-002")).thenReturn(Optional.of(alert));

        ResponseEntity<?> response = amlController.escalateAlert("alert-002");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(AlertStatus.ESCALATED, alert.getStatus());
        verify(amlAlertRepository).save(alert);
    }
}
