package com.example.orderimport.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.orderimport.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("select p.sku, p.id from Product p")
    List<Object[]> findSkuIdPairs();
}
