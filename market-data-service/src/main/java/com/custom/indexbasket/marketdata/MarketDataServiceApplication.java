package com.custom.indexbasket.marketdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Market Data Service - Real-time market data, pricing, and analytics service
 * 
 * This service provides:
 * - Real-time market data for basket constituents
 * - Live pricing calculations and market values
 * - Market analytics and risk metrics
 * - gRPC endpoints for high-performance communication
 * - Event-driven architecture for real-time updates
 * 
 * @author Custom Index Basket Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages = {
    "com.custom.indexbasket.marketdata",
    "com.custom.indexbasket.common"
})
@EnableR2dbcRepositories
public class MarketDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketDataServiceApplication.class, args);
    }
}
