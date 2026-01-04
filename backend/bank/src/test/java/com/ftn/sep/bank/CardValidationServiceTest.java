package com.ftn.sep.bank;

import com.ftn.sep.bank.service.CardValidationService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CardValidationServiceTest {

    private final CardValidationService service = new CardValidationService();

    @Test
    void testLuhnValidation() {
        assertTrue(service.validatePanWithLuhn("4532015112830366")); // Visa
        assertTrue(service.validatePanWithLuhn("5425233430109903")); // Mastercard

        assertFalse(service.validatePanWithLuhn("4532015112830367"));
        assertFalse(service.validatePanWithLuhn("1234567890123456"));
    }

    @Test
    void testExpiryDateValidation() {
        assertTrue(service.validateExpiryDate("12/26"));
        assertTrue(service.validateExpiryDate("12/30"));

        assertFalse(service.validateExpiryDate("12/20"));
        assertFalse(service.validateExpiryDate("12/25"));

        assertFalse(service.validateExpiryDate("invalid"));
    }
}
