package com.ftn.sep.psp.repository;

import com.ftn.sep.psp.model.PaymentSession;
import com.ftn.sep.psp.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentSessionRepository extends JpaRepository<PaymentSession, Long> {

    Optional<PaymentSession> findByStan(String stan);

    Optional<PaymentSession> findByPaymentId(String paymentId);

    Optional<PaymentSession> findByMerchantOrderId(String merchantOrderId);

    long countByStatus(PaymentStatus status);
}
