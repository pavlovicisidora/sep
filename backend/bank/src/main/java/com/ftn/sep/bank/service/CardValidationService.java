package com.ftn.sep.bank.service;

import com.ftn.sep.bank.model.CardType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class CardValidationService {

    public boolean validatePanWithLuhn(String pan) {
        if (pan == null || !pan.matches("\\d{13,19}")) {
            log.warn("Invalid PAN format (length: {})", pan != null ? pan.length() : "null");
            return false;
        }

        int sum = 0;
        boolean alternate = false;

        for (int i = pan.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(pan.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        boolean isValid = (sum % 10 == 0);
        log.debug("Luhn validation for PAN ending in {}: {}",
                pan.substring(pan.length() - 4), isValid);
        return isValid;
    }

    public boolean validateExpiryDate(String expiryDate) {
        if (expiryDate == null || !expiryDate.matches("\\d{2}/\\d{2}")) {
            log.warn("Invalid expiry date format: {}", expiryDate);
            return false;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
            YearMonth cardExpiry = YearMonth.parse(expiryDate, formatter);
            YearMonth currentMonth = YearMonth.now();

            boolean isValid = !cardExpiry.isBefore(currentMonth);
            log.debug("Expiry date validation for {}: {}", expiryDate, isValid);
            return isValid;
        } catch (Exception e) {
            log.error("Error parsing expiry date: {}", expiryDate, e);
            return false;
        }
    }

    public boolean validateSecurityCode(String securityCode) {
        boolean isValid = securityCode != null && securityCode.matches("\\d{3,4}");
        log.debug("Security code validation: {}", isValid);
        return isValid;
    }

    public CardType detectCardType(String pan) {
        if (pan == null || pan.isEmpty()) {
            return null;
        }

        if (pan.startsWith("4")) {
            return CardType.VISA;
        }

        if (pan.matches("^5[1-5].*") ||
                (pan.length() >= 4 && Integer.parseInt(pan.substring(0, 4)) >= 2221
                        && Integer.parseInt(pan.substring(0, 4)) <= 2720)) {
            return CardType.MASTERCARD;
        }

        if (pan.matches("^3[47].*")) {
            return CardType.AMEX;
        }

        if (pan.matches("^3[68].*")) {
            return CardType.DINNERS;
        }

        log.warn("Unknown card type for PAN starting with: {}", pan.substring(0, 1));
        return null;
    }

    public boolean validateCard(String pan, String expiryDate, String securityCode) {
        return validatePanWithLuhn(pan)
                && validateExpiryDate(expiryDate)
                && validateSecurityCode(securityCode);
    }
}
