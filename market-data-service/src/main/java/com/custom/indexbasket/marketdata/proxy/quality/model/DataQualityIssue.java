package com.custom.indexbasket.marketdata.proxy.quality.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a data quality issue that needs investigation.
 */
public class DataQualityIssue {
    
    private String issueId;
    private String issueType;
    private String description;
    private String severity;
    private String instrumentId;
    private String dataSource;
    private LocalDateTime reportedAt;
    private Map<String, Object> details;

    // Default constructor
    public DataQualityIssue() {}

    // All-args constructor
    public DataQualityIssue(String issueId, String issueType, String description, String severity,
                           String instrumentId, String dataSource, LocalDateTime reportedAt, Map<String, Object> details) {
        this.issueId = issueId;
        this.issueType = issueType;
        this.description = description;
        this.severity = severity;
        this.instrumentId = instrumentId;
        this.dataSource = dataSource;
        this.reportedAt = reportedAt;
        this.details = details;
    }

    // Getters and Setters
    public String getIssueId() { return issueId; }
    public void setIssueId(String issueId) { this.issueId = issueId; }

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getInstrumentId() { return instrumentId; }
    public void setInstrumentId(String instrumentId) { this.instrumentId = instrumentId; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}
