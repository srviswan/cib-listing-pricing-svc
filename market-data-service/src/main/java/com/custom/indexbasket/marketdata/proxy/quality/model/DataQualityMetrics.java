package com.custom.indexbasket.marketdata.proxy.quality.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Overall data quality metrics for the system.
 */
public class DataQualityMetrics {
    
    private LocalDateTime timestamp;
    private long totalRecords;
    private long validRecords;
    private long invalidRecords;
    private double overallQualityScore;
    private Map<String, Object> additionalMetrics;

    // Default constructor
    public DataQualityMetrics() {}

    // All-args constructor
    public DataQualityMetrics(LocalDateTime timestamp, long totalRecords, long validRecords, long invalidRecords,
                             double overallQualityScore, Map<String, Object> additionalMetrics) {
        this.timestamp = timestamp;
        this.totalRecords = totalRecords;
        this.validRecords = validRecords;
        this.invalidRecords = invalidRecords;
        this.overallQualityScore = overallQualityScore;
        this.additionalMetrics = additionalMetrics;
    }

    // Getters and Setters
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }

    public long getValidRecords() { return validRecords; }
    public void setValidRecords(long validRecords) { this.validRecords = validRecords; }

    public long getInvalidRecords() { return invalidRecords; }
    public void setInvalidRecords(long invalidRecords) { this.invalidRecords = invalidRecords; }

    public double getOverallQualityScore() { return overallQualityScore; }
    public void setOverallQualityScore(double overallQualityScore) { this.overallQualityScore = overallQualityScore; }

    public Map<String, Object> getAdditionalMetrics() { return additionalMetrics; }
    public void setAdditionalMetrics(Map<String, Object> additionalMetrics) { this.additionalMetrics = additionalMetrics; }
}
