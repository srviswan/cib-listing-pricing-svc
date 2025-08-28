package com.custom.indexbasket.publishing.repository;

import com.custom.indexbasket.publishing.domain.PublishingMetrics;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Repository for PublishingMetrics domain operations
 */
@Repository
public interface PublishingMetricsRepository extends ReactiveCrudRepository<PublishingMetrics, Long> {

    /**
     * Find by basket ID
     */
    Flux<PublishingMetrics> findByBasketId(String basketId);

    /**
     * Find by vendor name
     */
    Flux<PublishingMetrics> findByVendorName(String vendorName);

    /**
     * Find by operation type
     */
    Flux<PublishingMetrics> findByOperationType(String operationType);

    /**
     * Find by success status
     */
    Flux<PublishingMetrics> findBySuccess(Boolean success);

    /**
     * Find by timestamp range
     */
    Flux<PublishingMetrics> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find by basket ID and timestamp range
     */
    Flux<PublishingMetrics> findByBasketIdAndTimestampBetween(String basketId, LocalDateTime start, LocalDateTime end);

    /**
     * Find by vendor name and timestamp range
     */
    Flux<PublishingMetrics> findByVendorNameAndTimestampBetween(String vendorName, LocalDateTime start, LocalDateTime end);

    /**
     * Count successful operations by vendor
     */
    @Query("SELECT vendor_name, COUNT(*) as success_count FROM publishing_metrics " +
           "WHERE success = true AND timestamp >= :since " +
           "GROUP BY vendor_name")
    Flux<Object> countSuccessfulOperationsByVendor(LocalDateTime since);

    /**
     * Count failed operations by vendor
     */
    @Query("SELECT vendor_name, COUNT(*) as failure_count FROM publishing_metrics " +
           "WHERE success = false AND timestamp >= :since " +
           "GROUP BY vendor_name")
    Flux<Object> countFailedOperationsByVendor(LocalDateTime since);

    /**
     * Calculate average response time by vendor
     */
    @Query("SELECT vendor_name, AVG(duration_ms) as avg_duration FROM publishing_metrics " +
           "WHERE success = true AND timestamp >= :since " +
           "GROUP BY vendor_name")
    Flux<Object> calculateAverageResponseTimeByVendor(LocalDateTime since);
}
