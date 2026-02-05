package com.ftn.sep.webshop.dto;

import com.ftn.sep.webshop.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long userId;
    private Long vehicleId;
    private String vehicleName; // Brand + Model
    private LocalDate rentalStartDate;
    private LocalDate rentalEndDate;
    private BigDecimal totalPrice;
    private String currency;
    private OrderStatus status;
    private String merchantOrderId;
    private LocalDateTime createdAt;
    private String paymentMethod;
    private LocalDateTime lastPaymentAttempt;
}
