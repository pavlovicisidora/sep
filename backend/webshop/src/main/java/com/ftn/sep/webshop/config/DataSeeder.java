package com.ftn.sep.webshop.config;

import com.ftn.sep.webshop.model.User;
import com.ftn.sep.webshop.model.Vehicle;
import com.ftn.sep.webshop.repository.UserRepository;
import com.ftn.sep.webshop.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    public void run(String... args) {
        seedUsers();
        seedVehicles();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            log.info("Users already exist, skipping seed");
            return;
        }

        User user1 = new User();
        user1.setEmail("test@example.com");
        user1.setPassword("password123"); // TODO: Hash later
        user1.setFirstName("Marko");
        user1.setLastName("Marković");
        user1.setPhoneNumber("+381601234567");
        userRepository.save(user1);

        User user2 = new User();
        user2.setEmail("ana@example.com");
        user2.setPassword("password123");
        user2.setFirstName("Ana");
        user2.setLastName("Anić");
        user2.setPhoneNumber("+381607654321");
        userRepository.save(user2);

        log.info("Seeded {} users", userRepository.count());
    }

    private void seedVehicles() {
        if (vehicleRepository.count() > 0) {
            log.info("Vehicles already exist, skipping seed");
            return;
        }

        // Sedan
        Vehicle v1 = new Vehicle();
        v1.setBrand("Toyota");
        v1.setModel("Camry");
        v1.setYear(2023);
        v1.setCategory("Sedan");
        v1.setPricePerDay(new BigDecimal("5000"));
        v1.setAvailable(true);
        v1.setDescription("Comfortable sedan for everyday use");
        vehicleRepository.save(v1);

        // SUV
        Vehicle v2 = new Vehicle();
        v2.setBrand("Honda");
        v2.setModel("CR-V");
        v2.setYear(2023);
        v2.setCategory("SUV");
        v2.setPricePerDay(new BigDecimal("7000"));
        v2.setAvailable(true);
        v2.setDescription("Spacious SUV perfect for families");
        vehicleRepository.save(v2);

        // Compact
        Vehicle v3 = new Vehicle();
        v3.setBrand("Volkswagen");
        v3.setModel("Golf");
        v3.setYear(2022);
        v3.setCategory("Compact");
        v3.setPricePerDay(new BigDecimal("4000"));
        v3.setAvailable(true);
        v3.setDescription("Economical compact car");
        vehicleRepository.save(v3);

        // Luxury
        Vehicle v4 = new Vehicle();
        v4.setBrand("BMW");
        v4.setModel("5 Series");
        v4.setYear(2024);
        v4.setCategory("Luxury");
        v4.setPricePerDay(new BigDecimal("12000"));
        v4.setAvailable(true);
        v4.setDescription("Premium luxury sedan");
        vehicleRepository.save(v4);

        // SUV - not available
        Vehicle v5 = new Vehicle();
        v5.setBrand("Mercedes");
        v5.setModel("GLE");
        v5.setYear(2023);
        v5.setCategory("SUV");
        v5.setPricePerDay(new BigDecimal("15000"));
        v5.setAvailable(false);
        v5.setDescription("Luxury SUV - Currently rented");
        vehicleRepository.save(v5);

        log.info("Seeded {} vehicles", vehicleRepository.count());
    }
}
