package com.ftn.sep.webshop.repository;

import com.ftn.sep.webshop.model.RentalOrder;
import com.ftn.sep.webshop.model.OrderStatus;
import com.ftn.sep.webshop.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RentalOrderRepository extends JpaRepository<RentalOrder, Long> {

    List<RentalOrder> findByUser(User user);

    List<RentalOrder> findByStatus(OrderStatus status);

    Optional<RentalOrder> findByMerchantOrderId(String merchantOrderId);

    List<RentalOrder> findByStatusAndLastPaymentAttemptIsNotNullAndLastPaymentAttemptBefore(
            OrderStatus status, LocalDateTime time);
}
