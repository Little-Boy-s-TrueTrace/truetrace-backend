package com.example.bank.controller;

import com.example.bank.model.KycSession;
import com.example.bank.model.KycStatus;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.KycSessionRepository;
import com.example.bank.repository.UserRepository;
import com.example.bank.service.EventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/kyc")
public class KycController {

    private final KycSessionRepository kycSessionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final EventPublisher eventPublisher;

    public KycController(
            KycSessionRepository kycSessionRepository,
            UserRepository userRepository,
            AccountRepository accountRepository,
            EventPublisher eventPublisher) {
        this.kycSessionRepository = kycSessionRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(
            @RequestParam("customerName") String customerName,
            @RequestParam("cccdNumber") String cccdNumber,
            @RequestParam("selfie") MultipartFile selfie,
            @RequestParam("idFront") MultipartFile idFront,
            @RequestParam("idBack") MultipartFile idBack) throws java.io.IOException {
        if (selfie.isEmpty() || idFront.isEmpty() || idBack.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "All KYC images are required"));
        }

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        var currentUser = userRepository.findByUsername(currentUsername).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authenticated user not found"));
        }
        var currentAccount = accountRepository.findByUser(currentUser).orElse(null);
        if (currentAccount == null) {
            return ResponseEntity.status(409).body(Map.of("error", "Customer account not found"));
        }

        KycSession session = new KycSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setCustomerId(currentUser.getId().toString());
        session.setAccountId(currentAccount.getAccountNumber());
        session.setCustomerName(currentUser.getFullName());
        session.setCccdNumber(cccdNumber.trim());
        session.setStatus(KycStatus.PENDING);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        
        // Store opaque evidence references only. Raw biometric data is sent on the
        // short-lived event and is not written to the application database.
        session.setSelfieImagePath("evidence://" + session.getSessionId() + "/selfie");
        session.setIdFrontImagePath("evidence://" + session.getSessionId() + "/id-front");
        session.setIdBackImagePath("evidence://" + session.getSessionId() + "/id-back");
        
        kycSessionRepository.save(session);
        
        String evidenceDir = "/data/evidence/" + session.getSessionId();
        java.nio.file.Path dir = java.nio.file.Paths.get(evidenceDir);
        java.nio.file.Files.createDirectories(dir);
        java.nio.file.Files.write(dir.resolve("selfie.jpg"), selfie.getBytes());
        java.nio.file.Files.write(dir.resolve("id-front.jpg"), idFront.getBytes());
        java.nio.file.Files.write(dir.resolve("id-back.jpg"), idBack.getBytes());

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("session_id", session.getSessionId());
        event.put("customer_id", session.getCustomerId());
        event.put("account_id", session.getAccountId());
        event.put("customer_name", session.getCustomerName());
        event.put("cccd_number", session.getCccdNumber());
        event.put("selfie_filename", org.springframework.util.StringUtils.cleanPath(
                selfie.getOriginalFilename() == null ? "selfie" : selfie.getOriginalFilename()));
        event.put("face_image_base64", Base64.getEncoder().encodeToString(selfie.getBytes()));
        event.put("id_front_image_base64", Base64.getEncoder().encodeToString(idFront.getBytes()));
        event.put("id_back_image_base64", Base64.getEncoder().encodeToString(idBack.getBytes()));
        event.put("timestamp", session.getCreatedAt().toString());
        eventPublisher.publishKycEvent(event);
        
        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<KycSession>> listSessions(
            @RequestParam(required = false) KycStatus status,
            @RequestParam(defaultValue = "false") boolean mine) {
        if (mine) {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            var currentUser = userRepository.findByUsername(currentUsername).orElse(null);
            if (currentUser == null) {
                return ResponseEntity.ok(List.of());
            }
            List<KycSession> sessions = kycSessionRepository
                    .findByCustomerIdOrderByCreatedAtDesc(currentUser.getId().toString());
            if (status != null) {
                sessions = sessions.stream().filter(session -> session.getStatus() == status).toList();
            }
            return ResponseEntity.ok(sessions);
        }
        if (status != null) {
            return ResponseEntity.ok(kycSessionRepository.findByStatusOrderByCreatedAtDesc(status));
        }
        return ResponseEntity.ok(kycSessionRepository.findAllByOrderByCreatedAtDesc());
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
            session.setCccdValid(updated.getCccdValid());
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

    @GetMapping("/sessions/{sessionId}/evidence/{imageType}")
    public ResponseEntity<byte[]> getEvidence(@PathVariable String sessionId, @PathVariable String imageType) {
        // Validate imageType is one of: selfie, id-front, id-back
        if (!List.of("selfie", "id-front", "id-back").contains(imageType)) {
            return ResponseEntity.badRequest().build();
        }
        java.nio.file.Path imagePath = java.nio.file.Paths.get("/data/evidence/" + sessionId + "/" + imageType + ".jpg");
        if (!java.nio.file.Files.exists(imagePath)) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(imagePath);
            return ResponseEntity.ok()
                    .header("Content-Type", "image/jpeg")
                    .header("Cache-Control", "private, max-age=3600")
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
