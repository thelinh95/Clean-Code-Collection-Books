package com.example.orderimport.importdata;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

@Component
public class DemoCsvGenerator {

    public static final String HEADER = "order_code,customer_code,ordered_at,product_sku,quantity,unit_price";

    /**
     * Writes {@code lineCount} order-line rows. Orders are grouped so several
     * consecutive lines share the same order_code (5 lines per order).
     */
    public Path write(Path csvFile, int lineCount, int customerCount, int productCount) throws IOException {
        if (lineCount <= 0) {
            throw new IllegalArgumentException("lineCount must be positive");
        }
        Path parent = csvFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        int linesPerOrder = 5;
        Instant start = Instant.parse("2026-01-15T00:00:00Z");

        try (BufferedWriter writer = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            int orderIndex = 0;
            for (int line = 1; line <= lineCount; line++) {
                if ((line - 1) % linesPerOrder == 0) {
                    orderIndex++;
                }
                int customerIndex = ((orderIndex - 1) % customerCount) + 1;
                int productIndex = ((line - 1) % productCount) + 1;
                Instant orderedAt = start.plus(orderIndex, ChronoUnit.MINUTES);
                int quantity = 1 + (line % 4);
                BigDecimal unitPrice = BigDecimal.valueOf(10 + (productIndex % 20))
                        .setScale(2, RoundingMode.UNNECESSARY);

                writer.write("ORD-%06d,%s,%s,%s,%d,%s".formatted(
                        orderIndex,
                        MasterDataSeeder.code("CUST", customerIndex),
                        orderedAt,
                        MasterDataSeeder.code("SKU", productIndex),
                        quantity,
                        unitPrice.toPlainString()
                ));
                writer.newLine();
            }
        }
        return csvFile;
    }

    public static int expectedOrderCount(int lineCount) {
        return (lineCount + 4) / 5;
    }
}
