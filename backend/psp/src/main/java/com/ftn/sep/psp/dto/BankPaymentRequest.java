package com.ftn.sep.psp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankPaymentRequest {
    private String merchantId;
    private BigDecimal amount;
    private String currency;
    private String stan;
    private LocalDateTime pspTimestamp;
}
