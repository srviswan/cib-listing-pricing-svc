package com.custom.indexbasket.common.communication;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks performance metrics for a specific communication protocol.
 * Used by the Smart Communication Router for protocol selection and monitoring.
 */
public class ProtocolMetrics {
    
    private final CommunicationProtocol protocol;
    private final AtomicLong totalRequests;
    private final AtomicLong successfulRequests;
    private final AtomicLong failedRequests;
    private final AtomicLong timeoutRequests;
    private final AtomicReference<Double> averageLatencyMs;
    private final AtomicReference<Double> p95LatencyMs;
    private final AtomicReference<Double> p99LatencyMs;
    private final AtomicLong lastRequestTimestamp;
    
    public ProtocolMetrics(CommunicationProtocol protocol) {
        this.protocol = protocol;
        this.totalRequests = new AtomicLong(0);
        this.successfulRequests = new AtomicLong(0);
        this.failedRequests = new AtomicLong(0);
        this.timeoutRequests = new AtomicLong(0);
        this.averageLatencyMs = new AtomicReference<>(0.0);
        this.p95LatencyMs = new AtomicReference<>(0.0);
        this.p99LatencyMs = new AtomicReference<>(0.0);
        this.lastRequestTimestamp = new AtomicLong(0);
    }
    
    // Getters
    public CommunicationProtocol getProtocol() { return protocol; }
    public long getTotalRequests() { return totalRequests.get(); }
    public long getSuccessfulRequests() { return successfulRequests.get(); }
    public long getFailedRequests() { return failedRequests.get(); }
    public long getTimeoutRequests() { return timeoutRequests.get(); }
    public double getAverageLatencyMs() { return averageLatencyMs.get(); }
    public double getP95LatencyMs() { return p95LatencyMs.get(); }
    public double getP99LatencyMs() { return p99LatencyMs.get(); }
    public long getLastRequestTimestamp() { return lastRequestTimestamp.get(); }
    
    // Metrics update methods
    public void recordRequest(long latencyMs, boolean success, boolean timeout) {
        totalRequests.incrementAndGet();
        lastRequestTimestamp.set(System.currentTimeMillis());
        
        if (timeout) {
            timeoutRequests.incrementAndGet();
        } else if (success) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
        
        updateLatencyMetrics(latencyMs);
    }
    
    private void updateLatencyMetrics(long latencyMs) {
        long total = totalRequests.get();
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
    public double getSuccessRate() {
        long total = totalRequests.get();
        return total > 0 ? (double) successfulRequests.get() / total : 0.0;
    }
    
    public double getFailureRate() {
        long total = totalRequests.get();
        return total > 0 ? (double) failedRequests.get() / total : 0.0;
    }
    
    public double getTimeoutRate() {
        long total = totalRequests.get();
        return total > 0 ? (double) timeoutRequests.incrementAndGet() / total : 0.0;
    }
    
    public boolean isHealthy() {
        return getSuccessRate() >= 0.95 && getAverageLatencyMs() <= protocol.getMaxLatencyMs();
    }
    
    public boolean meetsLatencyRequirement(int requiredLatencyMs) {
        return getP95LatencyMs() <= requiredLatencyMs;
    }
    
    // Reset metrics
    public void reset() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        timeoutRequests.set(0);
        averageLatencyMs.set(0.0);
        p95LatencyMs.set(0.0);
        p99LatencyMs.set(0.0);
        lastRequestTimestamp.set(0);
    }
    
    @Override
    public String toString() {
        return String.format("ProtocolMetrics{protocol=%s, total=%d, success=%.2f%%, avgLatency=%.2fms, p95=%.2fms, p99=%.2fms}",
                protocol, getTotalRequests(), getSuccessRate() * 100, getAverageLatencyMs(), getP95LatencyMs(), getP99LatencyMs());
    }
}
