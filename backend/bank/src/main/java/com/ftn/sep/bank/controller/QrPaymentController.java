package com.ftn.sep.bank.controller;

import com.ftn.sep.bank.dto.ConfirmQrPaymentRequest;
import com.ftn.sep.bank.dto.QrPaymentRequest;
import com.ftn.sep.bank.dto.QrPaymentResponse;
import com.ftn.sep.bank.model.BankAccount;
import com.ftn.sep.bank.model.BankTransaction;
import com.ftn.sep.bank.model.TransactionStatus;
import com.ftn.sep.bank.security.HmacUtil;
import com.ftn.sep.bank.service.BankAccountService;
import com.ftn.sep.bank.service.PSPService;
import com.ftn.sep.bank.service.TransactionService;
import com.ftn.sep.bank.util.IpsQrGenerator;
import com.ftn.sep.bank.util.IpsQrValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/qr")
@RequiredArgsConstructor
@Slf4j
public class QrPaymentController {

    private final TransactionService transactionService;
    private final BankAccountService bankAccountService;
    private final IpsQrGenerator qrGenerator;
    private final IpsQrValidator qrValidator;
    private final HmacUtil hmacUtil;
    private final PSPService pspService;

    @Value("${psp.merchant.bank.id}")
    private String expectedMerchantId;

    @Value("${merchant.account.number}")
    private String merchantAccountNumber;

    @Value("${merchant.account.name}")
    private String merchantAccountName;

    @Value("${bank.frontend.url}")
    private String bankFrontendUrl;

    @PostMapping("/create")
    public ResponseEntity<QrPaymentResponse> createQrPayment(
            @RequestHeader("X-PSP-Signature") String signature,
            @RequestBody QrPaymentRequest request) {

        log.info("Received QR payment creation request - STAN: {}", request.getStan());

        String payload = hmacUtil.createPayload(
                request.getMerchantId(),
                request.getAmount().toString(),
                request.getCurrency(),
                request.getStan(),
                request.getPspTimestamp().toString()
        );

        if (!hmacUtil.validateSignature(payload, signature)) {
            log.warn("Invalid HMAC signature for STAN: {}", request.getStan());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new QrPaymentResponse(null, null, null, "ERROR", "Invalid signature"));
        }

        if (!expectedMerchantId.equals(request.getMerchantId())) {
            log.warn("Invalid merchant ID: {}", request.getMerchantId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new QrPaymentResponse(null, null, null, "ERROR", "Invalid merchant"));
        }

        try {
            BankTransaction transaction = new BankTransaction();
            transaction.setMerchantId(request.getMerchantId());
            transaction.setStan(request.getStan());
            transaction.setAmount(request.getAmount());
            transaction.setCurrency(request.getCurrency());
            transaction.setPspTimestamp(request.getPspTimestamp());
            transaction.setAcquirerTimestamp(LocalDateTime.now());
            transaction.setStatus(TransactionStatus.PENDING);
            transaction.setPaymentMethod("QR");

            transaction.setPaymentUrlExpiresAt(LocalDateTime.now().plusMinutes(10));

            BankTransaction savedTransaction = transactionService.createTransaction(transaction);

            String qrCodeBase64 = qrGenerator.generateQrCode(
                    merchantAccountNumber,
                    merchantAccountName,
                    request.getAmount(),
                    request.getCurrency(),
                    request.getStan(),
                    "Car rental payment - " + request.getStan()
            );

            String paymentId = "QR-" + savedTransaction.getId();
            String paymentUrl = bankFrontendUrl + "/qr-payment/" + paymentId;

            log.info("QR payment created successfully - Payment ID: {}, STAN: {}",
                    paymentId, request.getStan());

            return ResponseEntity.ok(new QrPaymentResponse(
                    paymentId,
                    paymentUrl,
                    qrCodeBase64,
                    "SUCCESS",
                    "QR payment session created successfully"
            ));

        } catch (Exception e) {
            log.error("Error creating QR payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new QrPaymentResponse(null, null, null, "ERROR", "Failed to create QR payment"));
        }
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<?> getQrPaymentData(@PathVariable String paymentId) {
        log.info("Fetching QR payment data for: {}", paymentId);

        try {
            Long transactionId = Long.parseLong(paymentId.replace("QR-", ""));

            BankTransaction transaction = transactionService.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            String qrCodeBase64 = qrGenerator.generateQrCode(
                    merchantAccountNumber,
                    merchantAccountName,
                    transaction.getAmount(),
                    transaction.getCurrency(),
                    transaction.getStan(),
                    "Car rental payment - " + transaction.getStan()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", paymentId);
            response.put("amount", transaction.getAmount());
            response.put("currency", transaction.getCurrency());
            response.put("recipientName", merchantAccountName);
            response.put("qrCodeBase64", qrCodeBase64);
            response.put("expiresAt", transaction.getPaymentUrlExpiresAt());
            response.put("stan", transaction.getStan());
            response.put("status", transaction.getStatus().name());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching QR payment data", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Payment not found"));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateQrCode(@RequestBody Map<String, String> request) {
        String payload = request.get("payload");

        log.info("Validating QR payload: {}", payload);

        Map<String, Object> validationResult = qrValidator.validateQrPayload(payload);

        return ResponseEntity.ok(validationResult);
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmQrPayment(@RequestBody ConfirmQrPaymentRequest request) {
        try {
            log.info("QR payment confirmation for transaction: {}, account: {}",
                    request.getTransactionId(), request.getAccountNumber());

            Optional<BankTransaction> optTransaction = transactionService.findById(request.getTransactionId());

            if (optTransaction.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            BankTransaction transaction = optTransaction.get();

            if (transaction.getStatus() != TransactionStatus.PENDING) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Transaction already processed"));
            }

            if (LocalDateTime.now().isAfter(transaction.getPaymentUrlExpiresAt())) {
                transactionService.updateTransactionStatus(
                        transaction.getId(),
                        TransactionStatus.EXPIRED,
                        "QR code expired"
                );
                return ResponseEntity.status(HttpStatus.GONE)
                        .body(Map.of("error", "QR code expired"));
            }

            // Validate payer's account and check balance
            if (request.getAccountNumber() == null || request.getAccountNumber().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Account number is required"));
            }

            Optional<BankAccount> optAccount = bankAccountService.findByAccountNumber(request.getAccountNumber());
            if (optAccount.isEmpty()) {
                transactionService.updateTransactionStatus(
                        transaction.getId(),
                        TransactionStatus.FAILED,
                        "Account not found"
                );

                String redirectUrl = pspService.notifyPaymentResult(
                        transaction.getStan(),
                        transaction.getGlobalTransactionId(),
                        transaction.getAcquirerTimestamp(),
                        "FAILED"
                );

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Account not found",
                                "redirectUrl", redirectUrl != null ? redirectUrl : ""
                        ));
            }

            BankAccount payerAccount = optAccount.get();

            if (!bankAccountService.hasSufficientFunds(payerAccount, transaction.getAmount())) {
                transactionService.updateTransactionStatus(
                        transaction.getId(),
                        TransactionStatus.FAILED,
                        "Insufficient funds"
                );

                String redirectUrl = pspService.notifyPaymentResult(
                        transaction.getStan(),
                        transaction.getGlobalTransactionId(),
                        transaction.getAcquirerTimestamp(),
                        "FAILED"
                );

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Insufficient funds",
                                "redirectUrl", redirectUrl != null ? redirectUrl : ""
                        ));
            }

            // Transfer funds: debit payer, credit merchant
            log.info("Payer balance before QR payment: {}", payerAccount.getBalance());
            bankAccountService.reserveFunds(payerAccount, transaction.getAmount());
            log.info("Payer balance after QR payment: {}", payerAccount.getBalance());

            // Credit merchant account
            Optional<BankAccount> optMerchantAccount = bankAccountService.findByAccountNumber(merchantAccountNumber);
            if (optMerchantAccount.isPresent()) {
                bankAccountService.releaseFunds(optMerchantAccount.get(), transaction.getAmount());
                log.info("Merchant account credited: {}", transaction.getAmount());
            }

            transaction.setAccount(payerAccount);
            transactionService.updateTransactionStatus(
                    transaction.getId(),
                    TransactionStatus.COMPLETED,
                    null
            );

            // Notify PSP and get redirect URL from WebShop via PSP
            String redirectUrl = pspService.notifyPaymentResult(
                    transaction.getStan(),
                    transaction.getGlobalTransactionId(),
                    transaction.getAcquirerTimestamp(),
                    "SUCCESS"
            );

            log.info("QR payment completed - GTX: {}, redirect: {}",
                    transaction.getGlobalTransactionId(), redirectUrl);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "redirectUrl", redirectUrl != null ? redirectUrl : "",
                    "message", "Payment confirmed successfully"
            ));

        } catch (Exception e) {
            log.error("Error confirming QR payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Payment confirmation failed"));
        }
    }
}
