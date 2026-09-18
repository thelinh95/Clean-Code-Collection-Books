package com.example.orderimport.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.orderimport.domain.SalesOrder;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
}
