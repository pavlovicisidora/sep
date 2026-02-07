package com.ftn.sep.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class ProcessPaymentRequest {

    @NotBlank(message = "Payment ID is required")
    private String paymentId;

    @NotBlank(message = "PAN is required")
    @Pattern(regexp = "\\d{13,19}", message = "Invalid PAN format")
    private String pan;

    @NotBlank(message = "Card holder name is required")
    private String cardHolderName;

    @NotBlank(message = "Expiry date is required")
    @Pattern(regexp = "\\d{2}/\\d{2}", message = "Expiry date must be in MM/YY format")
    private String expiryDate;

    @NotBlank(message = "Security code is required")
    @Pattern(regexp = "\\d{3,4}", message = "Security code must be 3 or 4 digits")
    private String securityCode;

    @Override
    public String toString() {
        return "ProcessPaymentRequest{" +
                "paymentId='" + paymentId + '\'' +
                ", pan='****" + (pan != null && pan.length() >= 4
                    ? pan.substring(pan.length() - 4) : "****") + '\'' +
                ", cardHolderName='" + cardHolderName + '\'' +
                ", expiryDate='" + expiryDate + '\'' +
                ", securityCode='***'" +
                '}';
    }
}
