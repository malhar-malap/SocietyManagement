package com.society.jaswanti.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionTimestampPrecisionTests {

    @Test
    @DisplayName("Transaction timestamps keep database precision (microseconds), even from a nanosecond clock")
    void timestampsAreTruncatedToMicroseconds() {
        LocalDateTime nanos = LocalDateTime.of(2026, 10, 26, 10, 4, 28, 270_521_173); // as a Linux clock returns it

        Transaction tx = new Transaction();
        tx.setTimestamp(nanos);
        tx.setApprovedAt(nanos);

        assertThat(tx.getTimestamp().getNano()).isEqualTo(270_521_000);
        assertThat(tx.getApprovedAt().getNano()).isEqualTo(270_521_000);
        assertThat(new Transaction().getTimestamp().getNano() % 1000).isZero();
    }
}
