package com.custom.indexbasket.basket.controller;

import com.custom.indexbasket.basket.domain.BasketEntity;
import com.custom.indexbasket.basket.dto.ApiResponse;
import com.custom.indexbasket.basket.dto.CreateBasketRequest;
import com.custom.indexbasket.basket.dto.PaginatedBasketsResponse;
import com.custom.indexbasket.basket.dto.UpdateBasketRequest;
import com.custom.indexbasket.basket.dto.UpdateBasketStatusRequest;
import com.custom.indexbasket.basket.service.BasketService;
import com.custom.indexbasket.basket.event.EventPublisher;
import com.custom.indexbasket.common.model.BasketStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST Controller for Basket operations
 * Provides CRUD operations for baskets
 */
@RestController
@RequestMapping("/api/v1/baskets")
@CrossOrigin(origins = "*")
public class BasketController {

    private static final Logger log = LoggerFactory.getLogger(BasketController.class);
    private final BasketService basketService;

    public BasketController(BasketService basketService) {
        this.basketService = basketService;
    }

    /**
     * Create a new basket
     */
    @PostMapping
    public Mono<ApiResponse<UUID>> createBasket(@RequestBody CreateBasketRequest request) {
        log.info("POST /api/v1/baskets - Creating new basket with code: {}", request.basketCode());
        
        return basketService.createBasket(request)
            .map(basketId -> {
                log.info("Basket created successfully with ID: {}", basketId);
                return ApiResponse.success("Basket created successfully", basketId);
            })
            .onErrorResume(error -> {
                log.error("Error creating basket: {}", error.getMessage(), error);
                return Mono.just(new ApiResponse<>(false, "Failed to create basket: " + error.getMessage(), null, java.time.LocalDateTime.now()));
            });
    }

    /**
     * Get all baskets with pagination and filtering
     */
    @GetMapping
    public Mono<ApiResponse<PaginatedBasketsResponse>> getAllBaskets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) BasketStatus status,
            @RequestParam(required = false) String basketType) {
        
        log.info("GET /api/v1/baskets - page: {}, size: {}, status: {}, type: {}", page, size, status, basketType);
        
        Pageable pageable = PageRequest.of(page, size);
        
        return basketService.getAllBaskets(pageable, status, basketType)
            .map(baskets -> {
                log.info("Retrieved {} baskets", baskets.baskets().size());
                return ApiResponse.success("Success", baskets);
            })
            .onErrorResume(error -> {
                log.error("Error retrieving baskets: {}", error.getMessage(), error);
                return Mono.just(new ApiResponse<>(false, "Failed to retrieve baskets: " + error.getMessage(), null, java.time.LocalDateTime.now()));
            });
    }

    /**
     * Get basket by ID
     */
    @GetMapping("/{basketId}")
    public Mono<ApiResponse<BasketEntity>> getBasketById(@PathVariable UUID basketId) {
        log.info("GET /api/v1/baskets/{} - Retrieving basket", basketId);
        
        return basketService.getBasketById(basketId)
            .map(basket -> {
                log.info("Basket retrieved successfully with ID: {}", basketId);
                return ApiResponse.success("Basket retrieved successfully", basket);
            })
            .onErrorResume(error -> {
                log.error("Error retrieving basket: {}", error.getMessage(), error);
                return Mono.just(new ApiResponse<>(false, "Failed to retrieve basket: " + error.getMessage(), null, java.time.LocalDateTime.now()));
            });
    }

    /**
     * Update an existing basket
     */
    @PutMapping("/{basketId}")
    public Mono<ApiResponse<BasketEntity>> updateBasket(
            @PathVariable UUID basketId,
            @RequestBody UpdateBasketRequest request) {
        
        log.info("PUT /api/v1/baskets/{} - Updating basket", basketId);
        
        return basketService.updateBasket(basketId, request)
            .map(updatedBasket -> {
                log.info("Basket updated successfully with ID: {}", basketId);
                return ApiResponse.success("Basket updated successfully", updatedBasket);
            })
            .onErrorResume(error -> {
                log.error("Error updating basket: {}", error.getMessage(), error);
                return Mono.just(new ApiResponse<>(false, "Failed to update basket: " + error.getMessage(), null, java.time.LocalDateTime.now()));
            });
    }

    /**
     * Update basket status using state machine workflow
     */
    @PutMapping("/{basketId}/status")
    public Mono<ApiResponse<Void>> updateBasketStatus(
            @PathVariable UUID basketId,
            @RequestBody UpdateBasketStatusRequest request) {
        
        log.info("PUT /api/v1/baskets/{}/status - Updating basket status with event: {}", basketId, request.event());
        
        return basketService.updateBasketStatus(basketId, request.event(), request.updatedBy())
            .then(Mono.just(ApiResponse.<Void>success("Basket status updated successfully", null)))
            .onErrorResume(error -> {
                log.error("Error updating basket status: {}", error.getMessage(), error);
                return Mono.just(new ApiResponse<>(false, "Failed to update basket status: " + error.getMessage(), null, java.time.LocalDateTime.now()));
            });
    }

    /**
     * Delete a basket (soft delete)
     */
    @DeleteMapping("/{basketId}")
    public Mono<ApiResponse<Void>> deleteBasket(
            @PathVariable UUID basketId,
            @RequestParam String deletedBy) {
        
        log.info("DELETE /api/v1/baskets/{} - Deleting basket by user: {}", basketId, deletedBy);
        
        return basketService.deleteBasket(basketId, deletedBy)
            .then(Mono.just(ApiResponse.<Void>success("Basket deleted successfully", null)))
            .onErrorResume(error -> {
                log.error("Error deleting basket: {}", error.getMessage(), error);
                return Mono.just(new ApiResponse<>(false, "Failed to delete basket: " + error.getMessage(), null, java.time.LocalDateTime.now()));
            });
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Mono<ApiResponse<String>> health() {
        log.info("GET /api/v1/baskets/health - Health check");
        return Mono.just(ApiResponse.success("Basket service is healthy", "UP"));
    }
    
    /**
     * Event publishing status endpoint
     */
    @GetMapping("/events/status")
    public Mono<ApiResponse<EventPublisher.PublishingStats>> getEventPublishingStatus() {
        log.info("GET /api/v1/baskets/events/status - Event publishing status");
        return basketService.getEventPublishingStatus()
            .map(stats -> ApiResponse.success("Event publishing status retrieved", stats))
            .onErrorResume(error -> {
                log.error("Error retrieving event publishing status: {}", error.getMessage(), error);
                return Mono.just(new ApiResponse<>(false, "Failed to retrieve event publishing status: " + error.getMessage(), null, java.time.LocalDateTime.now()));
            });
    }
}


