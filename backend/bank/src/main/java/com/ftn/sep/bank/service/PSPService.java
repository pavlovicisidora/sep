package com.ftn.sep.bank.service;

import com.ftn.sep.bank.security.HmacUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PSPService {

    private final RestTemplate restTemplate;
    private final HmacUtil hmacUtil;

    @Value("${psp.api.url}")
    private String pspApiUrl;

    public void notifyPaymentResult(String stan, String globalTransactionId,
                                    LocalDateTime acquirerTimestamp, String status) {
        String url = pspApiUrl + "/api/payment/callback";

        log.info("Notifying PSP about payment result - STAN: {}, Status: {}", stan, status);

        Map<String, Object> request = new HashMap<>();
        request.put("stan", stan);
        request.put("globalTransactionId", globalTransactionId);
        request.put("acquirerTimestamp", acquirerTimestamp);
        request.put("status", status);

        try {
            String payload = hmacUtil.createPayload(
                    stan,
                    status,
                    globalTransactionId,
                    acquirerTimestamp.toString()
            );
            String signature = hmacUtil.generateSignature(payload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Bank-Signature", signature);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(url, entity, String.class);

            log.info("Successfully notified PSP - STAN: {}", stan);

        } catch (Exception e) {
            log.error("Error notifying PSP", e);
        }
    }
}
