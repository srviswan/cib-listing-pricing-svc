package com.custom.indexbasket.marketdata.proxy.quality.model;

import java.time.LocalDateTime;

/**
 * Data quality trend over time.
 */
public class DataQualityTrend {
    
    private LocalDateTime timestamp;
    private double qualityScore;
    private long totalRecords;
    private long invalidRecords;

    // Default constructor
    public DataQualityTrend() {}

    // All-args constructor
    public DataQualityTrend(LocalDateTime timestamp, double qualityScore, long totalRecords, long invalidRecords) {
        this.timestamp = timestamp;
        this.qualityScore = qualityScore;
        this.totalRecords = totalRecords;
        this.invalidRecords = invalidRecords;
    }

    // Getters and Setters
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public double getQualityScore() { return qualityScore; }
    public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }

    public long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }

    public long getInvalidRecords() { return invalidRecords; }
    public void setInvalidRecords(long invalidRecords) { this.invalidRecords = invalidRecords; }
}
