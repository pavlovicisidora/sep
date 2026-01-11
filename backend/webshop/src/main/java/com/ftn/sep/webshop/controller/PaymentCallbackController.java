package com.ftn.sep.webshop.controller;

import com.ftn.sep.webshop.model.OrderStatus;
import com.ftn.sep.webshop.model.RentalOrder;
import com.ftn.sep.webshop.service.RentalOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment/callback")
@RequiredArgsConstructor
@Slf4j
public class PaymentCallbackController {

    private final RentalOrderService rentalOrderService;

    @PostMapping("/success")
    public ResponseEntity<String> handleSuccessCallback(@RequestBody Map<String, Object> callbackData) {
        String merchantOrderId = (String) callbackData.get("merchantOrderId");
        String globalTransactionId = (String) callbackData.get("globalTransactionId");

        log.info("Received SUCCESS callback - Order: {}, GTX: {}",
                merchantOrderId, globalTransactionId);

        RentalOrder order = rentalOrderService.findByMerchantOrderId(merchantOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setGlobalTransactionId(globalTransactionId);
        rentalOrderService.updateOrderStatus(order.getId(), OrderStatus.PAID);

        log.info("Order {} marked as PAID", merchantOrderId);

        return ResponseEntity.ok("Payment confirmed");
    }

    @PostMapping("/failed")
    public ResponseEntity<String> handleFailedCallback(@RequestBody Map<String, Object> callbackData) {
        String merchantOrderId = (String) callbackData.get("merchantOrderId");

        log.info("Received FAILED callback - Order: {}", merchantOrderId);

        RentalOrder order = rentalOrderService.findByMerchantOrderId(merchantOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        rentalOrderService.updateOrderStatus(order.getId(), OrderStatus.FAILED);

        log.info("Order {} marked as FAILED", merchantOrderId);

        return ResponseEntity.ok("Payment failure recorded");
    }

    @PostMapping("/error")
    public ResponseEntity<String> handleErrorCallback(@RequestBody Map<String, Object> callbackData) {
        String merchantOrderId = (String) callbackData.get("merchantOrderId");

        log.error("Received ERROR callback - Order: {}", merchantOrderId);

        RentalOrder order = rentalOrderService.findByMerchantOrderId(merchantOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        rentalOrderService.updateOrderStatus(order.getId(), OrderStatus.FAILED);

        log.info("Order {} marked as FAILED due to error", merchantOrderId);

        return ResponseEntity.ok("Payment error recorded");
    }
}
