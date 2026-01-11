package com.ftn.sep.bank.service;

import com.ftn.sep.bank.model.CardInfo;
import com.ftn.sep.bank.repository.CardInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardInfoRepository cardInfoRepository;

    public Optional<CardInfo> findByPan(String pan) {
        return cardInfoRepository.findByPan(pan);
    }

    public boolean validateCardData(String pan, String cardHolderName,
                                    String expiryDate, String securityCode) {
        Optional<CardInfo> cardOpt = findByPan(pan);

        if (cardOpt.isEmpty()) {
            log.warn("Card not found for PAN ending in: {}", pan.substring(pan.length() - 4));
            return false;
        }

        CardInfo card = cardOpt.get();

        if (!card.getActive()) {
            log.warn("Card is not active");
            return false;
        }

        if (!card.getCardHolderName().equalsIgnoreCase(cardHolderName.trim())) {
            log.warn("Card holder name mismatch");
            return false;
        }

        if (!card.getSecurityCode().equals(securityCode)) {
            log.warn("Security code mismatch");
            return false;
        }

        String[] parts = expiryDate.split("/");
        int month = Integer.parseInt(parts[0]);
        int year = 2000 + Integer.parseInt(parts[1]);
        LocalDate cardExpiryDate = LocalDate.of(year, month, 1);

        if (!card.getExpiryDate().equals(cardExpiryDate)) {
            log.warn("Expiry date mismatch");
            return false;
        }

        log.info("Card validation successful for PAN ending in: {}",
                pan.substring(pan.length() - 4));
        return true;
    }
}
