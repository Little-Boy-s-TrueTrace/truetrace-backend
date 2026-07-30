package com.example.bank.controller;

import com.example.bank.model.AmlAlert;
import com.example.bank.model.KycSession;
import com.example.bank.model.StrReport;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.AmlAlertRepository;
import com.example.bank.repository.KycSessionRepository;
import com.example.bank.repository.StrReportRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComplianceControllerTest {

    @Test
    void agentStatusUsesPersistedCountsAndActivityTimestamps() {
        var kycRepository = mock(KycSessionRepository.class);
        var alertRepository = mock(AmlAlertRepository.class);
        var reportRepository = mock(StrReportRepository.class);
        var accountRepository = mock(AccountRepository.class);

        var kycSession = new KycSession();
        kycSession.setCreatedAt(LocalDateTime.parse("2026-07-30T01:00:00"));
        kycSession.setUpdatedAt(LocalDateTime.parse("2026-07-30T01:01:00"));
        var alert = new AmlAlert();
        alert.setCreatedAt(LocalDateTime.parse("2026-07-30T01:02:00"));
        var report = new StrReport();
        report.setGeneratedAt(LocalDateTime.parse("2026-07-30T01:03:00"));

        when(kycRepository.count()).thenReturn(12L);
        when(kycRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(kycSession));
        when(alertRepository.count()).thenReturn(4L);
        when(alertRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(alert));
        when(reportRepository.count()).thenReturn(3L);
        when(reportRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(report));

        var controller = new ComplianceController(
                kycRepository,
                alertRepository,
                reportRepository,
                accountRepository);

        var statuses = controller.agentStatus();

        assertThat(statuses)
                .extracting(item -> item.get("processedCount"))
                .containsExactly(12L, 4L, 3L);
        assertThat(statuses)
                .extracting(item -> item.get("lastActivity"))
                .containsExactly(
                        "2026-07-30T01:01",
                        "2026-07-30T01:02",
                        "2026-07-30T01:03");
        assertThat(statuses)
                .allSatisfy(item -> {
                    assertThat(item.get("status")).isEqualTo("UNKNOWN");
                    assertThat(item).doesNotContainKeys("errorCount", "queueDepth");
                });
    }
}
