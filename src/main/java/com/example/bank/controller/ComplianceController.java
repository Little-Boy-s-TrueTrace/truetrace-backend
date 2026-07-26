package com.example.bank.controller;

import com.example.bank.model.KycStatus;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.AmlAlertRepository;
import com.example.bank.repository.KycSessionRepository;
import com.example.bank.repository.StrReportRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
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
        String now = Instant.now().toString();
        return List.of(
                agent("deepfake-inspector", "KYC & Deepfake Inspector", kyc.count(), now),
                agent("money-trail", "Transactions Graph Explorer", alerts.count(), now),
                agent("aml-reporter", "Autonomous STR Report Generator", reports.count(), now)
        );
    }

    private Map<String, Object> agent(String id, String name, long count, String now) {
        return Map.of(
                "agentId", id,
                "agentName", name,
                "status", "RUNNING",
                "lastActivity", now,
                "processedCount", count,
                "errorCount", 0,
                "queueDepth", 0
        );
    }
}
