package com.example.orderimport.importdata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

final class CsvRowStream {

    private CsvRowStream() {
    }

    /**
     * Caller must close the returned stream (try-with-resources) so the file
     * handle is released. Rows are parsed lazily; nothing is loaded into a List.
     */
    static Stream<OrderLineCsvRow> stream(Path csvFile) throws IOException {
        BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);
        AtomicInteger lineNumber = new AtomicInteger(1);
        return reader.lines()
                .skip(1)
                .filter(line -> !line.isBlank())
                .map(raw -> OrderLineCsvRow.parse(raw, lineNumber.incrementAndGet()))
                .onClose(() -> {
                    try {
                        reader.close();
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                });
    }
}
