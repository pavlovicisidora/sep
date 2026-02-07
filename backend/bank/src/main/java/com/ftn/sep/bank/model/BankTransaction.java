package com.ftn.sep.bank.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String globalTransactionId;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private String stan;

    @Column(nullable = false)
    private LocalDateTime pspTimestamp;

    @Column(unique = true, nullable = false)
    private String paymentId;

    @Column(nullable = false, length = 500)
    private String paymentUrl;

    @Column(nullable = false)
    private LocalDateTime paymentUrlExpiresAt;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private BankAccount account;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime acquirerTimestamp;

    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Version
    @Column(columnDefinition = "bigint default 0")
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        acquirerTimestamp = LocalDateTime.now();
        paymentUrlExpiresAt = LocalDateTime.now().plusMinutes(10);
    }

    public boolean isPaymentUrlExpired() {
        return LocalDateTime.now().isAfter(paymentUrlExpiresAt);
    }
}
