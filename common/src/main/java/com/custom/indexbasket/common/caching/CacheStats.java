package com.custom.indexbasket.common.caching;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents statistics for cache operations.
 * Provides insights into cache performance and usage patterns.
 */
public class CacheStats {
    
    private final String cacheName;
    private final AtomicLong totalGets;
    private final AtomicLong totalPuts;
    private final AtomicLong totalDeletes;
    private final AtomicLong totalHits;
    private final AtomicLong totalMisses;
    private final AtomicLong totalEvictions;
    private final AtomicLong totalExpirations;
    private final Instant startTime;
    
    public CacheStats(String cacheName) {
        this.cacheName = cacheName;
        this.totalGets = new AtomicLong(0);
        this.totalPuts = new AtomicLong(0);
        this.totalDeletes = new AtomicLong(0);
        this.totalHits = new AtomicLong(0);
        this.totalMisses = new AtomicLong(0);
        this.totalEvictions = new AtomicLong(0);
        this.totalExpirations = new AtomicLong(0);
        this.startTime = Instant.now();
    }
    
    // Getters
    public String getCacheName() { return cacheName; }
    public long getTotalGets() { return totalGets.get(); }
    public long getTotalPuts() { return totalPuts.get(); }
    public long getTotalDeletes() { return totalDeletes.get(); }
    public long getTotalHits() { return totalHits.get(); }
    public long getTotalMisses() { return totalMisses.get(); }
    public long getTotalEvictions() { return totalEvictions.get(); }
    public long getTotalExpirations() { return totalExpirations.get(); }
    public Instant getStartTime() { return startTime; }
    
    // Metrics update methods
    public void recordGet(boolean hit) {
        totalGets.incrementAndGet();
        if (hit) {
            totalHits.incrementAndGet();
        } else {
            totalMisses.incrementAndGet();
        }
    }
    
    public void recordPut() {
        totalPuts.incrementAndGet();
    }
    
    public void recordDelete() {
        totalDeletes.incrementAndGet();
    }
    
    public void recordEviction() {
        totalEvictions.incrementAndGet();
    }
    
    public void recordExpiration() {
        totalExpirations.incrementAndGet();
    }
    
    // Calculated metrics
    public long getTotalRequests() {
        return getTotalGets() + getTotalPuts() + getTotalDeletes();
    }
    
    public double getHitRate() {
        long total = getTotalGets();
        return total > 0 ? (double) getTotalHits() / total : 0.0;
    }
    
    public double getMissRate() {
        long total = getTotalGets();
        return total > 0 ? (double) getTotalMisses() / total : 0.0;
    }
    
    public double getEfficiency() {
        long total = getTotalRequests();
        return total > 0 ? (double) (getTotalHits() + getTotalPuts()) / total : 0.0;
    }
    
    public long getUptimeSeconds() {
        return Instant.now().getEpochSecond() - startTime.getEpochSecond();
    }
    
    public boolean isHealthy() {
        return getHitRate() >= 0.8; // 80% hit rate is considered healthy
    }
    
    public boolean isHighPerformance() {
        return getHitRate() >= 0.95; // 95% hit rate is considered high performance
    }
    
    // Reset stats
    public void reset() {
        totalGets.set(0);
        totalPuts.set(0);
        totalDeletes.set(0);
        totalHits.set(0);
        totalMisses.set(0);
        totalEvictions.set(0);
        totalExpirations.set(0);
    }
    
    @Override
    public String toString() {
        return String.format("CacheStats{name=%s, hits=%.2f%%, requests=%d, uptime=%ds}",
                cacheName, getHitRate() * 100, getTotalRequests(), getUptimeSeconds());
    }
}
