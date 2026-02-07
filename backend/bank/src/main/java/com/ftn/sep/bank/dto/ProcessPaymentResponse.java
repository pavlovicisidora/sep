package com.ftn.sep.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentResponse {
    private String globalTransactionId;
    private String stan;
    private String status;
    private String message;
    private String redirectUrl;
}
