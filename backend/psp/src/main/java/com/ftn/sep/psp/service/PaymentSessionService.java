package com.ftn.sep.psp.service;

import com.ftn.sep.psp.model.PaymentSession;
import com.ftn.sep.psp.model.PaymentStatus;
import com.ftn.sep.psp.repository.PaymentSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentSessionService {

    private final PaymentSessionRepository paymentSessionRepository;

    @Transactional
    public PaymentSession createSession(PaymentSession session) {
        session.setStan(generateStan());
        return paymentSessionRepository.save(session);
    }

    public Optional<PaymentSession> findByStan(String stan) {
        return paymentSessionRepository.findByStan(stan);
    }

    public Optional<PaymentSession> findByPaymentId(String paymentId) {
        return paymentSessionRepository.findByPaymentId(paymentId);
    }

    public Optional<PaymentSession> findByMerchantOrderId(String merchantOrderId) {
        return paymentSessionRepository.findByMerchantOrderId(merchantOrderId);
    }

    @Transactional
    public void updateStatus(Long sessionId, PaymentStatus status) {
        PaymentSession session = paymentSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Payment session not found"));
        session.setStatus(status);
        paymentSessionRepository.save(session);
    }

    @Transactional
    public void updateWithBankData(Long sessionId, String paymentId, String paymentUrl) {
        PaymentSession session = paymentSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Payment session not found"));
        session.setPaymentId(paymentId);
        session.setPaymentUrl(paymentUrl);
        session.setStatus(PaymentStatus.INITIALIZED);
        paymentSessionRepository.save(session);
    }

    private String generateStan() {
        return "PSP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
