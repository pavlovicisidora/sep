package com.ftn.sep.webshop.config;

import com.ftn.sep.webshop.model.User;
import com.ftn.sep.webshop.model.Vehicle;
import com.ftn.sep.webshop.repository.UserRepository;
import com.ftn.sep.webshop.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;

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
        user1.setPassword(passwordEncoder.encode("password123"));
        user1.setFirstName("Marko");
        user1.setLastName("Marković");
        user1.setPhoneNumber("+381601234567");
        userRepository.save(user1);

        User user2 = new User();
        user2.setEmail("ana@example.com");
        user2.setPassword(passwordEncoder.encode("password123"));
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

        Vehicle v1 = new Vehicle();
        v1.setBrand("Toyota");
        v1.setModel("Camry");
        v1.setYear(2023);
        v1.setCategory("Sedan");
        v1.setPricePerDay(new BigDecimal("5000"));
        v1.setAvailable(true);
        v1.setDescription("Comfortable sedan perfect for everyday use and long trips");
        v1.setImageUrl("https://images.unsplash.com/photo-1621007947382-bb3c3994e3fb?w=800&auto=format&fit=crop");
        vehicleRepository.save(v1);

        Vehicle v2 = new Vehicle();
        v2.setBrand("Honda");
        v2.setModel("CR-V");
        v2.setYear(2023);
        v2.setCategory("SUV");
        v2.setPricePerDay(new BigDecimal("7000"));
        v2.setAvailable(true);
        v2.setDescription("Spacious SUV perfect for families and outdoor adventures");
        v2.setImageUrl("https://images.unsplash.com/photo-1519641471654-76ce0107ad1b?w=800&auto=format&fit=crop");
        vehicleRepository.save(v2);

        Vehicle v3 = new Vehicle();
        v3.setBrand("Volkswagen");
        v3.setModel("Golf");
        v3.setYear(2022);
        v3.setCategory("Compact");
        v3.setPricePerDay(new BigDecimal("4000"));
        v3.setAvailable(true);
        v3.setDescription("Economical compact car ideal for city driving");
        v3.setImageUrl("https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=800&auto=format&fit=crop");
        vehicleRepository.save(v3);

        Vehicle v4 = new Vehicle();
        v4.setBrand("BMW");
        v4.setModel("5 Series");
        v4.setYear(2024);
        v4.setCategory("Luxury");
        v4.setPricePerDay(new BigDecimal("12000"));
        v4.setAvailable(true);
        v4.setDescription("Premium luxury sedan with advanced features");
        v4.setImageUrl("https://images.unsplash.com/photo-1555215695-3004980ad54e?w=800&auto=format&fit=crop");
        vehicleRepository.save(v4);

        Vehicle v5 = new Vehicle();
        v5.setBrand("Mercedes");
        v5.setModel("GLE");
        v5.setYear(2023);
        v5.setCategory("SUV");
        v5.setPricePerDay(new BigDecimal("15000"));
        v5.setAvailable(false);
        v5.setDescription("Luxury SUV - Currently rented");
        v5.setImageUrl("https://images.unsplash.com/photo-1618843479313-40f8afb4b4d8?w=800&auto=format&fit=crop");
        vehicleRepository.save(v5);

        Vehicle v6 = new Vehicle();
        v6.setBrand("Audi");
        v6.setModel("A4");
        v6.setYear(2023);
        v6.setCategory("Sedan");
        v6.setPricePerDay(new BigDecimal("8000"));
        v6.setAvailable(true);
        v6.setDescription("Elegant sedan combining performance and comfort");
        v6.setImageUrl("https://images.unsplash.com/photo-1606664515524-ed2f786a0bd6?w=800&auto=format&fit=crop");
        vehicleRepository.save(v6);

        Vehicle v7 = new Vehicle();
        v7.setBrand("Tesla");
        v7.setModel("Model 3");
        v7.setYear(2024);
        v7.setCategory("Electric");
        v7.setPricePerDay(new BigDecimal("10000"));
        v7.setAvailable(true);
        v7.setDescription("Eco-friendly electric sedan with cutting-edge technology");
        v7.setImageUrl("https://images.unsplash.com/photo-1560958089-b8a1929cea89?w=800&auto=format&fit=crop");
        vehicleRepository.save(v7);

        Vehicle v8 = new Vehicle();
        v8.setBrand("Ford");
        v8.setModel("Mustang");
        v8.setYear(2023);
        v8.setCategory("Sports");
        v8.setPricePerDay(new BigDecimal("14000"));
        v8.setAvailable(true);
        v8.setDescription("Iconic sports car delivering thrilling performance");
        v8.setImageUrl("https://d2qldpouxvc097.cloudfront.net/image-by-path?bucket=a5-gallery-serverless-prod-chromebucket-1iz9ffi08lwxm&key=433337%2Ffront34%2Flg%2F303337");
        vehicleRepository.save(v8);

        log.info("Seeded {} vehicles", vehicleRepository.count());
    }
}
