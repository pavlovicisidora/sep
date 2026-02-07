package com.ftn.sep.psp.service;

import com.ftn.sep.psp.model.MerchantConfig;
import com.ftn.sep.psp.repository.MerchantConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantConfigRepository merchantConfigRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean validateMerchant(String merchantId, String merchantPassword) {
        return merchantConfigRepository.findByMerchantId(merchantId)
                .map(merchant -> merchant.getActive()
                        && passwordEncoder.matches(merchantPassword, merchant.getMerchantPassword()))
                .orElse(false);
    }

    public Optional<MerchantConfig> findByMerchantId(String merchantId) {
        return merchantConfigRepository.findByMerchantId(merchantId);
    }
}
