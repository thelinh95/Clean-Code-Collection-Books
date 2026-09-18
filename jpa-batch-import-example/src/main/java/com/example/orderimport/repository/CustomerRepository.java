package com.example.orderimport.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.orderimport.domain.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("select c.code, c.id from Customer c")
    List<Object[]> findCodeIdPairs();
}
