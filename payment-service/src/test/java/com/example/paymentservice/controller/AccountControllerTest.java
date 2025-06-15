package com.example.paymentservice.controller;

import com.example.paymentservice.model.Account;
import com.example.paymentservice.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

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
    void createAccount_ShouldReturnCreatedAccount() {
        when(accountService.createAccount()).thenReturn(account);

        ResponseEntity<Account> response = accountController.createAccount();

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(userId, response.getBody().getUserId());
        verify(accountService).createAccount();
    }

    @Test
    void deposit_ShouldReturnUpdatedAccount() {
        BigDecimal amount = new BigDecimal("100.00");
        account.setBalance(amount);
        when(accountService.deposit(userId, amount)).thenReturn(account);

        ResponseEntity<Account> response = accountController.deposit(userId, amount);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(amount, response.getBody().getBalance());
        verify(accountService).deposit(userId, amount);
    }

    @Test
    void getBalance_ShouldReturnBalance() {
        BigDecimal expectedBalance = new BigDecimal("100.00");
        when(accountService.getBalance(userId)).thenReturn(expectedBalance);

        ResponseEntity<BigDecimal> response = accountController.getBalance(userId);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(expectedBalance, response.getBody());
        verify(accountService).getBalance(userId);
    }

    @Test
    void checkAccount_ShouldReturnTrue_WhenAccountExists() {
        when(accountService.accountExists(userId)).thenReturn(true);

        ResponseEntity<Boolean> response = accountController.checkAccount(userId);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody());
        verify(accountService).accountExists(userId);
    }

    @Test
    void checkAccount_ShouldReturnFalse_WhenAccountDoesNotExist() {
        when(accountService.accountExists(userId)).thenReturn(false);

        ResponseEntity<Boolean> response = accountController.checkAccount(userId);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertFalse(response.getBody());
        verify(accountService).accountExists(userId);
    }
} 