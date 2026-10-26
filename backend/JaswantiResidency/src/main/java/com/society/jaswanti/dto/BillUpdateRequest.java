package com.society.jaswanti.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fields an admin may change on a bill; null means "leave unchanged". Binding the MaintenanceBill entity instead
 * meant its default lateFee of 0 counted as "set", so every edit silently wiped the bill's late fee.
 */
public class BillUpdateRequest {
    private String billingMonth;
    private BigDecimal amount;
    private LocalDate dueDate;
    private BigDecimal lateFee;

    public String getBillingMonth() { return billingMonth; }
    public void setBillingMonth(String billingMonth) { this.billingMonth = billingMonth; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public BigDecimal getLateFee() { return lateFee; }
    public void setLateFee(BigDecimal lateFee) { this.lateFee = lateFee; }
}
