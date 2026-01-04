package com.ftn.sep.psp.security;

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

    @Value("${bank.hmac.secret}")
    private String hmacSecret;

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public String generateSignature(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    hmacSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating HMAC signature", e);
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }

    public String createPayload(String merchantId, String amount, String currency, String stan, String timestamp) {
        return String.format("%s|%s|%s|%s|%s", merchantId, amount, currency, stan, timestamp);
    }
}
