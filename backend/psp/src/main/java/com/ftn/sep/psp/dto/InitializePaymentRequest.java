package com.ftn.sep.psp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InitializePaymentRequest {

    @NotBlank(message = "Merchant ID is required")
    private String merchantId;

    @NotBlank(message = "Merchant password is required")
    private String merchantPassword;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Merchant order ID is required")
    private String merchantOrderId;

    @NotNull(message = "Merchant timestamp is required")
    private LocalDateTime merchantTimestamp;

    @NotBlank(message = "Success URL is required")
    private String successUrl;

    @NotBlank(message = "Failed URL is required")
    private String failedUrl;

    @NotBlank(message = "Error URL is required")
    private String errorUrl;
}
