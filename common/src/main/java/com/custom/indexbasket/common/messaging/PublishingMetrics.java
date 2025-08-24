package com.custom.indexbasket.common.messaging;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks metrics for event publishing operations.
 * Provides insights into publishing performance and reliability.
 */
public class PublishingMetrics {
    
    private final String topic;
    private final AtomicLong totalPublished;
    private final AtomicLong totalFailed;
    private final AtomicLong totalTimeout;
    private final AtomicReference<Double> averageLatencyMs;
    private final AtomicReference<Double> p95LatencyMs;
    private final AtomicReference<Double> p99LatencyMs;
    private final AtomicLong lastPublishedTimestamp;
    
    public PublishingMetrics(String topic) {
        this.topic = topic;
        this.totalPublished = new AtomicLong(0);
        this.totalFailed = new AtomicLong(0);
        this.totalTimeout = new AtomicLong(0);
        this.averageLatencyMs = new AtomicReference<>(0.0);
        this.p95LatencyMs = new AtomicReference<>(0.0);
        this.p99LatencyMs = new AtomicReference<>(0.0);
        this.lastPublishedTimestamp = new AtomicLong(0);
    }
    
    // Getters
    public String getTopic() { return topic; }
    public long getTotalPublished() { return totalPublished.get(); }
    public long getTotalFailed() { return totalFailed.get(); }
    public long getTotalTimeout() { return totalTimeout.get(); }
    public double getAverageLatencyMs() { return averageLatencyMs.get(); }
    public double getP95LatencyMs() { return p95LatencyMs.get(); }
    public double getP99LatencyMs() { return p99LatencyMs.get(); }
    public long getLastPublishedTimestamp() { return lastPublishedTimestamp.get(); }
    
    // Metrics update methods
    public void recordPublish(boolean success, long latencyMs, boolean timeout) {
        if (timeout) {
            totalTimeout.incrementAndGet();
        } else if (success) {
            totalPublished.incrementAndGet();
        } else {
            totalFailed.incrementAndGet();
        }
        
        lastPublishedTimestamp.set(System.currentTimeMillis());
        updateLatencyMetrics(latencyMs);
    }
    
    private void updateLatencyMetrics(long latencyMs) {
        long total = getTotalPublished() + getTotalFailed();
        double currentAvg = averageLatencyMs.get();
        
        // Update average latency using exponential moving average
        double alpha = 0.1; // Smoothing factor
        double newAvg = (alpha * latencyMs) + ((1 - alpha) * currentAvg);
        averageLatencyMs.set(newAvg);
        
        // For simplicity, we'll use the current latency as P95/P99
        // In a real implementation, you'd maintain a rolling window of latencies
        if (latencyMs > p95LatencyMs.get()) {
            p95LatencyMs.set((double) latencyMs);
        }
        if (latencyMs > p99LatencyMs.get()) {
            p99LatencyMs.set((double) latencyMs);
        }
    }
    
    // Calculated metrics
    public long getTotalAttempts() {
        return getTotalPublished() + getTotalFailed() + getTotalTimeout();
    }
    
    public double getSuccessRate() {
        long total = getTotalAttempts();
        return total > 0 ? (double) getTotalPublished() / total : 0.0;
    }
    
    public double getFailureRate() {
        long total = getTotalAttempts();
        return total > 0 ? (double) getTotalFailed() / total : 0.0;
    }
    
    public double getTimeoutRate() {
        long total = getTotalAttempts();
        return total > 0 ? (double) getTotalTimeout() / total : 0.0;
    }
    
    public boolean isHealthy() {
        return getSuccessRate() >= 0.95 && getAverageLatencyMs() < 100; // 95% success rate and <100ms avg
    }
    
    public boolean isHighPerformance() {
        return getAverageLatencyMs() < 10; // <10ms average latency
    }
    
    // Reset metrics
    public void reset() {
        totalPublished.set(0);
        totalFailed.set(0);
        totalTimeout.set(0);
        averageLatencyMs.set(0.0);
        p95LatencyMs.set(0.0);
        p99LatencyMs.set(0.0);
        lastPublishedTimestamp.set(0);
    }
    
    @Override
    public String toString() {
        return String.format("PublishingMetrics{topic=%s, total=%d, success=%.2f%%, avgLatency=%.2fms, p95=%.2fms, p99=%.2fms}",
                topic, getTotalAttempts(), getSuccessRate() * 100, getAverageLatencyMs(), getP95LatencyMs(), getP99LatencyMs());
    }
}
