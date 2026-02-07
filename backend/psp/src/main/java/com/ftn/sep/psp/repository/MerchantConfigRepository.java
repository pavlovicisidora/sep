package com.ftn.sep.psp.repository;

import com.ftn.sep.psp.model.MerchantConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantConfigRepository extends JpaRepository<MerchantConfig, Long> {

    Optional<MerchantConfig> findByMerchantId(String merchantId);
}
