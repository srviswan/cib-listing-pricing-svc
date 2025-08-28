package com.custom.indexbasket.publishing.repository;

import com.custom.indexbasket.publishing.domain.VendorHealth;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for VendorHealth domain operations
 */
@Repository
public interface VendorHealthRepository extends ReactiveCrudRepository<VendorHealth, Long> {

    /**
     * Find by vendor name
     */
    Mono<VendorHealth> findByVendorName(String vendorName);

    /**
     * Find by status
     */
    Flux<VendorHealth> findByStatus(String status);

    /**
     * Find healthy vendors
     */
    Flux<VendorHealth> findByStatusAndConsecutiveFailuresLessThan(String status, Integer maxFailures);

    /**
     * Find vendors with circuit breaker open
     */
    Flux<VendorHealth> findByCircuitBreakerState(String circuitBreakerState);

    /**
     * Find vendors with high error rate
     */
    Flux<VendorHealth> findByErrorRateGreaterThan(Double errorRateThreshold);
}
