package com.society.jaswanti.dto;

import com.society.jaswanti.entity.SocietyBankAccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CreateBankAccountRequest {

    @NotBlank(message = "Account name is required")
    private String accountName;

    private String bankName;
    private String accountNumber;
    private String ifscCode;

    @NotNull(message = "Account type is required")
    private SocietyBankAccountType accountType = SocietyBankAccountType.BANK;

    @NotNull(message = "Opening balance is required")
    @DecimalMin(value = "0.00", message = "Opening balance must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Opening balance cannot have more than 2 decimal places")
    private BigDecimal openingBalance = BigDecimal.ZERO;

    public CreateBankAccountRequest() {}

    public CreateBankAccountRequest(String accountName, String bankName, String accountNumber, String ifscCode,
                                    SocietyBankAccountType accountType, BigDecimal openingBalance) {
        this.accountName = accountName;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.ifscCode = ifscCode;
        this.accountType = accountType;
        this.openingBalance = openingBalance;
    }

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
}
