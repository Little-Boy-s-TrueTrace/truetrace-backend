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
        if (report.getStatus() == null) {
            report.setStatus(ReportStatus.DRAFT);
        }
        report.setGeneratedAt(LocalDateTime.now());
        strReportRepository.save(report);
        return ResponseEntity.ok(report);
    }

    @PutMapping("/reports/{reportId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String reportId, @RequestBody StrReport updated) {
        return strReportRepository.findByReportId(reportId).map(report -> {
            report.setStatus(updated.getStatus());
            report.setNarrativeTextVi(updated.getNarrativeTextVi());
            report.setNarrativeTextEn(updated.getNarrativeTextEn());
            report.setEvidenceSummaryJson(updated.getEvidenceSummaryJson());
            report.setTransactionDetailsJson(updated.getTransactionDetailsJson());
            report.setRiskLevel(updated.getRiskLevel());
            report.setRiskScore(updated.getRiskScore());
            report.setRecommendedActionsJson(updated.getRecommendedActionsJson());
            report.setRegulatoryReferencesJson(updated.getRegulatoryReferencesJson());
            strReportRepository.save(report);
            return ResponseEntity.ok(report);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reports/{reportId}/submit")
    public ResponseEntity<?> submitReport(@PathVariable String reportId) {
        return strReportRepository.findByReportId(reportId).map(report -> {
            report.setStatus(ReportStatus.SUBMITTED);
            report.setSubmittedAt(LocalDateTime.now());
            strReportRepository.save(report);
            return ResponseEntity.ok(report);
        }).orElse(ResponseEntity.notFound().build());
    }
}
