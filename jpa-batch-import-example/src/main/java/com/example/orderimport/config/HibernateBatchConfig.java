package com.example.orderimport.config;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.orderimport.importdata.ChunkPersister;

@Configuration
public class HibernateBatchConfig {

    @Bean
    HibernatePropertiesCustomizer hibernateBatchProperties() {
        return props -> {
            props.put("hibernate.jdbc.batch_size", ChunkPersister.JDBC_BATCH_SIZE);
            props.put("hibernate.jdbc.batch_versioned_data", true);
            props.put("hibernate.order_inserts", true);
            props.put("hibernate.order_updates", true);
        };
    }
}
