package com.ftn.sep.psp.service;

import com.ftn.sep.psp.model.MerchantConfig;
import com.ftn.sep.psp.repository.MerchantConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantConfigRepository merchantConfigRepository;

    public boolean validateMerchant(String merchantId, String merchantPassword) {
        return merchantConfigRepository
                .findByMerchantIdAndMerchantPassword(merchantId, merchantPassword)
                .map(MerchantConfig::getActive)
                .orElse(false);
    }

    public Optional<MerchantConfig> findByMerchantId(String merchantId) {
        return merchantConfigRepository.findByMerchantId(merchantId);
    }
}
