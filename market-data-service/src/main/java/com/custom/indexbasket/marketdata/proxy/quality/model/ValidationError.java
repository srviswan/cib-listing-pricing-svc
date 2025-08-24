package com.custom.indexbasket.marketdata.proxy.quality.model;

/**
 * Represents a validation error with code and message.
 */
public class ValidationError {
    
    private String errorCode;
    private String message;
    private String field;
    private Object value;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    // Default constructor
    public ValidationError() {}

    // All-args constructor
    public ValidationError(String errorCode, String message, String field, Object value, String severity) {
        this.errorCode = errorCode;
        this.message = message;
        this.field = field;
        this.value = value;
        this.severity = severity;
    }

    // Getters and Setters
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}
