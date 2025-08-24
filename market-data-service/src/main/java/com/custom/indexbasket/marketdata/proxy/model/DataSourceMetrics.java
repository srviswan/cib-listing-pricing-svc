package com.custom.indexbasket.marketdata.proxy.model;

import java.time.Duration;
import java.util.Map;

/**
 * Represents performance metrics for a data source.
 */
public record DataSourceMetrics(
    String dataSourceName,
    long totalRequests,
    long successfulRequests,
    long failedRequests,
    Duration averageResponseTime,
    Duration lastResponseTime,
    double successRate,
    Map<String, Object> customMetrics
) {}
