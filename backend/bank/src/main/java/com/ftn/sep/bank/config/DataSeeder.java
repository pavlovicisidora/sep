package com.ftn.sep.bank.config;

import com.ftn.sep.bank.model.BankAccount;
import com.ftn.sep.bank.model.CardInfo;
import com.ftn.sep.bank.model.CardType;
import com.ftn.sep.bank.repository.BankAccountRepository;
import com.ftn.sep.bank.repository.CardInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final BankAccountRepository accountRepository;
    private final CardInfoRepository cardRepository;

    @Override
    public void run(String... args) {
        seedAccounts();
    }

    private void seedAccounts() {
        if (accountRepository.count() > 0) {
            log.info("Bank accounts already exist, skipping seed");
            return;
        }

        // Account 1
        BankAccount account1 = new BankAccount();
        account1.setAccountNumber("1234567890");
        account1.setAccountHolderName("Marko Marković");
        account1.setBalance(new BigDecimal("500000.00"));
        account1.setCurrency("RSD");
        account1.setActive(true);
        accountRepository.save(account1);

        // Card 1 - Visa
        CardInfo card1 = new CardInfo();
        card1.setAccount(account1);
        card1.setPan("4532015112830366");
        card1.setCardHolderName("Marko Marković");
        card1.setExpiryDate(LocalDate.of(2027, 12, 1));
        card1.setSecurityCode("123");
        card1.setCardType(CardType.VISA);
        card1.setActive(true);
        cardRepository.save(card1);

        // Account 2
        BankAccount account2 = new BankAccount();
        account2.setAccountNumber("9876543210");
        account2.setAccountHolderName("Ana Anić");
        account2.setBalance(new BigDecimal("5000.00"));
        account2.setCurrency("RSD");
        account2.setActive(true);
        accountRepository.save(account2);

        // Card 2 - Mastercard
        CardInfo card2 = new CardInfo();
        card2.setAccount(account2);
        card2.setPan("5425233430109903");
        card2.setCardHolderName("Ana Anić");
        card2.setExpiryDate(LocalDate.of(2026, 6, 1));
        card2.setSecurityCode("456");
        card2.setCardType(CardType.MASTERCARD);
        card2.setActive(true);
        cardRepository.save(card2);

        // Account 3 - expired
        BankAccount account3 = new BankAccount();
        account3.setAccountNumber("1122334455");
        account3.setAccountHolderName("Petar Petrović");
        account3.setBalance(new BigDecimal("100000.00"));
        account3.setCurrency("RSD");
        account3.setActive(true);
        accountRepository.save(account3);

        // Card 3 - expired
        CardInfo card3 = new CardInfo();
        card3.setAccount(account3);
        card3.setPan("4024007134564842"); // Valid Luhn
        card3.setCardHolderName("Petar Petrović");
        card3.setExpiryDate(LocalDate.of(2023, 12, 1));
        card3.setSecurityCode("789");
        card3.setCardType(CardType.VISA);
        card3.setActive(true);
        cardRepository.save(card3);

        log.info("Seeded {} bank accounts with cards", accountRepository.count());
    }
}
