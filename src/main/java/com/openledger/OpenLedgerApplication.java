package com.openledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class for OpenLedger Backend.
 * Blockchain-based Financial Platform for Rwanda's Banking Sector.
 */
@SpringBootApplication
@EnableScheduling
public class OpenLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenLedgerApplication.class, args);
    }
}
