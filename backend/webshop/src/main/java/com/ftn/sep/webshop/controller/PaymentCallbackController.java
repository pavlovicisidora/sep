package com.ftn.sep.webshop.controller;

import com.ftn.sep.webshop.model.OrderStatus;
import com.ftn.sep.webshop.model.RentalOrder;
import com.ftn.sep.webshop.security.HmacUtil;
import com.ftn.sep.webshop.service.RentalOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/payment/callback")
@RequiredArgsConstructor
@Slf4j
public class PaymentCallbackController {

    private final RentalOrderService rentalOrderService;
    private final HmacUtil hmacUtil;

    @Value("${webshop.frontend.url}")
    private String frontendUrl;

    @PostMapping("/success")
    public ResponseEntity<Map<String, String>> handleSuccessCallback(
            @RequestHeader(value = "X-PSP-Signature", required = false) String signature,
            @RequestBody Map<String, Object> callbackData) {

        String merchantOrderId = (String) callbackData.get("merchantOrderId");
        String globalTransactionId = (String) callbackData.get("globalTransactionId");
        String status = (String) callbackData.get("status");

        log.info("Received SUCCESS callback - Order: {}, GTX: {}",
                merchantOrderId, globalTransactionId);

        // HMAC signature verification
        if (!verifySignature(signature, callbackData)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid signature"));
        }

        RentalOrder order = rentalOrderService.findByMerchantOrderId(merchantOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Amount and currency verification
        if (!verifyAmountAndCurrency(order, callbackData)) {
            log.error("Amount/currency mismatch for order: {}", merchantOrderId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Amount or currency mismatch"));
        }

        order.setGlobalTransactionId(globalTransactionId);
        rentalOrderService.updateOrderStatus(order.getId(), OrderStatus.PAID);

        log.info("Order {} marked as PAID", merchantOrderId);

        String redirectUrl = frontendUrl + "/payment/success?paymentId=" + merchantOrderId
                + "&gtx=" + globalTransactionId;

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "redirectUrl", redirectUrl
        ));
    }

    @PostMapping("/failed")
    public ResponseEntity<Map<String, String>> handleFailedCallback(
            @RequestHeader(value = "X-PSP-Signature", required = false) String signature,
            @RequestBody Map<String, Object> callbackData) {

        String merchantOrderId = (String) callbackData.get("merchantOrderId");

        log.info("Received FAILED callback - Order: {}", merchantOrderId);

        if (!verifySignature(signature, callbackData)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid signature"));
        }

        RentalOrder order = rentalOrderService.findByMerchantOrderId(merchantOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!verifyAmountAndCurrency(order, callbackData)) {
            log.error("Amount/currency mismatch for order: {}", merchantOrderId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Amount or currency mismatch"));
        }

        rentalOrderService.updateOrderStatus(order.getId(), OrderStatus.FAILED);

        log.info("Order {} marked as FAILED", merchantOrderId);

        String redirectUrl = frontendUrl + "/payment/failed?paymentId=" + merchantOrderId;

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "redirectUrl", redirectUrl
        ));
    }

    @PostMapping("/error")
    public ResponseEntity<Map<String, String>> handleErrorCallback(
            @RequestHeader(value = "X-PSP-Signature", required = false) String signature,
            @RequestBody Map<String, Object> callbackData) {

        String merchantOrderId = (String) callbackData.get("merchantOrderId");

        log.error("Received ERROR callback - Order: {}", merchantOrderId);

        if (!verifySignature(signature, callbackData)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid signature"));
        }

        RentalOrder order = rentalOrderService.findByMerchantOrderId(merchantOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!verifyAmountAndCurrency(order, callbackData)) {
            log.error("Amount/currency mismatch for order: {}", merchantOrderId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Amount or currency mismatch"));
        }

        rentalOrderService.updateOrderStatus(order.getId(), OrderStatus.FAILED);

        log.info("Order {} marked as FAILED due to error", merchantOrderId);

        String redirectUrl = frontendUrl + "/payment/failed?paymentId=" + merchantOrderId;

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "redirectUrl", redirectUrl
        ));
    }

    private boolean verifySignature(String signature, Map<String, Object> callbackData) {
        if (signature == null || signature.isEmpty()) {
            log.error("Missing HMAC signature in PSP callback");
            return false;
        }

        String merchantOrderId = (String) callbackData.get("merchantOrderId");
        String status = (String) callbackData.get("status");
        String amountStr = callbackData.get("amount").toString();
        String currency = (String) callbackData.get("currency");

        String payload = String.format("%s|%s|%s|%s",
                merchantOrderId, status, amountStr, currency);

        boolean valid = hmacUtil.validateSignature(payload, signature);
        if (valid) {
            log.info("HMAC signature verified for callback - Order: {}", merchantOrderId);
        } else {
            log.error("Invalid HMAC signature for callback - Order: {}", merchantOrderId);
        }
        return valid;
    }

    private boolean verifyAmountAndCurrency(RentalOrder order, Map<String, Object> callbackData) {
        Object callbackAmount = callbackData.get("amount");
        String callbackCurrency = (String) callbackData.get("currency");

        if (callbackAmount == null || callbackCurrency == null) {
            log.error("Missing amount or currency in callback data");
            return false;
        }

        BigDecimal amount = new BigDecimal(callbackAmount.toString());

        boolean amountMatch = order.getTotalPrice().compareTo(amount) == 0;
        boolean currencyMatch = order.getCurrency().equals(callbackCurrency);

        if (!amountMatch) {
            log.error("Amount mismatch - Order: {}, Expected: {}, Received: {}",
                    order.getMerchantOrderId(), order.getTotalPrice(), amount);
        }
        if (!currencyMatch) {
            log.error("Currency mismatch - Order: {}, Expected: {}, Received: {}",
                    order.getMerchantOrderId(), order.getCurrency(), callbackCurrency);
        }

        return amountMatch && currencyMatch;
    }
}
