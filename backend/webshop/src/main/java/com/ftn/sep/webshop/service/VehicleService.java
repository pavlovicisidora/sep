package com.ftn.sep.webshop.service;

import com.ftn.sep.webshop.model.Vehicle;
import com.ftn.sep.webshop.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Transactional
    public Vehicle createVehicle(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findByAvailableTrue();
    }

    public Optional<Vehicle> findById(Long id) {
        return vehicleRepository.findById(id);
    }

    public List<Vehicle> findByCategory(String category) {
        return vehicleRepository.findByCategory(category);
    }

    @Transactional
    public void updateAvailability(Long vehicleId, boolean available) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicle.setAvailable(available);
        vehicleRepository.save(vehicle);
    }
}
