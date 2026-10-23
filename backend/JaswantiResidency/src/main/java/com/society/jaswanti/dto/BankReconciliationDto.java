package com.society.jaswanti.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BankReconciliationDto {
    private Long bankAccountId;
    private String accountName;
    private String accountType;
    private BigDecimal openingBalance;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    private BigDecimal calculatedBalance;
    private BigDecimal currentBalance;
    private BigDecimal discrepancy;
    private boolean reconciled;
    private long transactionCount;
    private LocalDateTime reconciledAt;

    public BankReconciliationDto() {}

    public BankReconciliationDto(Long bankAccountId, String accountName, String accountType,
                                 BigDecimal openingBalance, BigDecimal totalCredits, BigDecimal totalDebits,
                                 BigDecimal calculatedBalance, BigDecimal currentBalance, BigDecimal discrepancy,
                                 boolean reconciled, long transactionCount, LocalDateTime reconciledAt) {
        this.bankAccountId = bankAccountId;
        this.accountName = accountName;
        this.accountType = accountType;
        this.openingBalance = openingBalance;
        this.totalCredits = totalCredits;
        this.totalDebits = totalDebits;
        this.calculatedBalance = calculatedBalance;
        this.currentBalance = currentBalance;
        this.discrepancy = discrepancy;
        this.reconciled = reconciled;
        this.transactionCount = transactionCount;
        this.reconciledAt = reconciledAt;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    public BigDecimal getTotalCredits() {
        return totalCredits;
    }

    public void setTotalCredits(BigDecimal totalCredits) {
        this.totalCredits = totalCredits;
    }

    public BigDecimal getTotalDebits() {
        return totalDebits;
    }

    public void setTotalDebits(BigDecimal totalDebits) {
        this.totalDebits = totalDebits;
    }

    public BigDecimal getCalculatedBalance() {
        return calculatedBalance;
    }

    public void setCalculatedBalance(BigDecimal calculatedBalance) {
        this.calculatedBalance = calculatedBalance;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getDiscrepancy() {
        return discrepancy;
    }

    public void setDiscrepancy(BigDecimal discrepancy) {
        this.discrepancy = discrepancy;
    }

    public boolean isReconciled() {
        return reconciled;
    }

    public void setReconciled(boolean reconciled) {
        this.reconciled = reconciled;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public LocalDateTime getReconciledAt() {
        return reconciledAt;
    }

    public void setReconciledAt(LocalDateTime reconciledAt) {
        this.reconciledAt = reconciledAt;
    }
}
