package com.ftn.sep.bank.scheduler;

import com.ftn.sep.bank.model.BankTransaction;
import com.ftn.sep.bank.model.TransactionStatus;
import com.ftn.sep.bank.repository.BankTransactionRepository;
import com.ftn.sep.bank.service.PSPService;
import com.ftn.sep.bank.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionCleanupScheduler {

    private final BankTransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final PSPService pspService;

    @Scheduled(fixedRate = 180000)
    public void cleanupExpiredTransactions() {
        List<BankTransaction> expiredTransactions = transactionRepository
                .findByStatusAndPaymentUrlExpiresAtBefore(TransactionStatus.PENDING, LocalDateTime.now());

        if (expiredTransactions.isEmpty()) {
            return;
        }

        log.info("Found {} expired PENDING transactions to clean up", expiredTransactions.size());

        for (BankTransaction transaction : expiredTransactions) {
            try {
                transactionService.updateTransactionStatus(
                        transaction.getId(),
                        TransactionStatus.EXPIRED,
                        "Payment session expired - user abandoned payment"
                );

                pspService.notifyPaymentResult(
                        transaction.getStan(),
                        transaction.getGlobalTransactionId(),
                        transaction.getAcquirerTimestamp(),
                        "FAILED"
                );

                log.info("Expired and notified PSP for transaction STAN: {}", transaction.getStan());
            } catch (Exception e) {
                log.error("Error cleaning up expired transaction STAN: {}", transaction.getStan(), e);
            }
        }
    }
}
