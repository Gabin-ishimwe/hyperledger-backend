package com.openledger.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class for OpenLedger API.
 */
@SpringBootApplication(scanBasePackages = "com.openledger")
@EnableScheduling
public class OpenLedgerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenLedgerApiApplication.class, args);
    }
}
