package com.custom.indexbasket.marketdata.proxy.cache.model;

/**
 * Statistics for cache operations.
 */
public record CacheStats(
    long hitCount,
    long missCount,
    long setCount,
    double hitRate,
    long maxSize,
    long currentSize
) {}
