package com.ftn.sep.webshop.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateOrderRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    @NotNull(message = "Start date is required")
    private LocalDate rentalStartDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate rentalEndDate;
}
