package com.ftn.sep.webshop.controller;

import com.ftn.sep.webshop.dto.CreateOrderRequest;
import com.ftn.sep.webshop.dto.OrderResponse;
import com.ftn.sep.webshop.model.OrderStatus;
import com.ftn.sep.webshop.model.RentalOrder;
import com.ftn.sep.webshop.model.User;
import com.ftn.sep.webshop.model.Vehicle;
import com.ftn.sep.webshop.security.SecurityUtils;
import com.ftn.sep.webshop.service.PSPService;
import com.ftn.sep.webshop.service.RentalOrderService;
import com.ftn.sep.webshop.service.UserService;
import com.ftn.sep.webshop.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class RentalOrderController {

    private final RentalOrderService rentalOrderService;
    private final UserService userService;
    private final VehicleService vehicleService;
    private final PSPService pspService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        User user = userService.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Vehicle vehicle = vehicleService.findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        RentalOrder order = rentalOrderService.createOrder(
                user,
                vehicle,
                request.getRentalStartDate(),
                request.getRentalEndDate()
        );

        OrderResponse response = mapToResponse(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> getMyOrders() {
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        User user = userService.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<OrderResponse> orders = rentalOrderService.getUserOrders(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        User currentUser = userService.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RentalOrder order = rentalOrderService.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(mapToResponse(order));
    }

    @PostMapping("/{orderId}/pay")
    public ResponseEntity<?> initiatePayment(@PathVariable Long orderId) {
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        User currentUser = userService.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RentalOrder order = rentalOrderService.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only pay for your own orders"));
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Order is not in PENDING status"));
        }

        try {
            Map<String, Object> pspResponse = pspService.initializePayment(
                    order.getMerchantOrderId(),
                    order.getTotalPrice(),
                    order.getCurrency()
            );

            order.setLastPaymentAttempt(LocalDateTime.now());
            rentalOrderService.save(order);

            log.info("Payment URL generated for order {} with merchant order ID: {}",
                    orderId, order.getMerchantOrderId());

            return ResponseEntity.ok(pspResponse);

        } catch (Exception e) {
            log.error("Error initiating payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to initiate payment"));
        }
    }

    @GetMapping("/{orderId}/status")
    public ResponseEntity<?> checkOrderStatus(@PathVariable Long orderId) {
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        User currentUser = userService.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RentalOrder order = rentalOrderService.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> statusInfo = new HashMap<>();
        statusInfo.put("orderId", order.getId());
        statusInfo.put("merchantOrderId", order.getMerchantOrderId());
        statusInfo.put("status", order.getStatus());
        statusInfo.put("amount", order.getTotalPrice());
        statusInfo.put("currency", order.getCurrency());
        statusInfo.put("globalTransactionId", order.getGlobalTransactionId());

        return ResponseEntity.ok(statusInfo);
    }

    @PostMapping("/{orderId}/check-payment-status")
    public ResponseEntity<?> checkPaymentStatusWithPSP(@PathVariable Long orderId) {
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        User currentUser = userService.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RentalOrder order = rentalOrderService.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            Map<String, Object> pspStatus = pspService.checkPaymentStatus(order.getMerchantOrderId());

            String status = (String) pspStatus.get("status");

            if ("SUCCESS".equals(status)) {
                String globalTransactionId = (String) pspStatus.get("globalTransactionId");
                order.setGlobalTransactionId(globalTransactionId);
                rentalOrderService.updateOrderStatus(orderId, OrderStatus.PAID);
                log.info("Order {} status updated to PAID after manual check", orderId);
            } else if ("FAILED".equals(status)) {
                rentalOrderService.updateOrderStatus(orderId, OrderStatus.FAILED);
                log.info("Order {} status updated to FAILED after manual check", orderId);
            }

            return ResponseEntity.ok(pspStatus);

        } catch (Exception e) {
            log.error("Error checking payment status with PSP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check payment status"));
        }
    }

    private OrderResponse mapToResponse(RentalOrder order) {
        String vehicleName = order.getVehicle().getBrand() + " " + order.getVehicle().getModel();
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getVehicle().getId(),
                vehicleName,
                order.getRentalStartDate(),
                order.getRentalEndDate(),
                order.getTotalPrice(),
                order.getCurrency(),
                order.getStatus(),
                order.getMerchantOrderId(),
                order.getCreatedAt()
        );
    }
}
