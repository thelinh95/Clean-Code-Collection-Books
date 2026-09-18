package com.example.orderimport.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.orderimport.domain.OrderLine;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
}
