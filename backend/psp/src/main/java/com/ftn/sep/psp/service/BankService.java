package com.ftn.sep.psp.service;

import com.ftn.sep.psp.dto.BankPaymentRequest;
import com.ftn.sep.psp.dto.BankPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankService {

    private final RestTemplate restTemplate;

    @Value("${bank.api.url}")
    private String bankApiUrl;

    public BankPaymentResponse createPaymentSession(BankPaymentRequest request) {
        String url = bankApiUrl + "/api/payment/create";

        log.info("Calling Bank API to create payment session - STAN: {}", request.getStan());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<BankPaymentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<BankPaymentResponse> response = restTemplate.postForEntity(
                    url,
                    entity,
                    BankPaymentResponse.class
            );

            BankPaymentResponse bankResponse = response.getBody();

            if (bankResponse != null && "SUCCESS".equals(bankResponse.getStatus())) {
                log.info("Successfully received payment URL from Bank - Payment ID: {}",
                        bankResponse.getPaymentId());
            } else {
                log.error("Bank returned non-success status: {}",
                        bankResponse != null ? bankResponse.getStatus() : "null");
            }

            return bankResponse;

        } catch (Exception e) {
            log.error("Error calling Bank API", e);
            throw new RuntimeException("Failed to communicate with Bank: " + e.getMessage());
        }
    }

    public void notifyPaymentResult(String stan, String status) {
        log.info("Received payment notification from Bank - STAN: {}, Status: {}", stan, status);
        // TODO: implementiraj logiku za azuriranje statusa i notifikaciju WebShop-a - bank poziva kad se zavrsi placanje
    }
}
