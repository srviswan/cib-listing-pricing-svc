package com.custom.indexbasket.marketdata.event;

import com.custom.indexbasket.common.messaging.EventPublisher;
import com.custom.indexbasket.marketdata.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Basket Event Consumer - Handles events from Basket Core Service
 * 
 * This component:
 * - Listens to basket lifecycle events
 * - Triggers market data updates
 * - Calculates basket metrics
 * - Publishes market data events
 * 
 * @author Custom Index Basket Team
 * @version 1.0.0
 */
@Component
public class BasketEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BasketEventConsumer.class);
    private final MarketDataService marketDataService;
    private final EventPublisher eventPublisher;

    public BasketEventConsumer(MarketDataService marketDataService, EventPublisher eventPublisher) {
        this.marketDataService = marketDataService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Handle basket created event
     */
    public Mono<Void> handleBasketCreated(String eventId, UUID basketId, String basketCode) {
        log.info("📥 Received BASKET_CREATED event: {} for basket: {}", eventId, basketCode);
        
        return marketDataService.initializeBasketMarketData(basketId, basketCode)
            .doOnSuccess(result -> log.info("✅ Basket market data initialized for: {}", basketCode))
            .doOnError(error -> log.error("❌ Failed to initialize basket market data for {}: {}", basketCode, error.getMessage()))
            .then();
    }

    /**
     * Handle basket updated event
     */
    public Mono<Void> handleBasketUpdated(String eventId, UUID basketId, String basketCode) {
        log.info("📥 Received BASKET_UPDATED event: {} for basket: {}", eventId, basketCode);
        
        return marketDataService.updateBasketMarketData(basketId, basketCode)
            .doOnSuccess(result -> log.info("✅ Basket market data updated for: {}", basketCode))
            .doOnError(error -> log.error("❌ Failed to update basket market data for {}: {}", basketCode, error.getMessage()))
            .then();
    }

    /**
     * Handle basket status changed event
     */
    public Mono<Void> handleBasketStatusChanged(String eventId, UUID basketId, String basketCode, String previousStatus, String newStatus) {
        log.info("📥 Received STATUS_CHANGED event: {} -> {} for basket: {}", previousStatus, newStatus, basketCode);
        
        return marketDataService.handleBasketStatusChange(basketId, basketCode, previousStatus, newStatus)
            .doOnSuccess(result -> log.info("✅ Basket status change handled for: {}", basketCode))
            .doOnError(error -> log.error("❌ Failed to handle basket status change for {}: {}", basketCode, error.getMessage()))
            .then();
    }

    /**
     * Handle basket deleted event
     */
    public Mono<Void> handleBasketDeleted(String eventId, UUID basketId, String basketCode) {
        log.info("📥 Received BASKET_DELETED event: {} for basket: {}", eventId, basketCode);
        
        return marketDataService.deactivateBasketMarketData(basketId, basketCode)
            .doOnSuccess(result -> log.info("✅ Basket market data deactivated for: {}", basketCode))
            .doOnError(error -> log.error("❌ Failed to deactivate basket market data for {}: {}", basketCode, error.getMessage()))
            .then();
    }
}
