package com.ftn.sep.psp.service.provider;

import com.ftn.sep.psp.dto.PaymentProviderResponse;
import com.ftn.sep.psp.security.HmacUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QRPaymentProvider implements PaymentProvider {

    private final RestTemplate restTemplate;
    private final HmacUtil hmacUtil;

    @Value("${bank.api.url}")
    private String bankApiUrl;

    @Value("${psp.merchant.bank.id}")
    private String pspBankMerchantId;

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
        return true;
    }

    @Override
    public PaymentProviderResponse createPaymentSession(
            String merchantId,
            BigDecimal amount,
            String currency,
            String stan,
            LocalDateTime timestamp) {

        String url = bankApiUrl + "/api/qr/create";

        log.info("Creating QR payment session with Bank - STAN: {}", stan);

        Map<String, Object> request = new HashMap<>();
        request.put("merchantId", pspBankMerchantId);
        request.put("amount", amount);
        request.put("currency", currency);
        request.put("stan", stan);
        request.put("pspTimestamp", timestamp);

        // Generate HMAC signature
        String payload = hmacUtil.createPayload(
                pspBankMerchantId,
                amount.toString(),
                currency,
                stan,
                timestamp.toString()
        );
        String signature = hmacUtil.generateSignature(payload);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-PSP-Signature", signature);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null) {
                log.info("Bank returned QR payment URL - Payment ID: {}", responseBody.get("paymentId"));

                return new PaymentProviderResponse(
                        (String) responseBody.get("paymentId"),
                        (String) responseBody.get("paymentUrl"),
                        (String) responseBody.get("status"),
                        (String) responseBody.get("message")
                );
            } else {
                throw new RuntimeException("Empty response from Bank");
            }

        } catch (Exception e) {
            log.error("Error communicating with Bank QR service", e);
            throw new RuntimeException("Failed to create QR payment session with Bank", e);
        }
    }
}
