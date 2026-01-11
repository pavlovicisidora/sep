package com.ftn.sep.bank.service;

import com.ftn.sep.bank.model.BankAccount;
import com.ftn.sep.bank.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;

    public Optional<BankAccount> findByAccountNumber(String accountNumber) {
        return bankAccountRepository.findByAccountNumber(accountNumber);
    }

    public boolean hasSufficientFunds(BankAccount account, BigDecimal amount) {
        boolean sufficient = account.getBalance().compareTo(amount) >= 0;
        log.debug("Sufficient funds check for account {}: {}",
                account.getAccountNumber(), sufficient);
        return sufficient;
    }

    @Transactional
    public void reserveFunds(BankAccount account, BigDecimal amount) {
        if (!hasSufficientFunds(account, amount)) {
            throw new RuntimeException("Insufficient funds");
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        bankAccountRepository.save(account);

        log.info("Reserved {} {} from account {}",
                amount, account.getCurrency(), account.getAccountNumber());
    }

    @Transactional
    public void releaseFunds(BankAccount account, BigDecimal amount) {
        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        bankAccountRepository.save(account);

        log.info("Released {} {} to account {}",
                amount, account.getCurrency(), account.getAccountNumber());
    }
}
