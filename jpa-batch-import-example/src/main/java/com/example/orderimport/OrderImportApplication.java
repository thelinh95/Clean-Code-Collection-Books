package com.example.orderimport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.orderimport.importdata.ImportDemoProperties;

@SpringBootApplication
@EnableConfigurationProperties(ImportDemoProperties.class)
public class OrderImportApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderImportApplication.class, args);
    }
}
