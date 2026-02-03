package com.ftn.sep.psp.service.provider;

import com.ftn.sep.psp.dto.PaymentProviderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class QRPaymentProvider implements PaymentProvider {

    @Override
    public String getMethodCode() {
        return "QR";
    }

    @Override
    public String getMethodName() {
        return "QR Code Payment";
    }

    @Override
    public String getDescription() {
        return "Pay with IPS QR Code";
    }

    @Override
    public boolean isAvailable() {
        // not yet implemented
        return false;
    }

    @Override
    public PaymentProviderResponse createPaymentSession(
            String merchantId,
            BigDecimal amount,
            String currency,
            String stan,
            LocalDateTime timestamp) {

        throw new UnsupportedOperationException("QR payment not yet implemented");
    }
}
