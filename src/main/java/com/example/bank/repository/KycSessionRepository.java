package com.example.bank.repository;

import com.example.bank.model.KycSession;
import com.example.bank.model.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycSessionRepository extends JpaRepository<KycSession, Long> {
    Optional<KycSession> findBySessionId(String sessionId);
    List<KycSession> findByStatus(KycStatus status);
    List<KycSession> findByCustomerId(String customerId);
}
