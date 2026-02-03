package com.ftn.sep.psp.service.provider;

import com.ftn.sep.psp.dto.BankPaymentRequest;
import com.ftn.sep.psp.dto.BankPaymentResponse;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class CardPaymentProvider implements PaymentProvider {

    private final RestTemplate restTemplate;
    private final HmacUtil hmacUtil;

    @Value("${bank.api.url}")
    private String bankApiUrl;

    @Value("${psp.merchant.bank.id}")
    private String pspBankMerchantId;

    @Override
    public String getMethodCode() {
        return "CARD";
    }

    @Override
    public String getMethodName() {
        return "Card Payment";
    }

    @Override
    public String getDescription() {
        return "Pay with Visa or Mastercard";
    }

    @Override
    public boolean isAvailable() {
        try {
            restTemplate.getForEntity(bankApiUrl + "/api/health", String.class);
            return true;
        } catch (Exception e) {
            log.warn("Bank service is not available", e);
            return false;
        }
    }

    @Override
    public PaymentProviderResponse createPaymentSession(
            String merchantId,
            BigDecimal amount,
            String currency,
            String stan,
            LocalDateTime timestamp) {

        String url = bankApiUrl + "/api/payment/create";

        log.info("Creating card payment session - STAN: {}", stan);

        BankPaymentRequest request = new BankPaymentRequest();
        request.setMerchantId(pspBankMerchantId);
        request.setAmount(amount);
        request.setCurrency(currency);
        request.setStan(stan);
        request.setPspTimestamp(timestamp);

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

        HttpEntity<BankPaymentRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<BankPaymentResponse> response = restTemplate.postForEntity(
                    url,
                    entity,
                    BankPaymentResponse.class
            );

            BankPaymentResponse responseBody = response.getBody();

            if (responseBody != null) {
                log.info("Card payment session created - Payment ID: {}", responseBody.getPaymentId());

                return new PaymentProviderResponse(
                        responseBody.getPaymentId(),
                        responseBody.getPaymentUrl(),
                        responseBody.getStatus(),
                        responseBody.getMessage()
                );
            } else {
                throw new RuntimeException("Empty response from Bank");
            }

        } catch (Exception e) {
            log.error("Error creating card payment session", e);
            throw new RuntimeException("Failed to create payment session with Bank", e);
        }
    }
}
