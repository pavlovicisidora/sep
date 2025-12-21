package com.ftn.sep.psp.controller;

import com.ftn.sep.psp.dto.InitializePaymentRequest;
import com.ftn.sep.psp.dto.InitializePaymentResponse;
import com.ftn.sep.psp.dto.PaymentCallbackRequest;
import com.ftn.sep.psp.model.PaymentSession;
import com.ftn.sep.psp.service.MerchantService;
import com.ftn.sep.psp.service.PaymentSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    // TODO: Dodati BankService za komunikaciju sa bankom

    @PostMapping("/initialize")
    public ResponseEntity<InitializePaymentResponse> initializePayment(
            @Valid @RequestBody InitializePaymentRequest request) {

        log.info("Received payment initialization request from merchant: {}", request.getMerchantId());

        if (!merchantService.validateMerchant(request.getMerchantId(), request.getMerchantPassword())) {
            log.warn("Invalid merchant credentials for: {}", request.getMerchantId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new InitializePaymentResponse(null, null, null, "ERROR", "Invalid merchant credentials"));
        }

        PaymentSession session = new PaymentSession();
        session.setMerchantId(request.getMerchantId());
        session.setMerchantOrderId(request.getMerchantOrderId());
        session.setMerchantTimestamp(request.getMerchantTimestamp());
        session.setAmount(request.getAmount());
        session.setCurrency(request.getCurrency());
        session.setSuccessUrl(request.getSuccessUrl());
        session.setFailedUrl(request.getFailedUrl());
        session.setErrorUrl(request.getErrorUrl());

        PaymentSession savedSession = paymentSessionService.createSession(session);

        log.info("Created payment session with STAN: {}", savedSession.getStan());

        // TODO: Ovde treba pozvati banku da dobijemo PAYMENT_URL i PAYMENT_ID

        InitializePaymentResponse response = new InitializePaymentResponse(
                null, // paymentId - dobiće se od banke
                null, // paymentUrl - dobiće se od banke
                savedSession.getStan(),
                "PENDING",
                "Payment session created, waiting for bank initialization"
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/callback")
    public ResponseEntity<String> handleBankCallback(@RequestBody PaymentCallbackRequest request) {
        log.info("Received callback from bank for STAN: {}", request.getStan());

        // TODO: Implementirati logiku za ažuriranje statusa i redirekciju korisnika

        return ResponseEntity.ok("Callback received");
    }

    @GetMapping("/status/{stan}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String stan) {
        return paymentSessionService.findByStan(stan)
                .map(session -> ResponseEntity.ok(session))
                .orElse(ResponseEntity.notFound().build());
    }
}
