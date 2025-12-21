package com.ftn.sep.psp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private String merchantOrderId;

    @Column(nullable = false)
    private LocalDateTime merchantTimestamp;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, length = 500)
    private String successUrl;

    @Column(nullable = false, length = 500)
    private String failedUrl;

    @Column(nullable = false, length = 500)
    private String errorUrl;

    @Column(unique = true, nullable = false)
    private String stan; // System Trace Audit Number

    @Column(nullable = false)
    private LocalDateTime pspTimestamp;

    private String paymentId;

    private String paymentUrl;

    private String globalTransactionId;

    private LocalDateTime acquirerTimestamp;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        pspTimestamp = LocalDateTime.now();
        expiresAt = LocalDateTime.now().plusMinutes(15);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
