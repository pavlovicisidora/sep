package com.ftn.sep.psp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentProviderResponse {
    private String paymentId;
    private String paymentUrl;
    private String status;
    private String message;
}
