package com.example.bank.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "str_reports")
public class StrReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String reportId;

    private Long alertId;
    private Long kycSessionId;

    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    private String subjectCustomerId;
    private String subjectFullName;
    private String subjectCccdNumber;

    @Column(columnDefinition = "TEXT")
    private String narrativeTextVi;

    @Column(columnDefinition = "TEXT")
    private String narrativeTextEn;

    @Column(columnDefinition = "TEXT")
    private String evidenceSummaryJson;

    @Column(columnDefinition = "TEXT")
    private String transactionDetailsJson;

    private BigDecimal totalAmount;

    private String currency = "VND";

    private String riskLevel;
    private Double riskScore;

    @Column(columnDefinition = "TEXT")
    private String recommendedActionsJson;

    @Column(columnDefinition = "TEXT")
    private String regulatoryReferencesJson;

    private LocalDateTime generatedAt;
    private LocalDateTime submittedAt;

    private String submittedBy;
    private String reviewedBy;

    public StrReport() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }

    public Long getKycSessionId() { return kycSessionId; }
    public void setKycSessionId(Long kycSessionId) { this.kycSessionId = kycSessionId; }

    public ReportType getReportType() { return reportType; }
    public void setReportType(ReportType reportType) { this.reportType = reportType; }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }

    public String getSubjectCustomerId() { return subjectCustomerId; }
    public void setSubjectCustomerId(String subjectCustomerId) { this.subjectCustomerId = subjectCustomerId; }

    public String getSubjectFullName() { return subjectFullName; }
    public void setSubjectFullName(String subjectFullName) { this.subjectFullName = subjectFullName; }

    public String getSubjectCccdNumber() { return subjectCccdNumber; }
    public void setSubjectCccdNumber(String subjectCccdNumber) { this.subjectCccdNumber = subjectCccdNumber; }

    public String getNarrativeTextVi() { return narrativeTextVi; }
    public void setNarrativeTextVi(String narrativeTextVi) { this.narrativeTextVi = narrativeTextVi; }

    public String getNarrativeTextEn() { return narrativeTextEn; }
    public void setNarrativeTextEn(String narrativeTextEn) { this.narrativeTextEn = narrativeTextEn; }

    public String getEvidenceSummaryJson() { return evidenceSummaryJson; }
    public void setEvidenceSummaryJson(String evidenceSummaryJson) { this.evidenceSummaryJson = evidenceSummaryJson; }

    public String getTransactionDetailsJson() { return transactionDetailsJson; }
    public void setTransactionDetailsJson(String transactionDetailsJson) { this.transactionDetailsJson = transactionDetailsJson; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }

    public String getRecommendedActionsJson() { return recommendedActionsJson; }
    public void setRecommendedActionsJson(String recommendedActionsJson) { this.recommendedActionsJson = recommendedActionsJson; }

    public String getRegulatoryReferencesJson() { return regulatoryReferencesJson; }
    public void setRegulatoryReferencesJson(String regulatoryReferencesJson) { this.regulatoryReferencesJson = regulatoryReferencesJson; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
}
