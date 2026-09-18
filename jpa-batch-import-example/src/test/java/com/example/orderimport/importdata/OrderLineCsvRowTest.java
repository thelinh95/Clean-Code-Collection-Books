package com.example.orderimport.importdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class OrderLineCsvRowTest {

    @Test
    void parseValidRow() {
        OrderLineCsvRow row = OrderLineCsvRow.parse(
                "ORD-000001,CUST-001,2026-01-15T10:00:00Z,SKU-010,3,15.50",
                2
        );

        assertThat(row.lineNumber()).isEqualTo(2);
        assertThat(row.orderCode()).isEqualTo("ORD-000001");
        assertThat(row.customerCode()).isEqualTo("CUST-001");
        assertThat(row.orderedAt()).isEqualTo(Instant.parse("2026-01-15T10:00:00Z"));
        assertThat(row.productSku()).isEqualTo("SKU-010");
        assertThat(row.quantity()).isEqualTo(3);
        assertThat(row.unitPrice()).isEqualByComparingTo(new BigDecimal("15.50"));
    }

    @Test
    void parseRejectsWrongColumnCount() {
        assertThatThrownBy(() -> OrderLineCsvRow.parse("ORD-1,CUST-1", 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Line 4");
    }
}
