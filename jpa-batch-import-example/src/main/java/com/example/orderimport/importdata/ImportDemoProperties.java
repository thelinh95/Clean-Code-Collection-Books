package com.example.orderimport.importdata;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "import.demo")
public record ImportDemoProperties(
        boolean enabled,
        int lines,
        int customers,
        int products,
        String csvPath
) {
}
