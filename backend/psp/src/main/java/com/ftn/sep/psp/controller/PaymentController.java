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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentSessionService paymentSessionService;
    private final MerchantService merchantService;
    private final BankService bankService;

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
        log.info("Received callback from bank for STAN: {}", request.getStan());

        // TODO: Implementirati logiku za azuriranje statusa i redirekciju korisnika
        // 1. Naći session po STAN-u
        // 2. Ayurirati status
        // 3. Pozvati SUCCESS_URL ili FAILED_URL na WebShop-u

        return ResponseEntity.ok("Callback received");
    }

    @GetMapping("/status/{stan}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String stan) {
        return paymentSessionService.findByStan(stan)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
