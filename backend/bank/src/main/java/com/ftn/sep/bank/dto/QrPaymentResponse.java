package com.ftn.sep.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QrPaymentResponse {
    private String paymentId;
    private String paymentUrl;
    private String qrCodeBase64;
    private String status;
    private String message;
}
