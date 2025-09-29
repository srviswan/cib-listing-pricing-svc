package com.custom.indexbasket.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Integration Manager Service Application
 * 
 * This service orchestrates data flow between SMA Refinitiv Adapter and FIX Bloomberg Adapter.
 * It manages basket price calculations, real-time publishing, and integration coordination.
 */
@SpringBootApplication
@EnableScheduling
public class IntegrationManagerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationManagerServiceApplication.class, args);
    }
}
