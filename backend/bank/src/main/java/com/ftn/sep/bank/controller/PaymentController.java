package com.ftn.sep.bank.controller;

import com.ftn.sep.bank.dto.*;
import com.ftn.sep.bank.model.BankAccount;
import com.ftn.sep.bank.model.BankTransaction;
import com.ftn.sep.bank.model.CardInfo;
import com.ftn.sep.bank.model.TransactionStatus;
import com.ftn.sep.bank.security.HmacUtil;
import com.ftn.sep.bank.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final TransactionService transactionService;
    private final CardValidationService cardValidationService;
    private final CardService cardService;
    private final BankAccountService bankAccountService;
    private final PSPService pspService;
    private final HmacUtil hmacUtil;
    private final AuditService auditService;

    @Value("${psp.merchant.bank.id}")
    private String merchantId;

    @PostMapping("/create")
    public ResponseEntity<CreatePaymentResponse> createPayment(
            @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "X-PSP-Signature", required = false) String signature) {

        log.info("Received payment creation request from PSP - STAN: {}", request.getStan());

        if (signature == null || signature.isEmpty()) {
            log.error("Missing HMAC signature in request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CreatePaymentResponse(null, null, "UNAUTHORIZED",
                            "Missing authentication signature"));
        }

        String payload = hmacUtil.createPayload(
                request.getMerchantId(),
                request.getAmount().toString(),
                request.getCurrency(),
                request.getStan(),
                request.getPspTimestamp().toString()
        );

        if (!hmacUtil.validateSignature(payload, signature)) {
            log.error("Invalid HMAC signature for STAN: {}", request.getStan());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CreatePaymentResponse(null, null, "UNAUTHORIZED",
                            "Invalid authentication signature"));
        }

        log.info("HMAC signature validated successfully for STAN: {}", request.getStan());

        if (!merchantId.equals(request.getMerchantId())) {
            log.error("Invalid merchant ID: {}", request.getMerchantId());
            return ResponseEntity.badRequest()
                    .body(new CreatePaymentResponse(null, null, "INVALID_MERCHANT",
                            "Invalid merchant credentials"));
        }

        BankTransaction transaction = new BankTransaction();
        transaction.setMerchantId(request.getMerchantId());
        transaction.setStan(request.getStan());
        transaction.setPspTimestamp(request.getPspTimestamp());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());

        BankTransaction savedTransaction = transactionService.createTransaction(transaction);

        CreatePaymentResponse response = new CreatePaymentResponse(
                savedTransaction.getPaymentId(),
                savedTransaction.getPaymentUrl(),
                "SUCCESS",
                "Payment URL created successfully"
        );

        log.info("Created payment URL: {} for STAN: {}",
                savedTransaction.getPaymentUrl(), request.getStan());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/form/{paymentId}")
    public ResponseEntity<PaymentFormData> getPaymentFormData(@PathVariable String paymentId) {
        log.info("Fetching payment form data for Payment ID: {}", paymentId);

        return transactionService.findByPaymentId(paymentId)
                .map(transaction -> {
                    if (transaction.isPaymentUrlExpired()) {
                        transactionService.updateTransactionStatus(
                                transaction.getId(),
                                TransactionStatus.EXPIRED,
                                "Payment URL expired"
                        );
                        return ResponseEntity.status(HttpStatus.GONE)
                                .body(new PaymentFormData(
                                        paymentId,
                                        transaction.getAmount(),
                                        transaction.getCurrency(),
                                        true
                                ));
                    }

                    PaymentFormData formData = new PaymentFormData(
                            paymentId,
                            transaction.getAmount(),
                            transaction.getCurrency(),
                            false
                    );
                    return ResponseEntity.ok(formData);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/process")
    public ResponseEntity<ProcessPaymentResponse> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request,
            HttpServletRequest httpRequest) {

        log.info("Processing payment for Payment ID: {}", request.getPaymentId());
        String clientIp = httpRequest.getRemoteAddr();
        String panLastFour = request.getPan() != null && request.getPan().length() >= 4
                ? request.getPan().substring(request.getPan().length() - 4) : "????";

        BankTransaction transaction = transactionService.findByPaymentId(request.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (transaction.isPaymentUrlExpired()) {
            transactionService.updateTransactionStatus(
                    transaction.getId(),
                    TransactionStatus.EXPIRED,
                    "Payment URL expired"
            );
            auditService.logPaymentAttempt(request.getPaymentId(), panLastFour,
                    "FAILURE", "Payment session expired", clientIp);
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(new ProcessPaymentResponse(
                            null,
                            transaction.getStan(),
                            "EXPIRED",
                            "Payment session has expired",
                            null
                    ));
        }

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            auditService.logPaymentAttempt(request.getPaymentId(), panLastFour,
                    "FAILURE", "Transaction already processed", clientIp);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ProcessPaymentResponse(
                            transaction.getGlobalTransactionId(),
                            transaction.getStan(),
                            "ALREADY_PROCESSED",
                            "This payment has already been processed",
                            null
                    ));
        }

        // Stage 1: Format validation (Luhn, expiry format, CVV format)
        if (!cardValidationService.validateCard(
                request.getPan(),
                request.getExpiryDate(),
                request.getSecurityCode())) {

            transactionService.updateTransactionStatus(
                    transaction.getId(),
                    TransactionStatus.FAILED,
                    "Invalid card data format"
            );

            String redirectUrl = pspService.notifyPaymentResult(
                    transaction.getStan(),
                    transaction.getGlobalTransactionId(),
                    transaction.getAcquirerTimestamp(),
                    "FAILED"
            );

            auditService.logPaymentAttempt(request.getPaymentId(), panLastFour,
                    "FAILURE", "Invalid card data format", clientIp);

            return ResponseEntity.badRequest()
                    .body(new ProcessPaymentResponse(
                            transaction.getGlobalTransactionId(),
                            transaction.getStan(),
                            "FAILED",
                            "Invalid card data",
                            redirectUrl
                    ));
        }

        // Stage 2: Card data validation against database
        if (!cardService.validateCardData(
                request.getPan(),
                request.getCardHolderName(),
                request.getExpiryDate(),
                request.getSecurityCode())) {

            transactionService.updateTransactionStatus(
                    transaction.getId(),
                    TransactionStatus.FAILED,
                    "Card validation failed"
            );

            String redirectUrl = pspService.notifyPaymentResult(
                    transaction.getStan(),
                    transaction.getGlobalTransactionId(),
                    transaction.getAcquirerTimestamp(),
                    "FAILED"
            );

            auditService.logPaymentAttempt(request.getPaymentId(), panLastFour,
                    "FAILURE", "Card validation failed", clientIp);

            return ResponseEntity.badRequest()
                    .body(new ProcessPaymentResponse(
                            transaction.getGlobalTransactionId(),
                            transaction.getStan(),
                            "FAILED",
                            "Invalid card information",
                            redirectUrl
                    ));
        }

        CardInfo card = cardService.findByPan(request.getPan())
                .orElseThrow(() -> new RuntimeException("Card not found"));

        BankAccount account = card.getAccount();

        // Stage 3: Balance check
        if (!bankAccountService.hasSufficientFunds(account, transaction.getAmount())) {
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

            auditService.logPaymentAttempt(request.getPaymentId(), panLastFour,
                    "FAILURE", "Insufficient funds", clientIp);

            return ResponseEntity.badRequest()
                    .body(new ProcessPaymentResponse(
                            transaction.getGlobalTransactionId(),
                            transaction.getStan(),
                            "FAILED",
                            "Insufficient funds",
                            redirectUrl
                    ));
        }

        // Stage 4: Reserve funds and complete
        try {
            log.info("Balance before the payment: {}", account.getBalance());

            bankAccountService.reserveFunds(account, transaction.getAmount());
            transaction.setAccount(account);
            transactionService.updateTransactionStatus(
                    transaction.getId(),
                    TransactionStatus.RESERVED,
                    null
            );

            log.info("Payment successful - GTX: {}, STAN: {}",
                    transaction.getGlobalTransactionId(),
                    transaction.getStan());
            log.info("Balance after the payment: {}", account.getBalance());

            String redirectUrl = pspService.notifyPaymentResult(
                    transaction.getStan(),
                    transaction.getGlobalTransactionId(),
                    transaction.getAcquirerTimestamp(),
                    "SUCCESS"
            );

            auditService.logPaymentAttempt(request.getPaymentId(), panLastFour,
                    "SUCCESS", "Payment processed successfully", clientIp);

            return ResponseEntity.ok(new ProcessPaymentResponse(
                    transaction.getGlobalTransactionId(),
                    transaction.getStan(),
                    "SUCCESS",
                    "Payment processed successfully",
                    redirectUrl
            ));

        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Concurrent payment attempt detected for Payment ID: {}", request.getPaymentId());
            auditService.logPaymentAttempt(request.getPaymentId(), panLastFour,
                    "FAILURE", "Concurrent payment attempt blocked", clientIp);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ProcessPaymentResponse(
                            transaction.getGlobalTransactionId(),
                            transaction.getStan(),
                            "ALREADY_PROCESSED",
                            "This payment has already been processed",
                            null
                    ));
        } catch (Exception e) {
            log.error("Error processing payment", e);
            transactionService.updateTransactionStatus(
                    transaction.getId(),
                    TransactionStatus.ERROR,
                    e.getMessage()
            );

            String redirectUrl = pspService.notifyPaymentResult(
                    transaction.getStan(),
                    transaction.getGlobalTransactionId(),
                    transaction.getAcquirerTimestamp(),
                    "ERROR"
            );

            auditService.logPaymentAttempt(request.getPaymentId(), panLastFour,
                    "ERROR", "Payment processing error: " + e.getMessage(), clientIp);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ProcessPaymentResponse(
                            transaction.getGlobalTransactionId(),
                            transaction.getStan(),
                            "ERROR",
                            "Payment processing error",
                            redirectUrl
                    ));
        }
    }
}
