package com.ftn.sep.bank.repository;

import com.ftn.sep.bank.model.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    Optional<BankTransaction> findByPaymentId(String paymentId);

    Optional<BankTransaction> findByGlobalTransactionId(String globalTransactionId);

    Optional<BankTransaction> findByStan(String stan);
}
