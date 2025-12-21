package com.ftn.sep.webshop.controller;

import com.ftn.sep.webshop.dto.CreateOrderRequest;
import com.ftn.sep.webshop.dto.OrderResponse;
import com.ftn.sep.webshop.model.RentalOrder;
import com.ftn.sep.webshop.model.User;
import com.ftn.sep.webshop.model.Vehicle;
import com.ftn.sep.webshop.service.RentalOrderService;
import com.ftn.sep.webshop.service.UserService;
import com.ftn.sep.webshop.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class RentalOrderController {

    private final RentalOrderService rentalOrderService;
    private final UserService userService;
    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        User user = userService.findById(request.getUserId())
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

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getUserOrders(@PathVariable Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<OrderResponse> orders = rentalOrderService.getUserOrders(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return rentalOrderService.findById(id)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
