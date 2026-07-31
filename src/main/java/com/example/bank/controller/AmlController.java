package com.example.bank.controller;

import com.example.bank.model.AmlAlert;
import com.example.bank.model.AlertStatus;
import com.example.bank.model.Account;
import com.example.bank.repository.AmlAlertRepository;
import com.example.bank.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/aml")
public class AmlController {

    @Autowired
    private AmlAlertRepository amlAlertRepository;

    @Autowired
    private AccountRepository accountRepository;

    @GetMapping("/alerts")
    public ResponseEntity<List<AmlAlert>> listAlerts(@RequestParam(required = false) AlertStatus status) {
        if (status != null) {
            return ResponseEntity.ok(amlAlertRepository.findByStatus(status));
        }
        return ResponseEntity.ok(amlAlertRepository.findAll());
    }

    @GetMapping("/alerts/{alertId}")
    public ResponseEntity<?> getAlert(@PathVariable String alertId) {
        return amlAlertRepository.findByAlertId(alertId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/alerts")
    public ResponseEntity<?> createAlert(@RequestBody AmlAlert alert) {
        alert.setAlertId(UUID.randomUUID().toString());
        alert.setStatus(AlertStatus.OPEN);
        alert.setCreatedAt(LocalDateTime.now());
        if (alert.getTimeWindowSeconds() == null) {
            alert.setTimeWindowSeconds(60);
        }
        amlAlertRepository.save(alert);
        return ResponseEntity.ok(alert);
    }

    @PutMapping("/alerts/{alertId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String alertId, @RequestBody AmlAlert updated) {
        return amlAlertRepository.findByAlertId(alertId).map(alert -> {
            alert.setStatus(updated.getStatus());
            alert.setAgentFindingJson(updated.getAgentFindingJson());
            alert.setRiskScore(updated.getRiskScore());
            alert.setGraphDataJson(updated.getGraphDataJson());
            alert.setInvolvedAccountsJson(updated.getInvolvedAccountsJson());
            alert.setTransactionChainJson(updated.getTransactionChainJson());
            if (updated.getStatus() == AlertStatus.CLOSED || updated.getStatus() == AlertStatus.FALSE_POSITIVE) {
                alert.setResolvedAt(LocalDateTime.now());
                alert.setResolvedBy(updated.getResolvedBy());
                alert.setResolutionNotes(updated.getResolutionNotes());
            }
            amlAlertRepository.save(alert);
            return ResponseEntity.ok(alert);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/alerts/{alertId}/escalate")
    public ResponseEntity<?> escalateAlert(@PathVariable String alertId) {
        return amlAlertRepository.findByAlertId(alertId).map(alert -> {
            alert.setStatus(AlertStatus.ESCALATED);
            amlAlertRepository.save(alert);
            return ResponseEntity.ok(alert);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/alerts/{alertId}/close")
    public ResponseEntity<?> closeAlert(@PathVariable String alertId, @RequestBody Map<String, String> body) {
        return amlAlertRepository.findByAlertId(alertId).map(alert -> {
            alert.setStatus(AlertStatus.CLOSED);
            alert.setResolvedAt(LocalDateTime.now());
            alert.setResolutionNotes(body.get("notes"));
            amlAlertRepository.save(alert);
            return ResponseEntity.ok(alert);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/freeze/{accountNumber}")
    public ResponseEntity<?> freezeAccount(@PathVariable String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).map(account -> {
            account.setStatus("FROZEN");
            accountRepository.save(account);
            return ResponseEntity.ok(Map.of("message", "Account frozen", "accountNumber", accountNumber));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/unfreeze/{accountNumber}")
    public ResponseEntity<?> unfreezeAccount(@PathVariable String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).map(account -> {
            account.setStatus("ACTIVE");
            accountRepository.save(account);
            return ResponseEntity.ok(Map.of("message", "Account unfrozen", "accountNumber", accountNumber));
        }).orElse(ResponseEntity.notFound().build());
    }
}
