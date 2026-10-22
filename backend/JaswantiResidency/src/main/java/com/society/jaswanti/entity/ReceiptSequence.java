package com.society.jaswanti.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "receipt_sequences")
public class ReceiptSequence {

    @Id
    @Column(length = 6, nullable = false)
    private String period;

    @Column(name = "next_val", nullable = false)
    private Long nextVal;

    public ReceiptSequence() {}

    public ReceiptSequence(String period, Long nextVal) {
        this.period = period;
        this.nextVal = nextVal;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Long getNextVal() {
        return nextVal;
    }

    public void setNextVal(Long nextVal) {
        this.nextVal = nextVal;
    }
}
