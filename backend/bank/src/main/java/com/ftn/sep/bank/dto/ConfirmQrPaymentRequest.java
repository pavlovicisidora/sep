package com.ftn.sep.bank.dto;

import lombok.Data;

@Data
public class ConfirmQrPaymentRequest {
    private Long transactionId;
    private String accountNumber;
}
