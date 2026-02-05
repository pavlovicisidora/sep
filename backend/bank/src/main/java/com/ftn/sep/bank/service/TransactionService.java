package com.ftn.sep.bank.service;

import com.ftn.sep.bank.model.BankTransaction;
import com.ftn.sep.bank.model.TransactionStatus;
import com.ftn.sep.bank.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final BankTransactionRepository transactionRepository;

    @Transactional
    public BankTransaction createTransaction(BankTransaction transaction) {
        transaction.setPaymentId(generatePaymentId());
        transaction.setGlobalTransactionId(generateGlobalTransactionId());
        transaction.setPaymentUrl(generatePaymentUrl(transaction.getPaymentId()));

        BankTransaction saved = transactionRepository.save(transaction);
        log.info("Created transaction with Payment ID: {}", saved.getPaymentId());
        return saved;
    }

    public Optional<BankTransaction> findByPaymentId(String paymentId) {
        return transactionRepository.findByPaymentId(paymentId);
    }

    public Optional<BankTransaction> findByStan(String stan) {
        return transactionRepository.findByStan(stan);
    }

    public Optional<BankTransaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    @Transactional
    public void updateTransactionStatus(Long transactionId, TransactionStatus status,
                                        String failureReason) {
        BankTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setStatus(status);
        if (failureReason != null) {
            transaction.setFailureReason(failureReason);
        }

        transactionRepository.save(transaction);
        log.info("Updated transaction {} to status: {}", transactionId, status);
    }

    private String generatePaymentId() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    private String generateGlobalTransactionId() {
        return "GTX-" + UUID.randomUUID().toString();
    }

    private String generatePaymentUrl(String paymentId) {
        return "https://localhost:4201/payment/" + paymentId;
    }
}