package com.ftn.sep.webshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {
    private Long id;
    private String brand;
    private String model;
    private Integer year;
    private String category;
    private BigDecimal pricePerDay;
    private Boolean available;
    private String imageUrl;
    private String description;
}
