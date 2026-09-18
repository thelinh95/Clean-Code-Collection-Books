package com.example.orderimport.importdata;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.orderimport.domain.Customer;
import com.example.orderimport.domain.OrderLine;
import com.example.orderimport.domain.Product;
import com.example.orderimport.domain.SalesOrder;
import com.example.orderimport.repository.CustomerRepository;
import com.example.orderimport.repository.ProductRepository;

import jakarta.persistence.EntityManager;

@Service
public class OrderImportService {

    private static final Logger log = LoggerFactory.getLogger(OrderImportService.class);

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final EntityManager entityManager;
    private final ChunkPersister chunkPersister;

    public OrderImportService(
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            EntityManager entityManager,
            ChunkPersister chunkPersister
    ) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.entityManager = entityManager;
        this.chunkPersister = chunkPersister;
    }

    /**
     * Two-pass file stream:
     * <ol>
     *   <li>unique orders only — persist SalesOrder in chunks</li>
     *   <li>every CSV row — persist OrderLine with getReference (no SELECT)</li>
     * </ol>
     * Lookups are id maps (not managed entities). After each chunk, flush + clear
     * so the persistence context never holds more than {@link ChunkPersister#CHUNK_SIZE}
     * entities. SEQUENCE assigns ids on persist(), so we can keep orderCode → id
     * after clear().
     */
    @Transactional
    public ImportResult importFromCsv(Path csvFile) throws IOException {
        long started = System.currentTimeMillis();

        Map<String, Long> customerIds = IdLookup.toMap(customerRepository.findCodeIdPairs());
        Map<String, Long> productIds = IdLookup.toMap(productRepository.findSkuIdPairs());
        if (customerIds.isEmpty() || productIds.isEmpty()) {
            throw new IllegalStateException("Seed customers and products before importing orders");
        }

        Map<String, Long> orderIds = persistOrders(csvFile, customerIds);
        int lines = persistLines(csvFile, orderIds, productIds);

        long millis = System.currentTimeMillis() - started;
        log.info("Imported {} orders and {} lines in {} ms", orderIds.size(), lines, millis);
        return new ImportResult(orderIds.size(), lines, millis);
    }

    private Map<String, Long> persistOrders(Path csvFile, Map<String, Long> customerIds) throws IOException {
        Map<String, Long> orderIds = HashMap.newHashMap(2_048);
        Set<String> seen = new HashSet<>();
        List<SalesOrder> chunk = new ArrayList<>(ChunkPersister.CHUNK_SIZE);

        try (Stream<OrderLineCsvRow> rows = CsvRowStream.stream(csvFile)) {
            rows.forEach(row -> {
                if (!seen.add(row.orderCode())) {
                    return;
                }
                Long customerId = customerIds.get(row.customerCode());
                if (customerId == null) {
                    throw new IllegalArgumentException(
                            "Line " + row.lineNumber() + ": unknown customer " + row.customerCode());
                }

                SalesOrder order = new SalesOrder();
                order.setCode(row.orderCode());
                order.setOrderedAt(row.orderedAt());
                order.setCustomer(entityManager.getReference(Customer.class, customerId));
                chunk.add(order);

                if (chunk.size() == ChunkPersister.CHUNK_SIZE) {
                    persistOrderChunk(chunk, orderIds);
                }
            });
        }
        persistOrderChunk(chunk, orderIds);
        return orderIds;
    }

    /**
     * persist() with SEQUENCE fills id immediately (no extra SELECT). Ids are
     * copied into the map, then flush/clear drops SalesOrder from the session.
     */
    private void persistOrderChunk(List<SalesOrder> chunk, Map<String, Long> orderIds) {
        if (chunk.isEmpty()) {
            return;
        }
        for (SalesOrder order : chunk) {
            entityManager.persist(order);
            orderIds.put(order.getCode(), order.getId());
        }
        entityManager.flush();
        entityManager.clear();
        chunk.clear();
    }

    private int persistLines(Path csvFile, Map<String, Long> orderIds, Map<String, Long> productIds) throws IOException {
        List<OrderLine> chunk = new ArrayList<>(ChunkPersister.CHUNK_SIZE);
        int[] count = {0};

        try (Stream<OrderLineCsvRow> rows = CsvRowStream.stream(csvFile)) {
            rows.forEach(row -> {
                Long orderId = orderIds.get(row.orderCode());
                Long productId = productIds.get(row.productSku());
                if (orderId == null) {
                    throw new IllegalArgumentException(
                            "Line " + row.lineNumber() + ": unknown order " + row.orderCode());
                }
                if (productId == null) {
                    throw new IllegalArgumentException(
                            "Line " + row.lineNumber() + ": unknown product " + row.productSku());
                }

                OrderLine line = new OrderLine();
                // getReference: proxy with id only. Works even if ManyToOne is EAGER,
                // because EAGER applies when loading OrderLine, not when setting FK.
                line.setOrder(entityManager.getReference(SalesOrder.class, orderId));
                line.setProduct(entityManager.getReference(Product.class, productId));
                line.setQuantity(row.quantity());
                line.setUnitPrice(row.unitPrice());
                chunk.add(line);
                count[0]++;

                if (chunk.size() == ChunkPersister.CHUNK_SIZE) {
                    chunkPersister.persistChunk(chunk);
                }
            });
        }
        chunkPersister.persistChunk(chunk);
        return count[0];
    }
}
