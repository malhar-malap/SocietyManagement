package com.society.jaswanti.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "society_bank_accounts")
public class SocietyBankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private SocietyBankAccountType accountType = SocietyBankAccountType.BANK;

    @Column(name = "opening_balance", precision = 12, scale = 2, nullable = false)
    private BigDecimal openingBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "current_balance", precision = 12, scale = 2, nullable = false)
    private BigDecimal currentBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public SocietyBankAccount() {}

    public SocietyBankAccount(String accountName, String bankName, String accountNumber, String ifscCode,
                              SocietyBankAccountType accountType, BigDecimal openingBalance) {
        this.accountName = accountName;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.ifscCode = ifscCode;
        this.accountType = accountType;
        this.openingBalance = normalize(openingBalance);
        this.currentBalance = normalize(openingBalance);
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    private static BigDecimal normalize(BigDecimal val) {
        return val != null ? val.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
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

    @JsonSetter
    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = normalize(openingBalance);
    }

    @JsonIgnore
    public void setOpeningBalance(Double openingBalance) {
        this.openingBalance = openingBalance != null ? normalize(BigDecimal.valueOf(openingBalance)) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getCurrentBalance() { return currentBalance; }

    @JsonSetter
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = normalize(currentBalance);
    }

    @JsonIgnore
    public void setCurrentBalance(Double currentBalance) {
        this.currentBalance = currentBalance != null ? normalize(BigDecimal.valueOf(currentBalance)) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
