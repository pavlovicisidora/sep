package com.ftn.sep.bank.service;

import com.ftn.sep.bank.model.AuditLog;
import com.ftn.sep.bank.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void logPaymentAttempt(String paymentId, String panLastFour,
                                   String result, String details, String ipAddress) {
        AuditLog entry = AuditLog.builder()
                .action("PAYMENT_ATTEMPT")
                .entityType("TRANSACTION")
                .entityId(paymentId)
                .details("PAN ending ****" + panLastFour + " - " + details)
                .result(result)
                .ipAddress(ipAddress)
                .build();
        save(entry);
    }

    @Async
    public void logQrPaymentAttempt(String transactionId, String accountNumber,
                                     String result, String details, String ipAddress) {
        AuditLog entry = AuditLog.builder()
                .action("QR_PAYMENT_ATTEMPT")
                .entityType("TRANSACTION")
                .entityId(transactionId)
                .details("Account: " + accountNumber + " - " + details)
                .result(result)
                .ipAddress(ipAddress)
                .build();
        save(entry);
    }

    @Async
    public void logStatusChange(String transactionId, String oldStatus,
                                 String newStatus, String details) {
        AuditLog entry = AuditLog.builder()
                .action("STATUS_CHANGE")
                .entityType("TRANSACTION")
                .entityId(transactionId)
                .details(oldStatus + " -> " + newStatus
                        + (details != null ? ": " + details : ""))
                .result("SUCCESS")
                .build();
        save(entry);
    }

    private void save(AuditLog entry) {
        try {
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to save audit log entry: {}", entry.getAction(), e);
        }
    }
}
