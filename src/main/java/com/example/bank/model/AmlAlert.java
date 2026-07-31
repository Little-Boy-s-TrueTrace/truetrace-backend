package com.example.bank.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "aml_alerts")
public class AmlAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String alertId;

    private String triggerTransactionId;
    private String primaryAccountNumber;

    @Enumerated(EnumType.STRING)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    private Double riskScore;

    @Column(columnDefinition = "TEXT")
    private String involvedAccountsJson;

    @Column(columnDefinition = "TEXT")
    private String transactionChainJson;

    @Column(columnDefinition = "TEXT")
    private String graphDataJson;

    @Column(columnDefinition = "TEXT")
    private String agentFindingJson;

    private BigDecimal totalAmount;

    private String currency = "VND";

    private Integer timeWindowSeconds = 60;

    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    private String resolvedBy;
    private String resolutionNotes;

    public AmlAlert() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getTriggerTransactionId() { return triggerTransactionId; }
    public void setTriggerTransactionId(String triggerTransactionId) { this.triggerTransactionId = triggerTransactionId; }

    public String getPrimaryAccountNumber() { return primaryAccountNumber; }
    public void setPrimaryAccountNumber(String primaryAccountNumber) { this.primaryAccountNumber = primaryAccountNumber; }

    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }

    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }

    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }

    public String getInvolvedAccountsJson() { return involvedAccountsJson; }
    public void setInvolvedAccountsJson(String involvedAccountsJson) { this.involvedAccountsJson = involvedAccountsJson; }

    public String getTransactionChainJson() { return transactionChainJson; }
    public void setTransactionChainJson(String transactionChainJson) { this.transactionChainJson = transactionChainJson; }

    public String getGraphDataJson() { return graphDataJson; }
    public void setGraphDataJson(String graphDataJson) { this.graphDataJson = graphDataJson; }

    public String getAgentFindingJson() { return agentFindingJson; }
    public void setAgentFindingJson(String agentFindingJson) { this.agentFindingJson = agentFindingJson; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Integer getTimeWindowSeconds() { return timeWindowSeconds; }
    public void setTimeWindowSeconds(Integer timeWindowSeconds) { this.timeWindowSeconds = timeWindowSeconds; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
}
