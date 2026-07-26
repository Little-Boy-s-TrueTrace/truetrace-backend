package com.example.bank.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private static final String TOPIC = "truetrace.transactions";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        if (kafkaTemplate == null) {
            log.warn("[Kafka] KafkaTemplate not available. Events will NOT be published to Kafka.");
        } else {
            log.info("[Kafka] EventPublisher initialized. Publishing to topic: {}", TOPIC);
        }
    }

    public void publishKycEvent(Object event) {
        publishEvent("kyc", event);
    }

    public void publishTransactionEvent(Object event) {
        publishEvent("transaction", event);
    }

    public void publishAmlAlert(Object event) {
        publishEvent("aml_alert", event);
    }

    private void publishEvent(String key, Object event) {
        if (kafkaTemplate == null) {
            return;
        }
        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(TOPIC, key, event);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[Kafka] Failed to publish event: {}", ex.getMessage());
                } else {
                    log.info("[Kafka] Published event with key={} to partition={} offset={}",
                            key,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("[Kafka] Error preparing event for publish: {}", e.getMessage());
        }
    }
}
