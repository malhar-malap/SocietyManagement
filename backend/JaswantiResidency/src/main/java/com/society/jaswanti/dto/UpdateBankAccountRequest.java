package com.society.jaswanti.dto;

import com.society.jaswanti.entity.SocietyBankAccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public class UpdateBankAccountRequest {
    private String accountName;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private SocietyBankAccountType accountType;

    @DecimalMin(value = "0.00", message = "Opening balance must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Opening balance cannot have more than 2 decimal places")
    private BigDecimal openingBalance;

    private Boolean active;

    public UpdateBankAccountRequest() {}

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

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
