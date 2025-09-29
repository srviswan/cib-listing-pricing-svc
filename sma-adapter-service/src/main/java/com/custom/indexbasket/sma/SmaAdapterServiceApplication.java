package com.custom.indexbasket.sma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SMA Refinitiv Adapter Service Application
 * 
 * This service provides integration with Refinitiv SMA API for real-time market data.
 * It acts as a bridge between the existing Market Data Service and Refinitiv's data sources.
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class SmaAdapterServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmaAdapterServiceApplication.class, args);
    }
}
