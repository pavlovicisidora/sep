package com.ftn.sep.psp.service;

import com.ftn.sep.psp.service.provider.PaymentProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PaymentProviderService {

    private final List<PaymentProvider> providers;

    public PaymentProviderService(List<PaymentProvider> providers) {
        this.providers = providers;
        log.info("Registered {} payment providers: {}",
                providers.size(),
                providers.stream().map(PaymentProvider::getMethodCode).toList());
    }

    public List<PaymentProvider> getAvailableProviders() {
        return providers.stream()
                .filter(PaymentProvider::isAvailable)
                .toList();
    }

    public Optional<PaymentProvider> getProviderByCode(String methodCode) {
        return providers.stream()
                .filter(p -> p.getMethodCode().equals(methodCode))
                .findFirst();
    }
}
