package com.ftn.sep.bank.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@Slf4j
public class IpsQrValidator {

    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^\\d{18}$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^RSD\\d+,?\\d*$");
    private static final Pattern PAYMENT_CODE_PATTERN = Pattern.compile("^\\d{3}$");

    public Map<String, Object> validateQrPayload(String payload) {
        List<String> errors = new ArrayList<>();
        Map<String, String> parsedData = new HashMap<>();

        log.info("Validating IPS QR payload: {}", payload);

        String[] fields = payload.split("\\|");

        for (String field : fields) {
            if (field.isEmpty()) continue;

            String[] keyValue = field.split(":", 2);
            if (keyValue.length == 2) {
                parsedData.put(keyValue[0], keyValue[1]);
            }
        }

        validateMandatoryField(parsedData, "K", "PR", errors);
        validateMandatoryField(parsedData, "V", "01", errors);
        validateMandatoryField(parsedData, "C", "1", errors);

        validateAccount(parsedData.get("R"), errors);

        validateRecipientName(parsedData.get("N"), errors);

        validateAmount(parsedData.get("I"), errors);

        validatePaymentCode(parsedData.get("SF"), errors);

        if (parsedData.containsKey("RO")) {
            validateReferenceNumber(parsedData.get("RO"), errors);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        result.put("parsedData", parsedData);

        return result;
    }

    private void validateMandatoryField(Map<String, String> data, String key,
                                        String expectedValue, List<String> errors) {
        if (!data.containsKey(key)) {
            errors.add("Mandatory field '" + key + "' is missing");
        } else if (!data.get(key).equals(expectedValue)) {
            errors.add("Field '" + key + "' has an invalid value. Expected: " +
                    expectedValue + ", received: " + data.get(key));
        }
    }

    private void validateAccount(String account, List<String> errors) {
        if (account == null || account.isEmpty()) {
            errors.add("Account number (R) is mandatory");
            return;
        }

        if (!ACCOUNT_PATTERN.matcher(account).matches()) {
            errors.add("Account number must be exactly 18 digits without hyphens. Received: " + account);
        }
    }

    private void validateRecipientName(String name, List<String> errors) {
        if (name == null || name.isEmpty()) {
            errors.add("Recipient name (N) is mandatory");
            return;
        }

        if (name.length() > 70) {
            errors.add("Recipient name cannot be longer than 70 characters");
        }

        String[] lines = name.split("\n");
        if (lines.length > 3) {
            errors.add("Recipient name cannot have more than 3 lines");
        }
    }

    private void validateAmount(String amount, List<String> errors) {
        if (amount == null || amount.isEmpty()) {
            errors.add("Amount (I) is mandatory");
            return;
        }

        if (!AMOUNT_PATTERN.matcher(amount).matches()) {
            errors.add("Amount must be in format RSDamount,decimals (e.g. RSD5000,00)");
            return;
        }

        if (amount.length() < 5 || amount.length() > 18) {
            errors.add("Amount must have between 5 and 18 alphanumeric characters");
        }

        String amountStr = amount.replace("RSD", "").replace(",", ".");
        try {
            BigDecimal amountValue = new BigDecimal(amountStr);
            if (amountValue.compareTo(BigDecimal.ZERO) < 0) {
                errors.add("Amount cannot be negative");
            }
            if (amountValue.compareTo(new BigDecimal("999999999999.99")) > 0) {
                errors.add("Amount cannot be greater than 999,999,999,999.99");
            }
        } catch (NumberFormatException e) {
            errors.add("Invalid amount format");
        }
    }

    private void validatePaymentCode(String code, List<String> errors) {
        if (code == null || code.isEmpty()) {
            errors.add("Payment code (SF) is mandatory");
            return;
        }

        if (!PAYMENT_CODE_PATTERN.matcher(code).matches()) {
            errors.add("Payment code must be exactly 3 digits");
            return;
        }

        char firstDigit = code.charAt(0);
        if (firstDigit != '1' && firstDigit != '2') {
            errors.add("Payment code must start with 1 (cash) or 2 (cashless)");
        }
    }

    private void validateReferenceNumber(String reference, List<String> errors) {
        if (reference.length() > 25) {
            errors.add("Reference number (RO) cannot be longer than 25 characters");
        }

        if (reference.length() < 2) {
            errors.add("Reference number (RO) must have at least 2 digits for the model");
            return;
        }

        String model = reference.substring(0, 2);
        if (!model.matches("\\d{2}")) {
            errors.add("Model in reference number must be 2 digits");
        }

        if ("97".equals(model) && reference.length() > 2) {
            String refNumber = reference.substring(2);
            if (!validateMod97(refNumber)) {
                errors.add("Reference number for model 97 is invalid (checksum does not match)");
            }
        }
    }

    private boolean validateMod97(String reference) {
        reference = reference.replaceAll("[^0-9]", "");

        if (reference.length() < 2) return false;

        try {
            long number = Long.parseLong(reference);
            return (number % 97) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
