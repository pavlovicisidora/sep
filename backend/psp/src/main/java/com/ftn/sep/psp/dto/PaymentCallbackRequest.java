package com.ftn.sep.psp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackRequest {
    private String stan;
    private String globalTransactionId;
    private LocalDateTime acquirerTimestamp;
    private String status; // SUCCESS, FAILED, ERROR
}
