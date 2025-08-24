package com.custom.indexbasket.marketdata.proxy.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents the health status of a data source.
 */
public record DataSourceHealth(
    String dataSourceName,
    HealthStatus status,
    LocalDateTime lastCheck,
    String errorMessage,
    Map<String, Object> details
) {
    public enum HealthStatus {
        HEALTHY, DEGRADED, UNHEALTHY, UNKNOWN
    }
}
