package com.custom.indexbasket.fix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FIX Adapter Service Application
 * 
 * This service provides FIX protocol integration with Bloomberg FixNet for market data publishing.
 * It handles FIX session management, message routing, and Bloomberg communication.
 */
@SpringBootApplication
@EnableScheduling
public class FixAdapterServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FixAdapterServiceApplication.class, args);
    }
}
