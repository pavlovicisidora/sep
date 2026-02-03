package com.ftn.sep.psp.service.provider;

import com.ftn.sep.psp.dto.PaymentProviderResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentProvider {

    String getMethodCode();

    String getMethodName();

    String getDescription();

    boolean isAvailable();

    PaymentProviderResponse createPaymentSession(
            String merchantId,
            BigDecimal amount,
            String currency,
            String stan,
            LocalDateTime timestamp
    );
}
