package com.ftn.sep.bank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmQrPaymentRequest {
    @NotBlank(message = "Transaction ID is required")
    private Long transactionId;

}