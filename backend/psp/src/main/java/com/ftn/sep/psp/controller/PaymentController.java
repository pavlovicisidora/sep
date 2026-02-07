package com.ftn.sep.psp.controller;

import com.ftn.sep.psp.dto.*;
import com.ftn.sep.psp.model.PaymentSession;
import com.ftn.sep.psp.model.PaymentStatus;
import com.ftn.sep.psp.security.HmacUtil;
import com.ftn.sep.psp.service.MerchantService;
import com.ftn.sep.psp.service.PaymentProviderService;
import com.ftn.sep.psp.service.PaymentSessionService;
import com.ftn.sep.psp.service.provider.PaymentProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentSessionService paymentSessionService;
    private final PaymentProviderService paymentProviderService;
    private final MerchantService merchantService;
    private final RestTemplate restTemplate;
    private final HmacUtil hmacUtil;

    @PostMapping("/initialize")
    public ResponseEntity<InitializePaymentResponse> initializePayment(
            @Valid @RequestBody InitializePaymentRequest request) {

        log.info("Received payment initialization request from merchant: {}, method: {}",
                request.getMerchantId(), request.getPaymentMethod());

        if (!merchantService.validateMerchant(request.getMerchantId(), request.getMerchantPassword())) {
            log.warn("Invalid merchant credentials for: {}", request.getMerchantId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new InitializePaymentResponse(null, null, null, "ERROR", "Invalid merchant credentials"));
        }

        PaymentProvider provider = paymentProviderService.getProviderByCode(request.getPaymentMethod())
                .orElseThrow(() -> new RuntimeException("Payment method not supported: " + request.getPaymentMethod()));

        if (!provider.isAvailable()) {
            log.warn("Payment method {} is not available", request.getPaymentMethod());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new InitializePaymentResponse(null, null, null, "ERROR",
                            "Payment method temporarily unavailable"));
        }

        PaymentSession session = getPaymentSession(request);
        PaymentSession savedSession = paymentSessionService.createSession(session);

        log.info("Created payment session with STAN: {}", savedSession.getStan());

        try {
            PaymentProviderResponse providerResponse = provider.createPaymentSession(
                    request.getMerchantId(),
                    savedSession.getAmount(),
                    savedSession.getCurrency(),
                    savedSession.getStan(),
                    savedSession.getPspTimestamp()
            );

            if (providerResponse != null && "SUCCESS".equals(providerResponse.getStatus())) {
                paymentSessionService.updateWithBankData(
                        savedSession.getId(),
                        providerResponse.getPaymentId(),
                        providerResponse.getPaymentUrl()
                );

                log.info("Payment session initialized successfully - redirecting user to: {}",
                        providerResponse.getPaymentUrl());

                InitializePaymentResponse response = new InitializePaymentResponse(
                        providerResponse.getPaymentId(),
                        providerResponse.getPaymentUrl(),
                        savedSession.getStan(),
                        "SUCCESS",
                        "Payment session initialized successfully"
                );

                return ResponseEntity.ok(response);
            } else {
                paymentSessionService.updateStatus(savedSession.getId(), PaymentStatus.ERROR);

                log.error("Provider returned error: {}",
                        providerResponse != null ? providerResponse.getMessage() : "null response");

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new InitializePaymentResponse(
                                null, null, savedSession.getStan(), "ERROR",
                                "Payment provider error"
                        ));
            }

        } catch (Exception e) {
            log.error("Error communicating with payment provider", e);
            paymentSessionService.updateStatus(savedSession.getId(), PaymentStatus.ERROR);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new InitializePaymentResponse(
                            null, null, savedSession.getStan(), "ERROR",
                            "Failed to communicate with payment provider"
                    ));
        }
    }

    private static PaymentSession getPaymentSession(InitializePaymentRequest request) {
        PaymentSession session = new PaymentSession();
        session.setMerchantId(request.getMerchantId());
        session.setMerchantOrderId(request.getMerchantOrderId());
        session.setMerchantTimestamp(request.getMerchantTimestamp());
        session.setAmount(request.getAmount());
        session.setCurrency(request.getCurrency());
        session.setSuccessUrl(request.getSuccessUrl());
        session.setFailedUrl(request.getFailedUrl());
        session.setErrorUrl(request.getErrorUrl());
        return session;
    }

    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> handleBankCallback(
            @RequestHeader(value = "X-Bank-Signature", required = false) String signature,
            @RequestBody PaymentCallbackRequest request) {
        log.info("Received callback from bank for STAN: {}, Status: {}",
                request.getStan(), request.getStatus());

        if (signature == null || signature.isEmpty()) {
            log.warn("Missing HMAC signature in Bank callback for STAN: {}", request.getStan());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing authentication signature"));
        }

        String payload = String.format("%s|%s|%s|%s",
                request.getStan(),
                request.getStatus(),
                request.getGlobalTransactionId(),
                request.getAcquirerTimestamp().toString()
        );

        if (!hmacUtil.validateSignature(payload, signature)) {
            log.warn("Invalid HMAC signature for Bank callback - STAN: {}", request.getStan());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authentication signature"));
        }
        log.info("Bank callback HMAC validated successfully for STAN: {}", request.getStan());

        PaymentSession session = paymentSessionService.findByStan(request.getStan())
                .orElseThrow(() -> new RuntimeException("Payment session not found"));

        session.setGlobalTransactionId(request.getGlobalTransactionId());
        session.setAcquirerTimestamp(request.getAcquirerTimestamp());

        PaymentStatus newStatus;
        String callbackUrl;

        switch (request.getStatus()) {
            case "SUCCESS":
                newStatus = PaymentStatus.SUCCESS;
                callbackUrl = session.getSuccessUrl();
                break;
            case "FAILED":
                newStatus = PaymentStatus.FAILED;
                callbackUrl = session.getFailedUrl();
                break;
            case "ERROR":
            default:
                newStatus = PaymentStatus.ERROR;
                callbackUrl = session.getErrorUrl();
                break;
        }

        paymentSessionService.updateStatus(session.getId(), newStatus);

        log.info("Updated payment session status to: {}", newStatus);

        String redirectUrl = notifyMerchant(callbackUrl, session, request);

        log.info("Returning redirect URL to bank: {}", redirectUrl);

        return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl != null ? redirectUrl : ""));
    }

    @GetMapping("/status/{stan}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String stan) {
        return paymentSessionService.findByStan(stan)
                .map(session -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("stan", session.getStan());
                    status.put("merchantOrderId", session.getMerchantOrderId());
                    status.put("status", session.getStatus());
                    status.put("amount", session.getAmount());
                    status.put("currency", session.getCurrency());
                    status.put("globalTransactionId", session.getGlobalTransactionId());
                    status.put("createdAt", session.getCreatedAt());
                    return ResponseEntity.ok(status);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/order/{merchantOrderId}")
    public ResponseEntity<?> getPaymentStatusByMerchantOrderId(@PathVariable String merchantOrderId) {
        log.info("Checking payment status for merchant order: {}", merchantOrderId);

        return paymentSessionService.findByMerchantOrderId(merchantOrderId)
                .map(session -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("merchantOrderId", session.getMerchantOrderId());
                    status.put("status", session.getStatus());
                    status.put("amount", session.getAmount());
                    status.put("currency", session.getCurrency());
                    status.put("globalTransactionId", session.getGlobalTransactionId());
                    status.put("createdAt", session.getCreatedAt());
                    return ResponseEntity.ok(status);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private String notifyMerchant(String callbackUrl, PaymentSession session,
                                  PaymentCallbackRequest bankCallback) {
        log.info("Notifying merchant at: {}", callbackUrl);

        try {
            Map<String, Object> callbackData = new HashMap<>();
            callbackData.put("merchantOrderId", session.getMerchantOrderId());
            callbackData.put("stan", session.getStan());
            callbackData.put("globalTransactionId", bankCallback.getGlobalTransactionId());
            callbackData.put("status", bankCallback.getStatus());
            callbackData.put("amount", session.getAmount());
            callbackData.put("currency", session.getCurrency());
            callbackData.put("timestamp", bankCallback.getAcquirerTimestamp());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(callbackData, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(callbackUrl, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("redirectUrl")) {
                String redirectUrl = (String) responseBody.get("redirectUrl");
                log.info("Merchant returned redirect URL: {}", redirectUrl);
                return redirectUrl;
            }

            log.info("Successfully notified merchant - Order ID: {}", session.getMerchantOrderId());
            return null;

        } catch (Exception e) {
            log.error("Error notifying merchant at: " + callbackUrl, e);
            return null;
        }
    }

    @GetMapping("/methods")
    public ResponseEntity<?> getAvailablePaymentMethods() {
        log.info("Fetching available payment methods");

        List<Map<String, String>> methods = paymentProviderService.getAvailableProviders()
                .stream()
                .map(provider -> Map.of(
                        "code", provider.getMethodCode(),
                        "name", provider.getMethodName(),
                        "description", provider.getDescription()
                ))
                .toList();

        return ResponseEntity.ok(methods);
    }
}
