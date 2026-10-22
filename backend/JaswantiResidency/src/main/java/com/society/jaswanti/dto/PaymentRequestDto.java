package com.society.jaswanti.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PaymentRequestDto {
    private BigDecimal amount;
    private Long residentId;
    private Long billId;

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

    public Long getResidentId() {
        return residentId;
    }

    public void setResidentId(Long residentId) {
        this.residentId = residentId;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }
}
