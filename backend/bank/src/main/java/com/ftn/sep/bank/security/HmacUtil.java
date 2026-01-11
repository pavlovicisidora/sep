package com.ftn.sep.bank.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
@Slf4j
public class HmacUtil {

    @Value("${psp.hmac.secret}")
    private String hmacSecret;

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public boolean validateSignature(String payload, String receivedSignature) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    hmacSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hmacBytes);

            boolean isValid = calculatedSignature.equals(receivedSignature);

            if (!isValid) {
                log.warn("HMAC validation failed. Expected: {}, Received: {}",
                        calculatedSignature, receivedSignature);
            }

            return isValid;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error validating HMAC signature", e);
            return false;
        }
    }

    public String createPayload(String merchantId, String amount, String currency,
                                String stan, String timestamp) {
        return String.format("%s|%s|%s|%s|%s", merchantId, amount, currency, stan, timestamp);
    }
}
