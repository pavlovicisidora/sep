package com.ftn.sep.psp.config;

import com.ftn.sep.psp.model.MerchantConfig;
import com.ftn.sep.psp.repository.MerchantConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final MerchantConfigRepository merchantConfigRepository;

    @Override
    public void run(String... args) {
        seedMerchants();
    }

    private void seedMerchants() {
        if (merchantConfigRepository.count() > 0) {
            log.info("Merchants already exist, skipping seed");
            return;
        }

        MerchantConfig webshop = new MerchantConfig();
        webshop.setMerchantId("WEBSHOP-001");
        webshop.setMerchantPassword("webshop-secret-key-123");
        webshop.setMerchantName("Car Rental Agency");
        webshop.setActive(true);
        merchantConfigRepository.save(webshop);

        log.info("Seeded {} merchants", merchantConfigRepository.count());
    }
}
