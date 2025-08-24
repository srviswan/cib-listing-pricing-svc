package com.custom.indexbasket.marketdata.proxy.quality.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a data quality score for an instrument from a specific data source.
 */
public class DataQualityScore {
    
    private String instrumentId;
    private String dataSource;
    private double score;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;

    // Default constructor
    public DataQualityScore() {}

    // All-args constructor
    public DataQualityScore(String instrumentId, String dataSource, double score, LocalDateTime timestamp, Map<String, Object> metadata) {
        this.instrumentId = instrumentId;
        this.dataSource = dataSource;
        this.score = score;
        this.timestamp = timestamp;
        this.metadata = metadata;
    }

    // Getters and Setters
    public String getInstrumentId() { return instrumentId; }
    public void setInstrumentId(String instrumentId) { this.instrumentId = instrumentId; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
