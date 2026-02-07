package com.ftn.sep.bank.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_info")
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class CardInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private BankAccount account;

    // Primary Account Number - encrypted with AES-256-GCM
    @Column(nullable = false, length = 500)
    private String pan;

    // SHA-256 hash of PAN for lookups
    @Column(unique = true, nullable = false, length = 64)
    private String panHash;

    @Column(nullable = false)
    private String cardHolderName;

    // Expiry date (MM/YY format stored as date)
    @Column(nullable = false)
    private LocalDate expiryDate;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType cardType;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "CardInfo{" +
                "id=" + id +
                ", cardHolderName='" + cardHolderName + '\'' +
                ", pan='[ENCRYPTED]'" +
                ", cardType=" + cardType +
                ", active=" + active +
                '}';
    }
}
