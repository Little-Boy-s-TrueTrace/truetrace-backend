package com.example.bank.service;

import com.example.bank.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransactionMonitorService {

    private final EventPublisher eventPublisher;

    @Autowired
    public TransactionMonitorService(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void monitorTransfer(Transaction tx) {
        eventPublisher.publishTransactionEvent(tx);
    }
}
