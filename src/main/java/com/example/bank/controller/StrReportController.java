package com.example.bank.controller;

import com.example.bank.model.StrReport;
import com.example.bank.model.ReportStatus;
import com.example.bank.repository.StrReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/str")
public class StrReportController {

    @Autowired
    private StrReportRepository strReportRepository;

    @GetMapping("/reports")
    public ResponseEntity<List<StrReport>> listReports(@RequestParam(required = false) ReportStatus status) {
        if (status != null) {
            return ResponseEntity.ok(strReportRepository.findByStatus(status));
        }
        return ResponseEntity.ok(strReportRepository.findAll());
    }

    @GetMapping("/reports/{reportId}")
    public ResponseEntity<?> getReport(@PathVariable String reportId) {
        return strReportRepository.findByReportId(reportId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reports")
    public ResponseEntity<?> createReport(@RequestBody StrReport report) {
        report.setReportId(UUID.randomUUID().toString());
        report.setStatus(ReportStatus.DRAFT);
        report.setGeneratedAt(LocalDateTime.now());
        strReportRepository.save(report);
        return ResponseEntity.ok(report);
    }

    @PutMapping("/reports/{reportId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String reportId, @RequestBody StrReport updated,
            java.security.Principal principal) {
        var existing = strReportRepository.findByReportId(reportId);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        StrReport report = existing.get();
        ReportStatus requested = updated.getStatus();
        if (requested == null) {
            return ResponseEntity.badRequest().body("A target status is required");
        }
        if (report.getStatus() == ReportStatus.SUBMITTED || report.getStatus() == ReportStatus.ARCHIVED) {
            return ResponseEntity.status(409).body("A submitted or archived STR is immutable");
        }
        if (requested == ReportStatus.SUBMITTED || requested == ReportStatus.ARCHIVED) {
            return ResponseEntity.status(409).body("Use the reviewed submit workflow for final submission");
        }
        report.setStatus(requested);
        report.setNarrativeTextVi(updated.getNarrativeTextVi());
        report.setNarrativeTextEn(updated.getNarrativeTextEn());
        report.setEvidenceSummaryJson(updated.getEvidenceSummaryJson());
        report.setTransactionDetailsJson(updated.getTransactionDetailsJson());
        report.setRiskLevel(updated.getRiskLevel());
        report.setRiskScore(updated.getRiskScore());
        report.setRecommendedActionsJson(updated.getRecommendedActionsJson());
        report.setRegulatoryReferencesJson(updated.getRegulatoryReferencesJson());
        if (requested == ReportStatus.READY_FOR_REVIEW) {
            report.setReviewedBy(principal.getName());
        }
        strReportRepository.save(report);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/reports/{reportId}/submit")
    public ResponseEntity<?> submitReport(@PathVariable String reportId, java.security.Principal principal) {
        var existing = strReportRepository.findByReportId(reportId);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        StrReport report = existing.get();
        if (report.getStatus() != ReportStatus.READY_FOR_REVIEW) {
            return ResponseEntity.status(409)
                    .body("STR must be reviewed and marked READY_FOR_REVIEW before submission");
        }
        report.setStatus(ReportStatus.SUBMITTED);
        report.setSubmittedAt(LocalDateTime.now());
        report.setSubmittedBy(principal.getName());
        strReportRepository.save(report);
        return ResponseEntity.ok(report);
    }
}
