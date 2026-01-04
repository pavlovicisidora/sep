package com.ftn.sep.webshop.service;

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
public class PSPService {

    private final RestTemplate restTemplate;

    @Value("${psp.api.url}")
    private String pspApiUrl;

    @Value("${psp.merchant.id}")
    private String merchantId;

    @Value("${psp.merchant.password}")
    private String merchantPassword;

    @Value("${webshop.base.url}")
    private String webshopBaseUrl;

    public Map<String, Object> initializePayment(String merchantOrderId,
                                                 BigDecimal amount,
                                                 String currency) {
        String url = pspApiUrl + "/api/payment/initialize";

        log.info("Initializing payment with PSP - Order ID: {}, Amount: {} {}",
                merchantOrderId, amount, currency);

        Map<String, Object> request = new HashMap<>();
        request.put("merchantId", merchantId);
        request.put("merchantPassword", merchantPassword);
        request.put("amount", amount);
        request.put("currency", currency);
        request.put("merchantOrderId", merchantOrderId);
        request.put("merchantTimestamp", LocalDateTime.now());
        request.put("successUrl", webshopBaseUrl + "/api/payment/callback/success");
        request.put("failedUrl", webshopBaseUrl + "/api/payment/callback/failed");
        request.put("errorUrl", webshopBaseUrl + "/api/payment/callback/error");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            Map<String, Object> pspResponse = response.getBody();

            if (pspResponse != null && "SUCCESS".equals(pspResponse.get("status"))) {
                log.info("Payment initialized successfully - Payment URL: {}",
                        pspResponse.get("paymentUrl"));
            } else {
                log.error("PSP returned error: {}",
                        pspResponse != null ? pspResponse.get("message") : "null");
            }

            return pspResponse;

        } catch (Exception e) {
            log.error("Error calling PSP API", e);
            throw new RuntimeException("Failed to initialize payment: " + e.getMessage());
        }
    }
}
