package com.custom.indexbasket.fix.controller;

import com.custom.indexbasket.fix.model.FixSessionStatus;
import com.custom.indexbasket.fix.model.PricePublishingRequest;
import com.custom.indexbasket.fix.model.PublishingResult;
import com.custom.indexbasket.fix.service.FixMessageHandler;
import com.custom.indexbasket.fix.service.FixSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * FIX Controller
 * 
 * REST controller for FIX protocol operations.
 */
@RestController
@RequestMapping("/api/v1/fix")
@Slf4j
public class FixController {
    
    @Autowired
    private FixMessageHandler messageHandler;
    
    @Autowired
    private FixSessionManager sessionManager;
    
    /**
     * Publish price via FIX
     */
    @PostMapping("/publish/price")
    public Mono<ResponseEntity<PublishingResult>> publishPrice(@RequestBody PricePublishingRequest request) {
        log.debug("Publishing price via FIX for basket: {} symbol: {}", request.getBasketId(), request.getSymbol());
        
        return messageHandler.publishMarketData(request)
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
            .doOnSuccess(response -> {
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Successfully published price via FIX for basket: {}", request.getBasketId());
                } else {
                    log.warn("Failed to publish price via FIX for basket: {}", request.getBasketId());
                }
            });
    }
    
    /**
     * Publish basket listing via FIX
     */
    @PostMapping("/publish/basket")
    public Mono<ResponseEntity<PublishingResult>> publishBasketListing(@RequestBody PricePublishingRequest request) {
        log.debug("Publishing basket listing via FIX for basket: {}", request.getBasketId());
        
        // For now, treat basket listing the same as price publishing
        // In a real implementation, this would construct a different FIX message type
        return messageHandler.publishMarketData(request)
            .map(result -> ResponseEntity.ok(result.toBuilder()
                .message("Basket listing published via FIX")
                .build()))
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PublishingResult.failure(request.getBasketId(), request.getSymbol(), "ERROR", "Basket listing failed")))
            .doOnSuccess(response -> {
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Successfully published basket listing via FIX for basket: {}", request.getBasketId());
                } else {
                    log.warn("Failed to publish basket listing via FIX for basket: {}", request.getBasketId());
                }
            });
    }
    
    /**
     * Get active FIX sessions
     */
    @GetMapping("/sessions")
    public Mono<ResponseEntity<List<FixSessionStatus>>> getActiveSessions() {
        return sessionManager.getSessionStatus()
            .map(session -> List.of(session))
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build())
            .doOnSuccess(response -> log.debug("Retrieved FIX session status"));
    }
    
    /**
     * Reconnect FIX session
     */
    @PostMapping("/sessions/{id}/reconnect")
    public Mono<ResponseEntity<Map<String, Object>>> reconnectSession(@PathVariable String id) {
        log.info("Reconnecting FIX session: {}", id);
        
        // In a real implementation, this would trigger session reconnection
        Map<String, Object> responseData = new java.util.HashMap<>();
        responseData.put("sessionId", id);
        responseData.put("status", "RECONNECTING");
        responseData.put("timestamp", java.time.LocalDateTime.now());
        responseData.put("message", "Session reconnection initiated");
        return Mono.just(ResponseEntity.ok(responseData))
        .doOnSuccess(response -> log.info("FIX session reconnection initiated for: {}", id));
    }
    
    /**
     * Publish Bloomberg market data
     */
    @PostMapping("/bloomberg/market-data")
    public Mono<ResponseEntity<PublishingResult>> publishBloombergMarketData(@RequestBody PricePublishingRequest request) {
        log.debug("Publishing Bloomberg market data for basket: {}", request.getBasketId());
        
        return messageHandler.publishMarketData(request)
            .map(result -> ResponseEntity.ok(result.toBuilder()
                .message("Bloomberg market data published")
                .build()))
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PublishingResult.failure(request.getBasketId(), request.getSymbol(), "ERROR", "Bloomberg market data failed")))
            .doOnSuccess(response -> {
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Successfully published Bloomberg market data for basket: {}", request.getBasketId());
                } else {
                    log.warn("Failed to publish Bloomberg market data for basket: {}", request.getBasketId());
                }
            });
    }
    
    /**
     * Publish Bloomberg orders
     */
    @PostMapping("/bloomberg/orders")
    public Mono<ResponseEntity<Map<String, Object>>> publishBloombergOrders(@RequestBody Map<String, Object> orderRequest) {
        log.debug("Publishing Bloomberg orders");
        
        // Mock implementation - in production this would handle order management
        Map<String, Object> responseData = new java.util.HashMap<>();
        responseData.put("status", "SUCCESS");
        responseData.put("message", "Bloomberg orders published");
        responseData.put("timestamp", java.time.LocalDateTime.now());
        responseData.put("orderId", "ORDER_" + System.currentTimeMillis());
        return Mono.just(ResponseEntity.ok(responseData))
        .doOnSuccess(response -> log.info("Bloomberg orders published successfully"));
    }
    
    /**
     * Get Bloomberg connection status
     */
    @GetMapping("/bloomberg/status")
    public Mono<ResponseEntity<Map<String, Object>>> getBloombergStatus() {
        return sessionManager.getSessionStatus()
            .map(status -> {
                Map<String, Object> responseData = new java.util.HashMap<>();
                responseData.put("status", status.getStatus());
                responseData.put("connected", status.isConnected());
                responseData.put("loggedOn", status.isLoggedOn());
                responseData.put("healthy", status.isHealthy());
                responseData.put("uptimeSeconds", status.getUptimeSeconds());
                responseData.put("messagesSent", status.getMessagesSent());
                responseData.put("messagesReceived", status.getMessagesReceived());
                responseData.put("errors", status.getErrors());
                responseData.put("averageLatency", status.getAverageLatency());
                responseData.put("timestamp", java.time.LocalDateTime.now());
                return ResponseEntity.ok(responseData);
            })
            .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "DOWN", "error", "Failed to get Bloomberg status")))
            .doOnSuccess(response -> log.debug("Retrieved Bloomberg connection status"));
    }
    
    /**
     * Check FIX service health
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return sessionManager.getSessionStatus()
            .map(status -> {
                Map<String, Object> responseData = new java.util.HashMap<>();
                responseData.put("status", status.isHealthy() ? "UP" : "DOWN");
                responseData.put("connected", status.isConnected());
                responseData.put("loggedOn", status.isLoggedOn());
                responseData.put("sessionId", status.getSessionId());
                responseData.put("timestamp", java.time.LocalDateTime.now());
                responseData.put("uptimeSeconds", status.getUptimeSeconds());
                return ResponseEntity.ok(responseData);
            })
            .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "DOWN", "error", "Health check failed")))
            .doOnSuccess(response -> log.debug("FIX health check completed with status: {}", response.getStatusCode()));
    }
    
    /**
     * Get FIX performance metrics
     */
    @GetMapping("/metrics")
    public Mono<ResponseEntity<Map<String, Object>>> metrics() {
        return sessionManager.getSessionStatus()
            .map(status -> {
                Map<String, Object> responseData = new java.util.HashMap<>();
                responseData.put("sessionStatus", status);
                responseData.put("timestamp", java.time.LocalDateTime.now());
                responseData.put("healthy", status.isHealthy());
                return ResponseEntity.ok(responseData);
            })
            .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Metrics retrieval failed")));
    }
}
