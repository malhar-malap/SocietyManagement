package com.society.jaswanti.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class OrderResponseDto {
    private String orderId;
    private BigDecimal amount;
    private String currency;

    public OrderResponseDto(String orderId, BigDecimal amount, String currency) {
        this.orderId = orderId;
        this.amount = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : null;
        this.currency = currency;
    }

    public OrderResponseDto(String orderId, Double amount, String currency) {
        this(orderId, amount != null ? BigDecimal.valueOf(amount) : null, currency);
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @JsonSetter
    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : null;
    }

    @JsonIgnore
    public void setAmount(Double amount) {
        this.amount = amount != null ? BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP) : null;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
