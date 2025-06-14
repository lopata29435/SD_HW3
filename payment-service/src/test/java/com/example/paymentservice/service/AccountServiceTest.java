package com.example.paymentservice.service;

import com.example.paymentservice.model.Account;
import com.example.paymentservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private UUID userId;
    private Account account;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        account = new Account();
        account.setUserId(userId);
        account.setBalance(BigDecimal.ZERO);
    }

    @Test
    void createAccount_ShouldCreateNewAccount() {
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        Account result = accountService.createAccount();

        assertNotNull(result);
        assertNotNull(result.getUserId());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void deposit_ShouldUpdateBalance() {
        BigDecimal amount = new BigDecimal("100.00");
        when(accountRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        Account result = accountService.deposit(userId, amount);

        assertEquals(amount, result.getBalance());
        verify(accountRepository).findByUserIdWithLock(userId);
        verify(accountRepository).save(account);
    }

    @Test
    void deposit_ShouldThrowException_WhenAccountNotFound() {
        BigDecimal amount = new BigDecimal("100.00");
        when(accountRepository.findByUserIdWithLock(userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> accountService.deposit(userId, amount));
        verify(accountRepository).findByUserIdWithLock(userId);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getBalance_ShouldReturnBalance() {
        BigDecimal expectedBalance = new BigDecimal("100.00");
        account.setBalance(expectedBalance);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        BigDecimal result = accountService.getBalance(userId);

        assertEquals(expectedBalance, result);
        verify(accountRepository).findByUserId(userId);
    }

    @Test
    void getBalance_ShouldThrowException_WhenAccountNotFound() {
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> accountService.getBalance(userId));
        verify(accountRepository).findByUserId(userId);
    }

    @Test
    void accountExists_ShouldReturnTrue_WhenAccountExists() {
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        boolean result = accountService.accountExists(userId);

        assertTrue(result);
        verify(accountRepository).findByUserId(userId);
    }

    @Test
    void accountExists_ShouldReturnFalse_WhenAccountDoesNotExist() {
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        boolean result = accountService.accountExists(userId);

        assertFalse(result);
        verify(accountRepository).findByUserId(userId);
    }
} 