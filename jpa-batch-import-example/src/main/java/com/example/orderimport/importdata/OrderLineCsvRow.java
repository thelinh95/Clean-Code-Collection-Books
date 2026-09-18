package com.example.orderimport.importdata;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One CSV row = one order line. Order header fields are repeated on every line
 * of the same order so the file can be streamed twice without buffering rows.
 *
 * <pre>
 * order_code,customer_code,ordered_at,product_sku,quantity,unit_price
 * ORD-000001,CUST-001,2026-01-15T10:00:00Z,SKU-010,3,15.50
 * </pre>
 */
public record OrderLineCsvRow(
        int lineNumber,
        String orderCode,
        String customerCode,
        Instant orderedAt,
        String productSku,
        int quantity,
        BigDecimal unitPrice
) {

    public static OrderLineCsvRow parse(String raw, int lineNumber) {
        String[] cols = raw.split(",", -1);
        if (cols.length != 6) {
            throw new IllegalArgumentException(
                    "Line " + lineNumber + ": expected 6 columns but got " + cols.length + " [" + raw + "]");
        }
        try {
            return new OrderLineCsvRow(
                    lineNumber,
                    cols[0].trim(),
                    cols[1].trim(),
                    Instant.parse(cols[2].trim()),
                    cols[3].trim(),
                    Integer.parseInt(cols[4].trim()),
                    new BigDecimal(cols[5].trim())
            );
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Line " + lineNumber + ": cannot parse [" + raw + "]", ex);
        }
    }
}
