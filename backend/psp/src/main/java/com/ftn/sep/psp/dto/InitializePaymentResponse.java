package com.ftn.sep.psp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitializePaymentResponse {
    private String paymentId;
    private String paymentUrl;
    private String stan;
    private String status;
    private String message;
}
