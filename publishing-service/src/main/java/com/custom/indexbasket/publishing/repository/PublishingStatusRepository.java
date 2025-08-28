package com.custom.indexbasket.publishing.repository;

import com.custom.indexbasket.publishing.domain.PublishingStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Repository for PublishingStatus domain operations
 */
@Repository
public interface PublishingStatusRepository extends ReactiveCrudRepository<PublishingStatus, Long> {

    /**
     * Find by basket ID
     */
    Flux<PublishingStatus> findByBasketId(String basketId);

    /**
     * Find by basket ID and vendor name
     */
    Mono<PublishingStatus> findByBasketIdAndVendorName(String basketId, String vendorName);

    /**
     * Find by status
     */
    Flux<PublishingStatus> findByStatus(String status);

    /**
     * Find by operation type
     */
    Flux<PublishingStatus> findByOperationType(String operationType);

    /**
     * Find by basket ID and operation type
     */
    Flux<PublishingStatus> findByBasketIdAndOperationType(String basketId, String operationType);

    /**
     * Find by vendor name
     */
    Flux<PublishingStatus> findByVendorName(String vendorName);

    /**
     * Count by basket ID and status
     */
    Mono<Long> countByBasketIdAndStatus(String basketId, String status);

    /**
     * Find latest status for each vendor for a basket
     */
    @Query("SELECT DISTINCT ON (vendor_name) * FROM publishing_status " +
           "WHERE basket_id = :basketId " +
           "ORDER BY vendor_name, created_at DESC")
    Flux<PublishingStatus> findLatestStatusByBasketId(String basketId);
}
