package com.example.orderservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderRequest {
    private String userId;
    private BigDecimal amount;
} 