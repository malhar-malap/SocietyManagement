package com.society.jaswanti.dto;

import com.society.jaswanti.entity.SocietyBankAccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SocietyBankAccountDto {
    private Long id;
    private String accountName;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private SocietyBankAccountType accountType;
    private BigDecimal openingBalance;
    private BigDecimal currentBalance;
    private boolean isActive;
    private LocalDateTime createdAt;

    public SocietyBankAccountDto() {}

    public SocietyBankAccountDto(Long id, String accountName, String bankName, String accountNumber, String ifscCode,
                                 SocietyBankAccountType accountType, BigDecimal openingBalance, BigDecimal currentBalance,
                                 boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.accountName = accountName;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.ifscCode = ifscCode;
        this.accountType = accountType;
        this.openingBalance = openingBalance;
        this.currentBalance = currentBalance;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public SocietyBankAccountType getAccountType() { return accountType; }
    public void setAccountType(SocietyBankAccountType accountType) { this.accountType = accountType; }

    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
