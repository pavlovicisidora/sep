package com.ftn.sep.bank.controller;

import com.ftn.sep.bank.dto.*;
import com.ftn.sep.bank.model.BankAccount;
import com.ftn.sep.bank.model.BankTransaction;
import com.ftn.sep.bank.model.CardInfo;
import com.ftn.sep.bank.model.TransactionStatus;
import com.ftn.sep.bank.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/create")
    public ResponseEntity<CreatePaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {

        log.info("Received payment creation request from PSP - STAN: {}", request.getStan());

        // TODO: Validacija merchantId (PSP) - za sada preskačemo

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
            @Valid @RequestBody ProcessPaymentRequest request) {

        log.info("Processing payment for Payment ID: {}", request.getPaymentId());

        BankTransaction transaction = transactionService.findByPaymentId(request.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (transaction.isPaymentUrlExpired()) {
            transactionService.updateTransactionStatus(
                    transaction.getId(),
                    TransactionStatus.EXPIRED,
                    "Payment URL expired"
            );
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(new ProcessPaymentResponse(
                            null,
                            transaction.getStan(),
                            "EXPIRED",
                            "Payment session has expired"
                    ));
        }

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ProcessPaymentResponse(
                            transaction.getGlobalTransactionId(),
                            transaction.getStan(),
                            "ALREADY_PROCESSED",
                            "This payment has already been processed"
                    ));
        }

        if (!cardValidationService.validateCard(
                request.getPan(),
                request.getExpiryDate(),
                request.getSecurityCode())) {

            transactionService.updateTransactionStatus(
                    transaction.getId(),
                    TransactionStatus.FAILED,
                    "Invalid card data format"
            );

            return ResponseEntity.badRequest()
                    .body(new ProcessPaymentResponse(
                            transaction.getGlobalTransactionId(),
                            transaction.getStan(),
                            "FAILED",
                            "Invalid card data"
                    ));
        }

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

            pspService.notifyPaymentResult(
                    transaction.getStan(),
                    transaction.getGlobalTransactionId(),
                    transaction.getAcquirerTimestamp(),
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body(new ProcessPaymentResponse(
                            transaction.getGlobalTransactionId(),
                            transaction.getStan(),
                            "FAILED",
                            "Invalid card information"
                    ));
        }

        CardInfo card = cardService.findByPan(request.getPan())
                .orElseThrow(() -> new RuntimeException("Card not found"));

        BankAccount account = card.getAccount();

        if (!bankAccountService.hasSufficientFunds(account, transaction.getAmount())) {
            transactionService.updateTransactionStatus(
                    transaction.getId(),
                    TransactionStatus.FAILED,
                    "Insufficient funds"
            );

            pspService.notifyPaymentResult(
                    transaction.getStan(),
                    transaction.getGlobalTransactionId(),
                    transaction.getAcquirerTimestamp(),
                    "FAILED"
            );

            return ResponseEntity.badRequest()
                    .body(new ProcessPaymentResponse(
                            transaction.getGlobalTransactionId(),
                            transaction.getStan(),
                            "FAILED",
                            "Insufficient funds"
                    ));
        }

        try {
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

            pspService.notifyPaymentResult(
                    transaction.getStan(),
                    transaction.getGlobalTransactionId(),
                    transaction.getAcquirerTimestamp(),
                    "SUCCESS"
            );

            return ResponseEntity.ok(new ProcessPaymentResponse(
                    transaction.getGlobalTransactionId(),
                    transaction.getStan(),
                    "SUCCESS",
                    "Payment processed successfully"
            ));

        } catch (Exception e) {
            log.error("Error processing payment", e);
            transactionService.updateTransactionStatus(
                    transaction.getId(),
                    TransactionStatus.ERROR,
                    e.getMessage()
            );

            pspService.notifyPaymentResult(
                    transaction.getStan(),
                    transaction.getGlobalTransactionId(),
                    transaction.getAcquirerTimestamp(),
                    "ERROR"
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ProcessPaymentResponse(
                            transaction.getGlobalTransactionId(),
                            transaction.getStan(),
                            "ERROR",
                            "Payment processing error"
                    ));
        }
    }
}
