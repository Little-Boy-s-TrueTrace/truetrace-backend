package com.example.bank.controller;

import com.example.bank.model.KycSession;
import com.example.bank.model.KycStatus;
import com.example.bank.repository.KycSessionRepository;
import com.example.bank.service.EventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/kyc")
public class KycController {

    @Autowired
    private KycSessionRepository kycSessionRepository;

    @Autowired
    private EventPublisher eventPublisher;

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(
            @RequestParam("customerName") String customerName,
            @RequestParam("cccdNumber") String cccdNumber,
            @RequestParam(value = "selfie", required = false) MultipartFile selfie,
            @RequestParam(value = "idFront", required = false) MultipartFile idFront,
            @RequestParam(value = "idBack", required = false) MultipartFile idBack) {
        
        KycSession session = new KycSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setCustomerName(customerName);
        session.setCccdNumber(cccdNumber);
        session.setStatus(KycStatus.PENDING);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        
        // Mock save paths
        session.setSelfieImagePath("path/to/selfie/" + session.getSessionId());
        session.setIdFrontImagePath("path/to/front/" + session.getSessionId());
        session.setIdBackImagePath("path/to/back/" + session.getSessionId());
        
        kycSessionRepository.save(session);
        eventPublisher.publishKycEvent(session);
        
        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<KycSession>> listSessions(@RequestParam(required = false) KycStatus status) {
        if (status != null) {
            return ResponseEntity.ok(kycSessionRepository.findByStatus(status));
        }
        return ResponseEntity.ok(kycSessionRepository.findAll());
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable String sessionId) {
        return kycSessionRepository.findBySessionId(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/sessions/{sessionId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String sessionId, @RequestBody KycSession updated) {
        return kycSessionRepository.findBySessionId(sessionId).map(session -> {
            session.setStatus(updated.getStatus());
            session.setAgentFindingJson(updated.getAgentFindingJson());
            session.setRiskLevel(updated.getRiskLevel());
            session.setRecommendedAction(updated.getRecommendedAction());
            session.setDeepfakeScore(updated.getDeepfakeScore());
            session.setFaceMatchScore(updated.getFaceMatchScore());
            session.setDocumentIntegrityScore(updated.getDocumentIntegrityScore());
            session.setLivenessScore(updated.getLivenessScore());
            session.setUpdatedAt(LocalDateTime.now());
            kycSessionRepository.save(session);
            return ResponseEntity.ok(session);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/sessions/{sessionId}/approve")
    public ResponseEntity<?> approveSession(@PathVariable String sessionId) {
        return kycSessionRepository.findBySessionId(sessionId).map(session -> {
            session.setStatus(KycStatus.APPROVED);
            session.setUpdatedAt(LocalDateTime.now());
            kycSessionRepository.save(session);
            return ResponseEntity.ok(session);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/sessions/{sessionId}/reject")
    public ResponseEntity<?> rejectSession(@PathVariable String sessionId) {
        return kycSessionRepository.findBySessionId(sessionId).map(session -> {
            session.setStatus(KycStatus.REJECTED);
            session.setUpdatedAt(LocalDateTime.now());
            kycSessionRepository.save(session);
            return ResponseEntity.ok(session);
        }).orElse(ResponseEntity.notFound().build());
    }
}
