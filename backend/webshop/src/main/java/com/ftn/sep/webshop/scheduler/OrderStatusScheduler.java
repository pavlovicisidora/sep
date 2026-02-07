package com.ftn.sep.webshop.scheduler;

import com.ftn.sep.webshop.model.OrderStatus;
import com.ftn.sep.webshop.model.RentalOrder;
import com.ftn.sep.webshop.repository.RentalOrderRepository;
import com.ftn.sep.webshop.service.PSPService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusScheduler {

    private final RentalOrderRepository rentalOrderRepository;
    private final PSPService pspService;

    @Scheduled(fixedRate = 120000)
    public void checkPendingOrderStatuses() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(2);

        List<RentalOrder> pendingOrders = rentalOrderRepository
                .findByStatusAndLastPaymentAttemptIsNotNullAndLastPaymentAttemptBefore(
                        OrderStatus.PENDING, cutoff);

        if (pendingOrders.isEmpty()) {
            return;
        }

        log.info("Checking status of {} pending orders with PSP", pendingOrders.size());

        for (RentalOrder order : pendingOrders) {
            try {
                Map<String, Object> statusResponse = pspService.checkPaymentStatus(order.getMerchantOrderId());

                if (statusResponse == null) {
                    log.warn("Null response from PSP for order: {}", order.getMerchantOrderId());
                    continue;
                }

                String status = (String) statusResponse.get("status");

                if ("SUCCESS".equals(status) || "RESERVED".equals(status) || "COMPLETED".equals(status)) {
                    order.setStatus(OrderStatus.PAID);
                    rentalOrderRepository.save(order);
                    log.info("Order {} updated to PAID based on PSP status", order.getMerchantOrderId());
                } else if ("FAILED".equals(status) || "ERROR".equals(status) || "EXPIRED".equals(status)) {
                    order.setStatus(OrderStatus.FAILED);
                    rentalOrderRepository.save(order);
                    log.info("Order {} updated to FAILED based on PSP status: {}", order.getMerchantOrderId(), status);
                } else {
                    log.debug("Order {} still pending at PSP, will retry next cycle", order.getMerchantOrderId());
                }
            } catch (Exception e) {
                log.warn("Could not check status for order {} - PSP may be unavailable, will retry next cycle",
                        order.getMerchantOrderId());
            }
        }
    }
}
