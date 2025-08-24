package com.custom.indexbasket.basket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.statemachine.config.EnableStateMachineFactory;

/**
 * Basket Core Service Application
 * 
 * Core service for basket lifecycle management with:
 * - Spring Boot 3.x + WebFlux reactive foundation
 * - Hybrid communication protocol integration
 * - Akka Typed actor system for real-time state management
 * - R2DBC for reactive database access
 * - State machine integration with Spring State Machine (temporarily disabled)
 */
@SpringBootApplication(exclude = {
    RedisAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class,
    KafkaAutoConfiguration.class
})
@EnableR2dbcRepositories
public class BasketCoreServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(BasketCoreServiceApplication.class);

    public static void main(String[] args) {
        log.info("🚀 Starting Basket Core Service...");
        ApplicationContext context = SpringApplication.run(BasketCoreServiceApplication.class, args);
        log.info("🚀 Basket Core Service started successfully!");
        
        String[] activeProfiles = context.getEnvironment().getActiveProfiles();
        if (activeProfiles.length == 0) {
            log.info("🚀 No active profiles");
        } else {
            log.info("🚀 Active profiles: {}", (Object) activeProfiles);
        }
        
        // Log performance and monitoring beans
        String[] connectionFactoryBeans = context.getBeanNamesForType(org.springframework.r2dbc.core.DatabaseClient.class);
        log.info("🚀 DatabaseClient beans found: {}", (Object) connectionFactoryBeans);
        String[] entityTemplateBeans = context.getBeanNamesForType(org.springframework.data.r2dbc.core.R2dbcEntityTemplate.class);
        log.info("🚀 R2dbcEntityTemplate beans found: {}", (Object) entityTemplateBeans);
        
        // Log state machine beans
        try {
            String[] stateMachineBeans = context.getBeanNamesForType(org.springframework.statemachine.StateMachine.class);
            log.info("🚀 StateMachine beans found: {}", (Object) stateMachineBeans);
        } catch (Exception e) {
            log.warn("🚀 Could not find StateMachine beans: {}", e.getMessage());
        }
        
        // Log monitoring beans
        try {
            String[] monitoringBeans = context.getBeanNamesForType(io.micrometer.core.instrument.MeterRegistry.class);
            log.info("🚀 MeterRegistry beans found: {}", (Object) monitoringBeans);
        } catch (Exception e) {
            log.warn("🚀 Could not find MeterRegistry beans: {}", e.getMessage());
        }
    }
}
