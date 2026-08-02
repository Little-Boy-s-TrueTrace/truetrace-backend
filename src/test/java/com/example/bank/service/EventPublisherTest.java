package com.example.bank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EventPublisherTest {

    private EventPublisher eventPublisherWithoutKafka;

    @BeforeEach
    void setUp() {
        eventPublisherWithoutKafka = new EventPublisher(null);
    }

    @Test
    void publishKycEvent_withoutKafka_handlesNullGracefully() {
        assertDoesNotThrow(() -> eventPublisherWithoutKafka.publishKycEvent("test_kyc_payload"));
    }

    @Test
    void publishTransactionEvent_withoutKafka_handlesNullGracefully() {
        assertDoesNotThrow(() -> eventPublisherWithoutKafka.publishTransactionEvent("test_tx_payload"));
    }

    @Test
    void publishAmlAlert_withoutKafka_handlesNullGracefully() {
        assertDoesNotThrow(() -> eventPublisherWithoutKafka.publishAmlAlert("test_alert_payload"));
    }

    @Test
    void publishNullPayload_withoutKafka_handlesNullGracefully() {
        assertDoesNotThrow(() -> eventPublisherWithoutKafka.publishKycEvent(null));
        assertDoesNotThrow(() -> eventPublisherWithoutKafka.publishTransactionEvent(null));
        assertDoesNotThrow(() -> eventPublisherWithoutKafka.publishAmlAlert(null));
    }
}
