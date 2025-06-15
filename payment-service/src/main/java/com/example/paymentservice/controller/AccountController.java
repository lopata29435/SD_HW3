package com.example.paymentservice.controller;

import com.example.paymentservice.model.Account;
import com.example.paymentservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<Account> createAccount() {
        return ResponseEntity.ok(accountService.createAccount());
    }

    @PostMapping("/{userId}/deposit")
    public ResponseEntity<Account> deposit(@PathVariable UUID userId, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(accountService.deposit(userId, amount));
    }

    @GetMapping("/{userId}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable UUID userId) {
        return ResponseEntity.ok(accountService.getBalance(userId));
    }

    @GetMapping("/check/{userId}")
    public ResponseEntity<Boolean> checkAccount(@PathVariable UUID userId) {
        return ResponseEntity.ok(accountService.accountExists(userId));
    }
} 