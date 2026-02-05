package com.ftn.sep.bank.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;

@Component
@Slf4j
public class IpsQrGenerator {

    private static final String IPS_CONSTANT = "PR";
    private static final String IPS_VERSION = "01";
    private static final String CHARSET = "1";
    private static final String PAYMENT_CODE = "289"; // bezgotovinski - gradjani

    public String generateQrCode(
            String recipientAccount,
            String recipientName,
            BigDecimal amount,
            String currency,
            String paymentReference,
            String paymentPurpose) {

        String ipsPayload = buildIpsPayload(
                recipientAccount,
                recipientName,
                amount,
                currency,
                paymentReference,
                paymentPurpose
        );

        log.info("Generated IPS payload: {}", ipsPayload);

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    ipsPayload,
                    BarcodeFormat.QR_CODE,
                    400,
                    400
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            byte[] imageBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);

        } catch (WriterException | IOException e) {
            log.error("Error generating QR code", e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    private String buildIpsPayload(
            String recipientAccount,
            String recipientName,
            BigDecimal amount,
            String currency,
            String paymentReference,
            String paymentPurpose) {

        StringBuilder payload = new StringBuilder();

        // K - identifikacija (obavezan)
        payload.append("K:").append(IPS_CONSTANT).append("|");

        // V - verzija (obavezan)
        payload.append("V:").append(IPS_VERSION).append("|");

        // C - znakovni skup (obavezan)
        payload.append("C:").append(CHARSET).append("|");

        // R - broj racuna primaoca (obavezan) - 18 cifara bez crtica
        payload.append("R:").append(formatAccountNumber(recipientAccount)).append("|");

        // N - naziv primaoca (obavezan) - max 70 ansi karaktera
        payload.append("N:").append(recipientName).append("|");

        // I - valuta i iznos (obavezan) - format: RSDiznos,decimale
        payload.append("I:").append(currency).append(formatAmount(amount)).append("|");

        // SF - sifra placanja (obavezan) - 3 cifre
        payload.append("SF:").append(PAYMENT_CODE).append("|");

        // S - svrha placanja (opcioni)
        if (paymentPurpose != null && !paymentPurpose.isEmpty()) {
            payload.append("S:").append(truncate(paymentPurpose, 35)).append("|");
        }

        // RO - model i poziv na broj odobrenja (opcioni)
        if (paymentReference != null && !paymentReference.isEmpty()) {
            payload.append("RO:00").append(paymentReference);
        } else {
            payload.setLength(payload.length() - 1);
        }

        return payload.toString();
    }

    private String formatAccountNumber(String account) {
        String cleaned = account.replaceAll("[-\\s]", "");

        if (cleaned.startsWith("RS")) {
            cleaned = cleaned.substring(2);
        }

        if (cleaned.length() == 18) {
            return cleaned;
        }

        if (account.contains("-")) {
            String[] parts = account.replaceAll("RS", "").split("-");

            if (parts.length == 3) {
                String prefix = parts[0];
                String middle = parts[1];
                String suffix = parts[2];

                String paddedMiddle = String.format("%013d", Long.parseLong(middle));

                return prefix + paddedMiddle + suffix;
            }
        }

        if (cleaned.length() < 18) {
            return String.format("%0" + 18 + "d", new java.math.BigInteger(cleaned));
        }

        return cleaned.substring(0, 18);
    }

    private String formatAmount(BigDecimal amount) {
        String formatted = String.format("%.2f", amount);

        formatted = formatted.replace('.', ',');

        if (formatted.endsWith("0") && !formatted.endsWith(",00")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }

        return formatted;
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }
}