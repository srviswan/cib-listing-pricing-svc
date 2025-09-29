package com.custom.indexbasket.integration.service;

import com.custom.indexbasket.integration.model.IntegrationStatus;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Integration Health Service
 * 
 * Monitors health of all integrated services and provides status information.
 */
@Service
@Slf4j
public class IntegrationHealthService {
    
    @Autowired
    private WebClient smaWebClient;
    
    @Autowired
    private WebClient fixWebClient;
    
    @Autowired
    private WebClient basketCoreWebClient;
    
    @Autowired
    private WebClient marketDataWebClient;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    // Health tracking
    private final AtomicInteger successfulOperations = new AtomicInteger(0);
    private final AtomicInteger failedOperations = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    
    /**
     * Check overall integration health
     */
    public Mono<IntegrationStatus> checkIntegrationHealth() {
        return Mono.zip(
            checkSmaAdapterHealth(),
            checkFixAdapterHealth(),
            checkBasketCoreHealth(),
            checkMarketDataHealth()
        )
        .map(tuple -> {
            IntegrationStatus.ServiceStatus smaStatus = tuple.getT1();
            IntegrationStatus.ServiceStatus fixStatus = tuple.getT2();
            IntegrationStatus.ServiceStatus basketCoreStatus = tuple.getT3();
            IntegrationStatus.ServiceStatus marketDataStatus = tuple.getT4();
            
            boolean allHealthy = smaStatus.isHealthy() && fixStatus.isHealthy() && 
                               basketCoreStatus.isHealthy() && marketDataStatus.isHealthy();
            
            double avgProcessingTime = successfulOperations.get() > 0 ? 
                (double) totalProcessingTime.get() / successfulOperations.get() : 0.0;
            
            return IntegrationStatus.builder()
                .status(allHealthy ? "UP" : "DOWN")
                .healthy(allHealthy)
                .timestamp(LocalDateTime.now())
                .smaAdapter(smaStatus)
                .fixAdapter(fixStatus)
                .basketCore(basketCoreStatus)
                .marketData(marketDataStatus)
                .activeBaskets(0) // Would get from basket core
                .totalBaskets(0) // Would get from basket core
                .averageProcessingTime(avgProcessingTime)
                .successfulOperations(successfulOperations.get())
                .failedOperations(failedOperations.get())
                .metrics(Map.of(
                    "uptime", getUptime(),
                    "lastHealthCheck", LocalDateTime.now().toString()
                ))
                .build();
        })
        .doOnSuccess(status -> {
            if (status.isHealthy()) {
                meterRegistry.gauge("integration.health.status", 1);
            } else {
                meterRegistry.gauge("integration.health.status", 0);
            }
        })
        .doOnError(error -> {
            meterRegistry.gauge("integration.health.status", 0);
            log.error("Health check failed: {}", error.getMessage());
        });
    }
    
    /**
     * Check SMA adapter health
     */
    private Mono<IntegrationStatus.ServiceStatus> checkSmaAdapterHealth() {
        long startTime = System.currentTimeMillis();
        
        return smaWebClient
            .get()
            .uri("/api/v1/sma/health")
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(Duration.ofSeconds(5))
            .map(response -> {
                long responseTime = System.currentTimeMillis() - startTime;
                String status = (String) response.get("status");
                
                return IntegrationStatus.ServiceStatus.builder()
                    .name("SMA_ADAPTER")
                    .status(status)
                    .healthy("UP".equals(status))
                    .url("http://localhost:8084/sma-adapter")
                    .responseTime(responseTime)
                    .successCount(1)
                    .errorCount(0)
                    .lastChecked(LocalDateTime.now())
                    .build();
            })
            .onErrorReturn(IntegrationStatus.ServiceStatus.builder()
                .name("SMA_ADAPTER")
                .status("DOWN")
                .healthy(false)
                .url("http://localhost:8084/sma-adapter")
                .responseTime(System.currentTimeMillis() - startTime)
                .successCount(0)
                .errorCount(1)
                .lastChecked(LocalDateTime.now())
                .lastError("Health check failed")
                .build())
            .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Check FIX adapter health
     */
    private Mono<IntegrationStatus.ServiceStatus> checkFixAdapterHealth() {
        long startTime = System.currentTimeMillis();
        
        return fixWebClient
            .get()
            .uri("/api/v1/fix/health")
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(Duration.ofSeconds(5))
            .map(response -> {
                long responseTime = System.currentTimeMillis() - startTime;
                String status = (String) response.get("status");
                
                return IntegrationStatus.ServiceStatus.builder()
                    .name("FIX_ADAPTER")
                    .status(status)
                    .healthy("UP".equals(status))
                    .url("http://localhost:8085/fix-adapter")
                    .responseTime(responseTime)
                    .successCount(1)
                    .errorCount(0)
                    .lastChecked(LocalDateTime.now())
                    .build();
            })
            .onErrorReturn(IntegrationStatus.ServiceStatus.builder()
                .name("FIX_ADAPTER")
                .status("DOWN")
                .healthy(false)
                .url("http://localhost:8085/fix-adapter")
                .responseTime(System.currentTimeMillis() - startTime)
                .successCount(0)
                .errorCount(1)
                .lastChecked(LocalDateTime.now())
                .lastError("Health check failed")
                .build())
            .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Check Basket Core health
     */
    private Mono<IntegrationStatus.ServiceStatus> checkBasketCoreHealth() {
        long startTime = System.currentTimeMillis();
        
        return basketCoreWebClient
            .get()
            .uri("/actuator/health")
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(Duration.ofSeconds(5))
            .map(response -> {
                long responseTime = System.currentTimeMillis() - startTime;
                String status = (String) response.get("status");
                
                return IntegrationStatus.ServiceStatus.builder()
                    .name("BASKET_CORE")
                    .status(status)
                    .healthy("UP".equals(status))
                    .url("http://localhost:8081")
                    .responseTime(responseTime)
                    .successCount(1)
                    .errorCount(0)
                    .lastChecked(LocalDateTime.now())
                    .build();
            })
            .onErrorReturn(IntegrationStatus.ServiceStatus.builder()
                .name("BASKET_CORE")
                .status("DOWN")
                .healthy(false)
                .url("http://localhost:8081")
                .responseTime(System.currentTimeMillis() - startTime)
                .successCount(0)
                .errorCount(1)
                .lastChecked(LocalDateTime.now())
                .lastError("Health check failed")
                .build())
            .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Check Market Data health
     */
    private Mono<IntegrationStatus.ServiceStatus> checkMarketDataHealth() {
        long startTime = System.currentTimeMillis();
        
        return marketDataWebClient
            .get()
            .uri("/actuator/health")
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(Duration.ofSeconds(5))
            .map(response -> {
                long responseTime = System.currentTimeMillis() - startTime;
                String status = (String) response.get("status");
                
                return IntegrationStatus.ServiceStatus.builder()
                    .name("MARKET_DATA")
                    .status(status)
                    .healthy("UP".equals(status))
                    .url("http://localhost:8082")
                    .responseTime(responseTime)
                    .successCount(1)
                    .errorCount(0)
                    .lastChecked(LocalDateTime.now())
                    .build();
            })
            .onErrorReturn(IntegrationStatus.ServiceStatus.builder()
                .name("MARKET_DATA")
                .status("DOWN")
                .healthy(false)
                .url("http://localhost:8082")
                .responseTime(System.currentTimeMillis() - startTime)
                .successCount(0)
                .errorCount(1)
                .lastChecked(LocalDateTime.now())
                .lastError("Health check failed")
                .build())
            .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Record successful operation
     */
    public void recordSuccessfulOperation(long processingTime) {
        successfulOperations.incrementAndGet();
        totalProcessingTime.addAndGet(processingTime);
        meterRegistry.counter("integration.operations.success").increment();
    }
    
    /**
     * Record failed operation
     */
    public void recordFailedOperation() {
        failedOperations.incrementAndGet();
        meterRegistry.counter("integration.operations.failed").increment();
    }
    
    /**
     * Get service uptime
     */
    private long getUptime() {
        // In a real implementation, this would track actual uptime
        return System.currentTimeMillis();
    }
    
    /**
     * Get service status by name
     */
    public Mono<IntegrationStatus.ServiceStatus> getServiceStatus(String serviceName) {
        return checkIntegrationHealth()
            .map(status -> status.getServiceStatus(serviceName));
    }
}
