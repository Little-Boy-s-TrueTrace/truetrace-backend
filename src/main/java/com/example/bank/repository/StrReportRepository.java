package com.example.bank.repository;

import com.example.bank.model.StrReport;
import com.example.bank.model.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StrReportRepository extends JpaRepository<StrReport, Long> {
    Optional<StrReport> findByReportId(String reportId);
    Optional<StrReport> findFirstByOrderByIdDesc();
    List<StrReport> findByStatus(ReportStatus status);
    List<StrReport> findByAlertId(Long alertId);
}
