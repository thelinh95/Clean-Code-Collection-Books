package com.example.orderimport.importdata;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.orderimport.domain.Customer;
import com.example.orderimport.domain.Product;

@Service
public class MasterDataSeeder {

    private final ChunkPersister chunkPersister;

    public MasterDataSeeder(ChunkPersister chunkPersister) {
        this.chunkPersister = chunkPersister;
    }

    @Transactional
    public void seed(int customerCount, int productCount) {
        List<Customer> customers = new ArrayList<>(ChunkPersister.CHUNK_SIZE);
        for (int i = 1; i <= customerCount; i++) {
            Customer customer = new Customer();
            customer.setCode(code("CUST", i));
            customer.setName("Customer " + i);
            customers.add(customer);
            if (customers.size() == ChunkPersister.CHUNK_SIZE) {
                chunkPersister.persistChunk(customers);
            }
        }
        chunkPersister.persistChunk(customers);

        List<Product> products = new ArrayList<>(ChunkPersister.CHUNK_SIZE);
        for (int i = 1; i <= productCount; i++) {
            Product product = new Product();
            product.setSku(code("SKU", i));
            product.setName("Product " + i);
            products.add(product);
            if (products.size() == ChunkPersister.CHUNK_SIZE) {
                chunkPersister.persistChunk(products);
            }
        }
        chunkPersister.persistChunk(products);
    }

    static String code(String prefix, int index) {
        return "%s-%03d".formatted(prefix, index);
    }
}
