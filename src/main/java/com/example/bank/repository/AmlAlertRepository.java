package com.example.bank.repository;

import com.example.bank.model.AmlAlert;
import com.example.bank.model.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmlAlertRepository extends JpaRepository<AmlAlert, Long> {
    Optional<AmlAlert> findByAlertId(String alertId);
    List<AmlAlert> findByStatus(AlertStatus status);
    List<AmlAlert> findByPrimaryAccountNumber(String primaryAccountNumber);
}
