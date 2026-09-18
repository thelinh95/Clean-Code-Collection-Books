package com.example.orderimport.importdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.orderimport.repository.CustomerRepository;
import com.example.orderimport.repository.OrderLineRepository;
import com.example.orderimport.repository.ProductRepository;
import com.example.orderimport.repository.SalesOrderRepository;

import jakarta.persistence.EntityManagerFactory;

@SpringBootTest
class OrderImportServiceTest {

    private static final int LINES = 10_000;
    private static final int CUSTOMERS = 100;
    private static final int PRODUCTS = 100;

    @Autowired
    MasterDataSeeder seeder;

    @Autowired
    DemoCsvGenerator csvGenerator;

    @Autowired
    OrderImportService importService;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    SalesOrderRepository salesOrderRepository;

    @Autowired
    OrderLineRepository orderLineRepository;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void importsTenThousandLinesWithoutKeepingFullPersistenceContext(@TempDir Path tempDir) throws Exception {
        seeder.seed(CUSTOMERS, PRODUCTS);
        Path csv = csvGenerator.write(tempDir.resolve("orders.csv"), LINES, CUSTOMERS, PRODUCTS);

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        ImportResult result = importService.importFromCsv(csv);

        int expectedOrders = DemoCsvGenerator.expectedOrderCount(LINES);
        assertThat(result.linesInserted()).isEqualTo(LINES);
        assertThat(result.ordersInserted()).isEqualTo(expectedOrders);
        assertThat(customerRepository.count()).isEqualTo(CUSTOMERS);
        assertThat(productRepository.count()).isEqualTo(PRODUCTS);
        assertThat(salesOrderRepository.count()).isEqualTo(expectedOrders);
        assertThat(orderLineRepository.count()).isEqualTo(LINES);

        assertThat(stats.getEntityInsertCount())
                .as("only orders + lines are inserted during import")
                .isEqualTo((long) expectedOrders + LINES);
        assertThat(stats.getPrepareStatementCount())
                .as("JDBC batching must send far fewer statements than row inserts")
                .isLessThan(stats.getEntityInsertCount() / 5);
    }
}
