package com.custom.indexbasket.marketdata.proxy.quality.model;

import java.util.List;

/**
 * Result of data validation operation.
 */
public class ValidationResult {
    
    private boolean isValid;
    private List<ValidationError> errors;
    
    // Default constructor
    public ValidationResult() {}
    
    public ValidationResult(boolean isValid, List<ValidationError> errors) {
        this.isValid = isValid;
        this.errors = errors;
    }

    // Getters and Setters
    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }

    public List<ValidationError> getErrors() { return errors; }
    public void setErrors(List<ValidationError> errors) { this.errors = errors; }
}
