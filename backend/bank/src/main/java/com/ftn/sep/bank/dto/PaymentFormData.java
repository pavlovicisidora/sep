package com.ftn.sep.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFormData {
    private String paymentId;
    private BigDecimal amount;
    private String currency;
    private boolean expired;
}
