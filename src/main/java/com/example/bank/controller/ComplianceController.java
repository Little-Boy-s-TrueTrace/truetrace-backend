package com.example.bank.controller;

import com.example.bank.model.KycStatus;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.AmlAlertRepository;
import com.example.bank.repository.KycSessionRepository;
import com.example.bank.repository.StrReportRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ComplianceController {
    private final KycSessionRepository kyc;
    private final AmlAlertRepository alerts;
    private final StrReportRepository reports;
    private final AccountRepository accounts;

    public ComplianceController(
            KycSessionRepository kyc,
            AmlAlertRepository alerts,
            StrReportRepository reports,
            AccountRepository accounts) {
        this.kyc = kyc;
        this.alerts = alerts;
        this.reports = reports;
        this.accounts = accounts;
    }

    @GetMapping("/compliance/stats")
    public Map<String, Object> stats() {
        long total = kyc.count();
        long approved = kyc.findByStatus(KycStatus.APPROVED).size();
        return Map.of(
                "totalKycProcessed", total,
                "deepfakesDetected", kyc.findByStatus(KycStatus.REJECTED).size(),
                "amlAlertsRaised", alerts.count(),
                "strReportsGenerated", reports.count(),
                "activeFreezes", accounts.countByStatus("FROZEN"),
                "kycApprovalRate", total == 0 ? 0.0 : approved * 100.0 / total,
                "avgProcessingTimeMs", 0
        );
    }

    @GetMapping("/agents/status")
    public List<Map<String, Object>> agentStatus() {
        String kycLastActivity = kyc.findFirstByOrderByIdDesc()
                .map(session -> session.getUpdatedAt() != null
                        ? session.getUpdatedAt()
                        : session.getCreatedAt())
                .filter(java.util.Objects::nonNull)
                .map(LocalDateTime::toString)
                .orElse("never");
        String amlLastActivity = alerts.findFirstByOrderByIdDesc()
                .map(alert -> alert.getCreatedAt())
                .filter(java.util.Objects::nonNull)
                .map(LocalDateTime::toString)
                .orElse("never");
        String strLastActivity = reports.findFirstByOrderByIdDesc()
                .map(report -> report.getGeneratedAt())
                .filter(java.util.Objects::nonNull)
                .map(LocalDateTime::toString)
                .orElse("never");
        return List.of(
                agent("deepfake-inspector", "KYC & Deepfake Inspector", kyc.count(), kycLastActivity),
                agent("money-trail", "Transactions Graph Explorer", alerts.count(), amlLastActivity),
                agent("aml-reporter", "Autonomous STR Report Generator", reports.count(), strLastActivity)
        );
    }

    @GetMapping("/compliance/accounts/{accountNumber}/profile")
    public ResponseEntity<?> accountComplianceProfile(@PathVariable String accountNumber) {
        var account = accounts.findByAccountNumber(accountNumber).orElse(null);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        String customerId = account.getUser().getId().toString();
        var latestKyc = kyc.findFirstByAccountIdOrderByCreatedAtDesc(accountNumber)
                .or(() -> kyc.findFirstByCustomerIdOrderByCreatedAtDesc(customerId));

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("accountNumber", account.getAccountNumber());
        profile.put("accountStatus", account.getStatus());
        profile.put("customerId", customerId);
        profile.put("fullName", account.getUser().getFullName());
        profile.put("email", account.getUser().getEmail());
        latestKyc.ifPresent(session -> {
            profile.put("kycSessionDbId", session.getId());
            profile.put("kycSessionId", session.getSessionId());
            profile.put("kycStatus", session.getStatus());
            profile.put("cccdNumber", session.getCccdNumber());
            profile.put("cccdValid", session.getCccdValid());
        });
        return ResponseEntity.ok(profile);
    }

    private Map<String, Object> agent(String id, String name, long count, String lastActivity) {
        return Map.of(
                "agentId", id,
                "agentName", name,
                "status", "UNKNOWN",
                "lastActivity", lastActivity,
                "processedCount", count
        );
    }
}
