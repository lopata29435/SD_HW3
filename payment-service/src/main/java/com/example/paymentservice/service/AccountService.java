package com.example.paymentservice.service;

import com.example.paymentservice.model.Account;
import com.example.paymentservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    @Transactional
    public Account createAccount() {
        Account account = new Account();
        account.setUserId(UUID.randomUUID());
        account.setBalance(BigDecimal.ZERO);
        return accountRepository.save(account);
    }

    @Transactional
    public Account deposit(UUID userId, BigDecimal amount) {
        Account account = accountRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + userId));
        
        account.setBalance(account.getBalance().add(amount));
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + userId));
        return account.getBalance();
    }

    @Transactional(readOnly = true)
    public boolean accountExists(UUID userId) {
        return accountRepository.findByUserId(userId).isPresent();
    }
} 