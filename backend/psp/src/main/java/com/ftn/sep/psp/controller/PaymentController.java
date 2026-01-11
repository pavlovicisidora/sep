package com.ftn.sep.psp.controller;

import com.ftn.sep.psp.dto.*;
import com.ftn.sep.psp.model.PaymentSession;
import com.ftn.sep.psp.model.PaymentStatus;
import com.ftn.sep.psp.service.BankService;
import com.ftn.sep.psp.service.MerchantService;
import com.ftn.sep.psp.service.PaymentSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentSessionService paymentSessionService;
    private final MerchantService merchantService;
    private final BankService bankService;
    private final RestTemplate restTemplate;

    @Value("${psp.merchant.bank.id}")
    private String pspBankMerchantId;

    @PostMapping("/initialize")
    public ResponseEntity<InitializePaymentResponse> initializePayment(
            @Valid @RequestBody InitializePaymentRequest request) {

        log.info("Received payment initialization request from merchant: {}", request.getMerchantId());

        if (!merchantService.validateMerchant(request.getMerchantId(), request.getMerchantPassword())) {
            log.warn("Invalid merchant credentials for: {}", request.getMerchantId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new InitializePaymentResponse(null, null, null, "ERROR", "Invalid merchant credentials"));
        }

        PaymentSession session = getPaymentSession(request);

        PaymentSession savedSession = paymentSessionService.createSession(session);

        log.info("Created payment session with STAN: {}", savedSession.getStan());

        try {
            BankPaymentRequest bankRequest = new BankPaymentRequest(
                    pspBankMerchantId,
                    savedSession.getAmount(),
                    savedSession.getCurrency(),
                    savedSession.getStan(),
                    savedSession.getPspTimestamp()
            );

            BankPaymentResponse bankResponse = bankService.createPaymentSession(bankRequest);

            if (bankResponse != null && "SUCCESS".equals(bankResponse.getStatus())) {
                paymentSessionService.updateWithBankData(
                        savedSession.getId(),
                        bankResponse.getPaymentId(),
                        bankResponse.getPaymentUrl()
                );

                log.info("Payment session initialized successfully - redirecting user to: {}",
                        bankResponse.getPaymentUrl());

                InitializePaymentResponse response = new InitializePaymentResponse(
                        bankResponse.getPaymentId(),
                        bankResponse.getPaymentUrl(),
                        savedSession.getStan(),
                        "SUCCESS",
                        "Payment session initialized successfully"
                );

                return ResponseEntity.ok(response);
            } else {
                paymentSessionService.updateStatus(savedSession.getId(), PaymentStatus.ERROR);

                log.error("Bank returned error: {}",
                        bankResponse != null ? bankResponse.getMessage() : "null response");

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new InitializePaymentResponse(
                                null, null, savedSession.getStan(), "ERROR",
                                "Bank service error"
                        ));
            }

        } catch (Exception e) {
            log.error("Error communicating with Bank", e);
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
    public ResponseEntity<String> handleBankCallback(@RequestBody PaymentCallbackRequest request) {
        log.info("Received callback from bank for STAN: {}, Status: {}",
                request.getStan(), request.getStatus());

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

        notifyMerchant(callbackUrl, session, request);

        return ResponseEntity.ok("Callback processed successfully");
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

    private void notifyMerchant(String callbackUrl, PaymentSession session,
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

            restTemplate.postForEntity(callbackUrl, entity, String.class);

            log.info("Successfully notified merchant - Order ID: {}", session.getMerchantOrderId());

        } catch (Exception e) {
            log.error("Error notifying merchant at: " + callbackUrl, e);
        }
    }
}
