package com.example.bank.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_sessions")
public class KycSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sessionId;

    private String customerId;
    private String accountId;
    private String customerName;

    @Enumerated(EnumType.STRING)
    private KycStatus status;

    private String selfieImagePath;
    private String idFrontImagePath;
    private String idBackImagePath;

    private Integer deepfakeScore;
    private Integer faceMatchScore;
    private Integer documentIntegrityScore;
    private Integer livenessScore;

    private String cccdNumber;
    private Boolean cccdValid;

    @Column(columnDefinition = "TEXT")
    private String agentFindingJson;

    private String riskLevel;
    private String recommendedAction;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String reviewedBy;

    public KycSession() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public KycStatus getStatus() { return status; }
    public void setStatus(KycStatus status) { this.status = status; }
    
    public String getSelfieImagePath() { return selfieImagePath; }
    public void setSelfieImagePath(String selfieImagePath) { this.selfieImagePath = selfieImagePath; }
    
    public String getIdFrontImagePath() { return idFrontImagePath; }
    public void setIdFrontImagePath(String idFrontImagePath) { this.idFrontImagePath = idFrontImagePath; }
    
    public String getIdBackImagePath() { return idBackImagePath; }
    public void setIdBackImagePath(String idBackImagePath) { this.idBackImagePath = idBackImagePath; }
    
    public Integer getDeepfakeScore() { return deepfakeScore; }
    public void setDeepfakeScore(Integer deepfakeScore) { this.deepfakeScore = deepfakeScore; }
    
    public Integer getFaceMatchScore() { return faceMatchScore; }
    public void setFaceMatchScore(Integer faceMatchScore) { this.faceMatchScore = faceMatchScore; }
    
    public Integer getDocumentIntegrityScore() { return documentIntegrityScore; }
    public void setDocumentIntegrityScore(Integer documentIntegrityScore) { this.documentIntegrityScore = documentIntegrityScore; }
    
    public Integer getLivenessScore() { return livenessScore; }
    public void setLivenessScore(Integer livenessScore) { this.livenessScore = livenessScore; }
    
    public String getCccdNumber() { return cccdNumber; }
    public void setCccdNumber(String cccdNumber) { this.cccdNumber = cccdNumber; }
    
    public Boolean getCccdValid() { return cccdValid; }
    public void setCccdValid(Boolean cccdValid) { this.cccdValid = cccdValid; }
    
    public String getAgentFindingJson() { return agentFindingJson; }
    public void setAgentFindingJson(String agentFindingJson) { this.agentFindingJson = agentFindingJson; }
    
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
}
