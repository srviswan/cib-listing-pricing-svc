package com.custom.indexbasket.basket.service;

import com.custom.indexbasket.basket.config.MonitoringConfig.BasketMetrics;
import com.custom.indexbasket.basket.domain.BasketEntity;
import com.custom.indexbasket.basket.domain.BasketConstituentEntity;
import com.custom.indexbasket.basket.dto.*;
import com.custom.indexbasket.basket.repository.BasketRepository;
import com.custom.indexbasket.basket.repository.BasketConstituentRepository;
import com.custom.indexbasket.common.model.Basket;
import com.custom.indexbasket.common.model.BasketConstituent;
import com.custom.indexbasket.common.model.BasketStatus;
import com.custom.indexbasket.basket.config.StateMachineConfig;
import com.custom.indexbasket.basket.event.BasketEvent;
import com.custom.indexbasket.basket.event.EventPublisher;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Business logic layer for basket operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BasketService {

    private final BasketRepository basketRepository;
    private final BasketConstituentRepository constituentRepository;
    private final BasketMetrics basketMetrics;
    private final StateMachineConfig stateMachineConfig;
    private final EventPublisher eventPublisher;

    /**
     * Create a new basket
     */
    public Mono<UUID> createBasket(CreateBasketRequest request) {
        log.info("Creating basket with code: {}", request.basketCode());
        
        Timer.Sample timer = basketMetrics.startCreateBasketTimer();
        
        BasketEntity basket = new BasketEntity();
        basket.setBasketCode(request.basketCode());
        basket.setBasketName(request.basketName());
        basket.setDescription(request.description());
        basket.setBasketType(request.basketType());
        basket.setBaseCurrency(request.baseCurrency());
        // Don't set totalWeight - let database use default 100.00
        // Don't set status - let database use default 'DRAFT'
        // Don't set version - let database use default 'v1.0'
        basket.setCreatedBy(request.createdBy());
        // Don't set timestamps - let database use defaults
        // Don't set entityVersion - let database use default 1
        // Don't set isActive - let database use default true
        
        return basketRepository.save(basket)
            .map(savedBasket -> {
                log.info("Basket created successfully with ID: {}", savedBasket.getId());
                
                // Publish event to downstream services
                BasketEvent event = BasketEvent.createEvent(BasketEvent.EventType.BASKET_CREATED, savedBasket.getId(), savedBasket.getBasketCode())
                    .withBasketName(savedBasket.getBasketName())
                    .withDescription(savedBasket.getDescription())
                    .withBasketType(savedBasket.getBasketType())
                    .withBaseCurrency(savedBasket.getBaseCurrency())
                    .withTotalWeight(savedBasket.getTotalWeight())
                    .withNewStatus(savedBasket.getStatus())
                    .withTriggeredBy(savedBasket.getCreatedBy());
                
                eventPublisher.publishEvent(event)
                    .doOnSuccess(result -> log.info("📤 BASKET_CREATED event published for basket: {}", savedBasket.getBasketCode()))
                    .doOnError(throwable -> log.error("❌ Failed to publish BASKET_CREATED event: {}", throwable.getMessage()))
                    .subscribe();
                
                return savedBasket.getId();
            })
            .doFinally(signalType -> {
                timer.stop(basketMetrics.createBasketTimer);
                log.info("Basket creation completed with signal: {}", signalType);
            })
            .onErrorMap(error -> {
                log.error("Error creating basket: {}", error.getMessage(), error);
                return error;
            });
    }

    /**
     * Get basket by ID
     */
    public Mono<BasketEntity> getBasketById(UUID basketId) {
        log.info("Retrieving basket with ID: {}", basketId);
        
        Timer.Sample timer = basketMetrics.startReadBasketTimer();
        
        return basketRepository.findById(basketId)
            .doFinally(signalType -> {
                timer.stop(basketMetrics.readBasketTimer);
                log.info("Basket retrieval completed with signal: {}", signalType);
            })
            .onErrorMap(error -> {
                log.error("Error retrieving basket: {}", error.getMessage(), error);
                return error;
            });
    }

    /**
     * Get all baskets with pagination and filtering
     */
    public Mono<PaginatedBasketsResponse> getAllBaskets(Pageable pageable, BasketStatus status, String basketType) {
        log.info("Retrieving baskets - page: {}, size: {}, status: {}, type: {}", 
                pageable.getPageNumber(), pageable.getPageSize(), status, basketType);
        
        Timer.Sample timer = basketMetrics.startListBasketsTimer();
        
        Flux<BasketEntity> basketsQuery;
        Mono<Long> countQuery;
        
        // Apply filters based on parameters
        if (status != null && basketType != null) {
            // Need to implement combined filter - for now use status filter
            basketsQuery = basketRepository.findByStatus(status);
            countQuery = basketRepository.countByStatus(status);
        } else if (status != null) {
            basketsQuery = basketRepository.findByStatus(status);
            countQuery = basketRepository.countByStatus(status);
        } else if (basketType != null) {
            basketsQuery = basketRepository.findByBasketType(basketType);
            countQuery = basketRepository.countActive(); // Approximate count
        } else {
            basketsQuery = basketRepository.findAllActive();
            countQuery = basketRepository.countActive();
        }
        
        // Apply pagination
        basketsQuery = basketsQuery
                .skip(pageable.getPageNumber() * pageable.getPageSize())
                .take(pageable.getPageSize());
        
        return basketsQuery
                .collectList()
                .zipWith(countQuery)
                .map(tuple -> {
                    List<BasketEntity> baskets = tuple.getT1();
                    long totalElements = tuple.getT2();
                    int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());
                    
                    return new PaginatedBasketsResponse(
                            baskets, 
                            pageable.getPageNumber(), 
                            pageable.getPageSize(), 
                            totalElements, 
                            totalPages
                    );
                })
                .doFinally(signalType -> {
                    timer.stop(basketMetrics.listBasketsTimer);
                    log.info("Basket listing completed with signal: {}", signalType);
                });
    }

    /**
     * Update an existing basket
     */
    public Mono<BasketEntity> updateBasket(UUID basketId, UpdateBasketRequest request) {
        log.info("Updating basket with ID: {}", basketId);
        
        Timer.Sample timer = basketMetrics.startUpdateBasketTimer();
        
        return basketRepository.findById(basketId)
            .switchIfEmpty(Mono.error(new RuntimeException("Basket not found with ID: " + basketId)))
            .flatMap(existingBasket -> {
                // Calculate new entity version
                Long newEntityVersion = existingBasket.getEntityVersion() + 1;
                LocalDateTime updatedAt = LocalDateTime.now();
                
                log.info("Updating basket fields. New entity version: {}", newEntityVersion);
                
                // Use custom update query that only updates provided fields
                return basketRepository.updateBasketFields(
                        basketId,
                        request.basketName(),
                        request.description(),
                        request.basketType(),
                        request.baseCurrency(),
                        request.totalWeight(),
                        request.status(),
                        request.version(),
                        request.updatedBy(),
                        LocalDateTime.now(),
                        existingBasket.getEntityVersion() + 1
                    )
                    .then(Mono.defer(() -> {
                        // After successful update, fetch the updated basket for event publishing
                        return basketRepository.findById(basketId);
                    }))
                    .flatMap(updatedBasket -> {
                        log.info("Basket updated successfully with ID: {}", basketId);
                        
                        // Publish event to downstream services
                        BasketEvent event = BasketEvent.createEvent(BasketEvent.EventType.BASKET_UPDATED, basketId, updatedBasket.getBasketCode())
                            .withBasketName(updatedBasket.getBasketName())
                            .withDescription(updatedBasket.getDescription())
                            .withBasketType(updatedBasket.getBasketType())
                            .withBaseCurrency(updatedBasket.getBaseCurrency())
                            .withTotalWeight(updatedBasket.getTotalWeight())
                            .withNewStatus(updatedBasket.getStatus())
                            .withTriggeredBy(request.updatedBy());
                        
                        eventPublisher.publishEvent(event)
                            .doOnSuccess(result -> log.info("📤 BASKET_UPDATED event published for basket: {}", updatedBasket.getBasketCode()))
                            .doOnError(throwable -> log.error("❌ Failed to publish BASKET_UPDATED event: {}", throwable.getMessage()))
                            .subscribe();
                        
                        return Mono.just(updatedBasket);
                    });
            })
            .doFinally(signalType -> {
                timer.stop(basketMetrics.updateBasketTimer);
                log.info("Basket update completed with signal: {}", signalType);
            })
            .onErrorMap(error -> {
                log.error("Error updating basket: {}", error.getMessage(), error);
                return error;
            });
    }

    /**
     * Update basket status using simplified state machine workflow
     */
    public Mono<Void> updateBasketStatus(UUID basketId, String event, String updatedBy) {
        log.info("Updating basket status with ID: {} using event: {}", basketId, event);
        
        return basketRepository.findById(basketId)
            .switchIfEmpty(Mono.error(new RuntimeException("Basket not found with ID: " + basketId)))
            .flatMap(existingBasket -> {
                // Use simplified state machine logic
                BasketStatus newStatus = stateMachineConfig.determineNewStatus(existingBasket.getStatus(), event);
                
                if (newStatus == null) {
                    return Mono.error(new RuntimeException("Invalid state transition: " + event + " from " + existingBasket.getStatus()));
                }
                
                log.info("Transitioning basket from {} to {} via event: {}", existingBasket.getStatus(), newStatus, event);
                
                // Update basket with new status
                return basketRepository.updateBasketFields(
                        basketId,
                        null, // basketName
                        null, // description
                        null, // basketType
                        null, // baseCurrency
                        null, // totalWeight
                        newStatus, // status
                        null, // version
                        updatedBy, // updatedBy
                        LocalDateTime.now(), // updatedAt
                        existingBasket.getEntityVersion() + 1 // entityVersion
                )
                .then(Mono.defer(() -> {
                    // Publish event to downstream services
                    BasketEvent statusEvent = BasketEvent.createEvent(BasketEvent.EventType.STATUS_CHANGED, basketId, existingBasket.getBasketCode())
                        .withPreviousStatus(existingBasket.getStatus())
                        .withNewStatus(newStatus)
                        .withEventTrigger(event)
                        .withTriggeredBy(updatedBy)
                        .withBasketName(existingBasket.getBasketName())
                        .withBasketType(existingBasket.getBasketType())
                        .withBaseCurrency(existingBasket.getBaseCurrency())
                        .withTotalWeight(existingBasket.getTotalWeight());
                    
                    eventPublisher.publishEvent(statusEvent)
                        .doOnSuccess(result -> log.info("📤 STATUS_CHANGED event published: {} -> {} for basket: {}", 
                            existingBasket.getStatus(), newStatus, existingBasket.getBasketCode()))
                        .doOnError(throwable -> log.error("❌ Failed to publish STATUS_CHANGED event: {}", throwable.getMessage()))
                        .subscribe();
                    
                    return Mono.empty();
                }))
                .then();
            })
            .onErrorMap(error -> {
                log.error("Error updating basket status: {}", error.getMessage(), error);
                return error;
            });
    }

    /**
     * Delete a basket (soft delete)
     */
    public Mono<Void> deleteBasket(UUID basketId, String deletedBy) {
        log.info("Soft deleting basket with ID: {}", basketId);
        
        Timer.Sample timer = basketMetrics.startDeleteBasketTimer();
        
        return basketRepository.findById(basketId)
            .switchIfEmpty(Mono.error(new RuntimeException("Basket not found with ID: " + basketId)))
            .flatMap(existingBasket -> {
                log.info("Soft deleting basket with ID: {}", basketId);
                
                return basketRepository.softDeleteBasket(basketId, deletedBy, LocalDateTime.now(), existingBasket.getEntityVersion() + 1)
                    .then(Mono.defer(() -> {
                        // Publish event to downstream services
                        BasketEvent event = BasketEvent.createEvent(BasketEvent.EventType.BASKET_DELETED, basketId, existingBasket.getBasketCode())
                            .withBasketName(existingBasket.getBasketName())
                            .withDescription(existingBasket.getDescription())
                            .withBasketType(existingBasket.getBasketType())
                            .withBaseCurrency(existingBasket.getBaseCurrency())
                            .withTotalWeight(existingBasket.getTotalWeight())
                            .withNewStatus(existingBasket.getStatus())
                            .withTriggeredBy(deletedBy);
                        
                        eventPublisher.publishEvent(event)
                            .doOnSuccess(result -> log.info("📤 BASKET_DELETED event published for basket: {}", existingBasket.getBasketCode()))
                            .doOnError(throwable -> log.error("❌ Failed to publish BASKET_DELETED event: {}", throwable.getMessage()))
                            .subscribe();
                        
                        return Mono.empty();
                    }))
                    .then();
            })
            .doFinally(signalType -> {
                timer.stop(basketMetrics.deleteBasketTimer);
                log.info("Basket deletion completed with signal: {}", signalType);
            })
            .onErrorMap(error -> {
                log.error("Error deleting basket: {}", error.getMessage(), error);
                return error;
            });
    }
    
    /**
     * Get event publishing status and statistics
     */
    public Mono<EventPublisher.PublishingStats> getEventPublishingStatus() {
        log.info("Retrieving event publishing status");
        return eventPublisher.getPublishingStats()
            .onErrorMap(error -> {
                log.error("Error retrieving event publishing status: {}", error.getMessage(), error);
                return error;
            });
    }

    /**
     * Add constituent to basket
     */
    public Mono<BasketConstituentEntity> addConstituent(UUID basketId, AddConstituentRequest request) {
        log.info("Adding constituent to basket - ID: {}, symbol: {}", basketId, request.symbol());
        
        BasketConstituentEntity constituent = new BasketConstituentEntity();
        constituent.setBasketId(basketId);
        constituent.setBasketCode(basketId.toString()); // This should be the actual basket code
        constituent.setSymbol(request.symbol());
        constituent.setSymbolName(request.symbolName());
        constituent.setWeight(request.weight());
        constituent.setShares(request.shares());
        constituent.setTargetAllocation(request.targetAllocation());
        constituent.setSector(request.sector());
        constituent.setCountry(request.country());
        constituent.setCurrency(request.currency());
        constituent.setCreatedAt(LocalDateTime.now());
        constituent.setUpdatedAt(LocalDateTime.now());
        constituent.setIsActive(true);
        
        return constituentRepository.save(constituent)
                .doOnSuccess(saved -> log.info("Constituent added successfully with ID: {}", saved.getEntityId()));
    }

    /**
     * Update constituent weight
     */
    public Mono<Void> updateConstituentWeight(UUID basketId, String symbol, BigDecimal newWeight, String updatedBy) {
        log.info("Updating constituent weight - basket: {}, symbol: {}, new weight: {}", 
                basketId, symbol, newWeight);
        
        return constituentRepository.findByBasketIdAndSymbol(basketId, symbol)
                .flatMap(constituent -> {
                    constituent.setWeight(newWeight);
                    constituent.setUpdatedBy(updatedBy);
                    constituent.setUpdatedAt(LocalDateTime.now());
                    return constituentRepository.save(constituent);
                })
                .then();
    }

    /**
     * Remove constituent from basket
     */
    public Mono<Void> removeConstituent(UUID basketId, String symbol) {
        log.info("Removing constituent from basket - ID: {}, symbol: {}", basketId, symbol);
        
        return constituentRepository.findByBasketIdAndSymbol(basketId, symbol)
                .flatMap(constituent -> {
                    constituent.setIsActive(false);
                    constituent.setUpdatedAt(LocalDateTime.now());
                    return constituentRepository.save(constituent);
                })
                .then();
    }

    /**
     * Rebalance basket
     */
    public Mono<BasketEntity> rebalanceBasket(UUID basketId, List<CreateConstituentRequest> newConstituents, String updatedBy) {
        log.info("Rebalancing basket - ID: {}, constituents count: {}", basketId, newConstituents.size());
        
        return basketRepository.findById(basketId)
                .flatMap(basket -> {
                    // Deactivate existing constituents
                    return constituentRepository.findByBasketId(basketId)
                            .flatMap(constituent -> {
                                constituent.setIsActive(false);
                                constituent.setUpdatedAt(LocalDateTime.now());
                                return constituentRepository.save(constituent);
                            })
                            .collectList()
                            .then(Mono.just(basket));
                })
                .flatMap(basket -> {
                    // Add new constituents
                    return Flux.fromIterable(newConstituents)
                            .flatMap(request -> {
                                // Convert CreateConstituentRequest to AddConstituentRequest
                                AddConstituentRequest addRequest = new AddConstituentRequest(
                                    request.symbol(),
                                    request.symbolName(),
                                    request.weight(),
                                    request.shares(),
                                    request.targetAllocation(),
                                    request.sector(),
                                    request.country(),
                                    request.currency()
                                );
                                return addConstituent(basketId, addRequest);
                            })
                            .collectList()
                            .then(Mono.just(basket));
                })
                .flatMap(basket -> {
                    basket.setLastRebalanceDate(LocalDateTime.now());
                    basket.setUpdatedBy(updatedBy);
                    basket.setUpdatedAt(LocalDateTime.now());
                    return basketRepository.save(basket);
                });
    }

    /**
     * Get basket constituents
     */
    public Mono<List<BasketConstituentEntity>> getBasketConstituents(UUID basketId) {
        log.info("Retrieving constituents for basket: {}", basketId);
        return constituentRepository.findByBasketId(basketId).collectList();
    }

    /**
     * Get basket analytics
     */
    public Mono<BasketAnalyticsResponse> getBasketAnalytics(UUID basketId) {
        log.info("Retrieving analytics for basket: {}", basketId);
        
        // This is a placeholder implementation
        return Mono.just(new BasketAnalyticsResponse(
                basketId.toString(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                LocalDateTime.now()
        ));
    }

    /**
     * Get sector exposure
     */
    public Mono<List<SectorExposureResponse>> getSectorExposure(UUID basketId) {
        log.info("Retrieving sector exposure for basket: {}", basketId);
        
        // This is a placeholder implementation
        return Mono.just(List.of());
    }

    /**
     * Get health status
     */
    public Mono<HealthResponse> getHealth() {
        log.info("Retrieving health status");
        
        return Mono.just(new HealthResponse(
                "UP",
                LocalDateTime.now(),
                Map.of("service", "Basket Core Service", "status", "healthy")
        ));
    }

    /**
     * Get service metrics
     */
    public Mono<MetricsResponse> getMetrics() {
        log.info("Retrieving service metrics");
        
        return Mono.just(new MetricsResponse(
                LocalDateTime.now(),
                Map.of("active_baskets", 0, "total_constituents", 0)
        ));
    }
}
