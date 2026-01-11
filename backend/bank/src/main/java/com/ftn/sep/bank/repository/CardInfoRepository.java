package com.ftn.sep.bank.repository;

import com.ftn.sep.bank.model.CardInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardInfoRepository extends JpaRepository<CardInfo, Long> {

    Optional<CardInfo> findByPan(String pan);
}
