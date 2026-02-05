package com.ftn.sep.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QrPaymentRequest {
    private String merchantId;
    private BigDecimal amount;
    private String currency;
    private String stan;
    private LocalDateTime pspTimestamp;
}
