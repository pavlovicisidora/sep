package com.ftn.sep.psp.service;

import com.ftn.sep.psp.dto.BankPaymentRequest;
import com.ftn.sep.psp.dto.BankPaymentResponse;
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
public class BankService {

    private final RestTemplate restTemplate;
    private final HmacUtil hmacUtil;

    @Value("${bank.api.url}")
    private String bankApiUrl;

    public BankPaymentResponse createPaymentSession(BankPaymentRequest request) {
        String url = bankApiUrl + "/api/payment/create";

        log.info("Calling Bank API to create payment session - STAN: {}", request.getStan());

        String payload = hmacUtil.createPayload(
                request.getMerchantId(),
                request.getAmount().toString(),
                request.getCurrency(),
                request.getStan(),
                request.getPspTimestamp().toString()
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
                log.info("Successfully received payment URL from Bank - Payment ID: {}",
                        responseBody.getPaymentId());
                return responseBody;
            } else {
                throw new RuntimeException("Empty response from Bank");
            }

        } catch (Exception e) {
            log.error("Error calling Bank API", e);
            throw new RuntimeException("Failed to create payment session with Bank", e);
        }
    }

}
