package com.example.orderimport.importdata;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "import.demo.enabled", havingValue = "true")
public class ImportDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportDemoRunner.class);

    private final ImportDemoProperties properties;
    private final MasterDataSeeder seeder;
    private final DemoCsvGenerator csvGenerator;
    private final OrderImportService importService;

    public ImportDemoRunner(
            ImportDemoProperties properties,
            MasterDataSeeder seeder,
            DemoCsvGenerator csvGenerator,
            OrderImportService importService
    ) {
        this.properties = properties;
        this.seeder = seeder;
        this.csvGenerator = csvGenerator;
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int lines = properties.lines();
        int customers = properties.customers();
        int products = properties.products();
        Path csv = Path.of(properties.csvPath());

        log.info("Seeding {} customers and {} products", customers, products);
        seeder.seed(customers, products);

        log.info("Writing {} CSV lines to {}", lines, csv);
        csvGenerator.write(csv, lines, customers, products);

        ImportResult result = importService.importFromCsv(csv);
        log.info(
                "Done. orders={}, lines={}, expectedOrders={}, elapsedMs={}",
                result.ordersInserted(),
                result.linesInserted(),
                DemoCsvGenerator.expectedOrderCount(lines),
                result.millis()
        );
    }
}
