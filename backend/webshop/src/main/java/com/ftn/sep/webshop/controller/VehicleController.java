package com.ftn.sep.webshop.controller;

import com.ftn.sep.webshop.dto.VehicleResponse;
import com.ftn.sep.webshop.model.Vehicle;
import com.ftn.sep.webshop.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        List<VehicleResponse> vehicles = vehicleService.getAllVehicles().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/available")
    public ResponseEntity<List<VehicleResponse>> getAvailableVehicles() {
        List<VehicleResponse> vehicles = vehicleService.getAvailableVehicles().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicle(@PathVariable Long id) {
        return vehicleService.findById(id)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private VehicleResponse mapToResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getCategory(),
                vehicle.getPricePerDay(),
                vehicle.getAvailable(),
                vehicle.getImageUrl(),
                vehicle.getDescription()
        );
    }
}
