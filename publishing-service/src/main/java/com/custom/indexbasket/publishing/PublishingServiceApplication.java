package com.custom.indexbasket.publishing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Publishing Service - Dual Publishing Engine
 * 
 * Handles basket listing and real-time price publishing through vendor integration frameworks.
 * Leverages extensive reuse of existing common component infrastructure.
 */
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class
})
@EnableConfigurationProperties
@EnableScheduling
@ComponentScan(basePackages = {
    "com.custom.indexbasket.publishing",
    "com.custom.indexbasket.common"
})
@EnableR2dbcRepositories
public class PublishingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PublishingServiceApplication.class, args);
    }
}
