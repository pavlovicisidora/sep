package com.ftn.sep.webshop.service;

import com.ftn.sep.webshop.model.RentalOrder;
import com.ftn.sep.webshop.model.OrderStatus;
import com.ftn.sep.webshop.model.User;
import com.ftn.sep.webshop.model.Vehicle;
import com.ftn.sep.webshop.repository.RentalOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RentalOrderService {

    private final RentalOrderRepository rentalOrderRepository;

    @Transactional
    public RentalOrder createOrder(User user, Vehicle vehicle, LocalDate startDate, LocalDate endDate) {
        if (!vehicle.getAvailable()) {
            throw new RuntimeException("Vehicle is not available");
        }

        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days <= 0) {
            throw new RuntimeException("Invalid rental period");
        }

        BigDecimal totalPrice = vehicle.getPricePerDay().multiply(BigDecimal.valueOf(days));

        RentalOrder order = new RentalOrder();
        order.setUser(user);
        order.setVehicle(vehicle);
        order.setRentalStartDate(startDate);
        order.setRentalEndDate(endDate);
        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatus.PENDING);
        order.setMerchantOrderId(generateMerchantOrderId());

        return rentalOrderRepository.save(order);
    }

    public List<RentalOrder> getUserOrders(User user) {
        return rentalOrderRepository.findByUser(user);
    }

    public Optional<RentalOrder> findById(Long id) {
        return rentalOrderRepository.findById(id);
    }

    public Optional<RentalOrder> findByMerchantOrderId(String merchantOrderId) {
        return rentalOrderRepository.findByMerchantOrderId(merchantOrderId);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        RentalOrder order = rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        rentalOrderRepository.save(order);
    }

    @Transactional
    public boolean trySetProcessing(Long orderId) {
        int updated = rentalOrderRepository.updateStatusIfExpected(
                orderId, OrderStatus.PENDING, OrderStatus.PROCESSING);
        return updated > 0;
    }

    @Transactional
    public RentalOrder save(RentalOrder order) {
        return rentalOrderRepository.save(order);
    }

    private String generateMerchantOrderId() {
        return "WS-" + UUID.randomUUID().toString();
    }
}
