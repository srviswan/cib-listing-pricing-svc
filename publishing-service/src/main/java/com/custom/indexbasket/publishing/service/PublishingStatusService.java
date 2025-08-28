package com.custom.indexbasket.publishing.service;

import com.custom.indexbasket.publishing.domain.PublishingStatus;
import com.custom.indexbasket.publishing.dto.PublishingResult;
import com.custom.indexbasket.publishing.repository.PublishingStatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing publishing status operations
 */
@Slf4j
@Service
public class PublishingStatusService {

    @Autowired
    private PublishingStatusRepository repository;

    /**
     * Save publishing status
     */
    public Mono<PublishingStatus> saveStatus(PublishingResult result, String operationType) {
        PublishingStatus status = PublishingStatus.builder()
            .basketId(result.getBasketId())
            .vendorName(result.getVendor())
            .status(result.getStatus())
            .operationType(operationType)
            .vendorReference(result.getVendorReference())
            .responseTimeMs(result.getResponseTime() != null ? result.getResponseTime().toMillis() : null)
            .responseData(convertResultToJson(result))
            .publishedAt(result.getTimestamp())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        if ("FAILED".equals(result.getStatus()) || "TIMEOUT".equals(result.getStatus())) {
            status.setErrorMessage(result.getErrorMessage());
        }

        return repository.save(status)
            .doOnSuccess(saved -> log.debug("Saved publishing status for basket: {} vendor: {}", 
                result.getBasketId(), result.getVendor()))
            .doOnError(error -> log.error("Failed to save publishing status for basket: {} vendor: {}: {}", 
                result.getBasketId(), result.getVendor(), error.getMessage()));
    }

    /**
     * Get publishing status for a basket
     */
    public Flux<PublishingStatus> getStatusByBasketId(String basketId) {
        return repository.findByBasketId(basketId)
            .doOnNext(status -> log.debug("Retrieved status for basket: {} vendor: {} status: {}", 
                basketId, status.getVendorName(), status.getStatus()));
    }

    /**
     * Get latest status for each vendor for a basket
     */
    public Flux<PublishingStatus> getLatestStatusByBasketId(String basketId) {
        return repository.findLatestStatusByBasketId(basketId)
            .doOnNext(status -> log.debug("Retrieved latest status for basket: {} vendor: {} status: {}", 
                basketId, status.getVendorName(), status.getStatus()));
    }

    /**
     * Get status by basket ID and vendor
     */
    public Mono<PublishingStatus> getStatusByBasketIdAndVendor(String basketId, String vendorName) {
        return repository.findByBasketIdAndVendorName(basketId, vendorName)
            .doOnNext(status -> log.debug("Retrieved status for basket: {} vendor: {} status: {}", 
                basketId, vendorName, status.getStatus()))
            .doOnError(error -> log.debug("No status found for basket: {} vendor: {}", basketId, vendorName));
    }

    /**
     * Update status
     */
    public Mono<PublishingStatus> updateStatus(PublishingStatus status) {
        status.setUpdatedAt(LocalDateTime.now());
        return repository.save(status)
            .doOnSuccess(updated -> log.debug("Updated publishing status for basket: {} vendor: {}", 
                status.getBasketId(), status.getVendorName()));
    }

    /**
     * Get status by operation type
     */
    public Flux<PublishingStatus> getStatusByOperationType(String operationType) {
        return repository.findByOperationType(operationType)
            .doOnNext(status -> log.debug("Retrieved {} status for basket: {} vendor: {}", 
                operationType, status.getBasketId(), status.getVendorName()));
    }

    /**
     * Count successful operations for a basket
     */
    public Mono<Long> countSuccessfulOperations(String basketId) {
        return repository.countByBasketIdAndStatus(basketId, "SUCCESS")
            .doOnNext(count -> log.debug("Counted {} successful operations for basket: {}", count, basketId));
    }

    /**
     * Convert PublishingResult to JSON string for storage
     */
    private String convertResultToJson(PublishingResult result) {
        // Simple JSON conversion - in production, use Jackson ObjectMapper
        return String.format(
            "{\"vendor\":\"%s\",\"basketId\":\"%s\",\"status\":\"%s\",\"vendorReference\":\"%s\",\"timestamp\":\"%s\"}",
            result.getVendor(),
            result.getBasketId(),
            result.getStatus(),
            result.getVendorReference() != null ? result.getVendorReference() : "",
            result.getTimestamp() != null ? result.getTimestamp().toString() : ""
        );
    }
}
