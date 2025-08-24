package com.custom.indexbasket.marketdata.proxy.quality;

import com.custom.indexbasket.marketdata.dto.MarketDataResponse;
import com.custom.indexbasket.marketdata.proxy.quality.model.ValidationResult;

/**
 * Interface for validating market data.
 */
public interface DataValidator {
    
    /**
     * Validate market data and return validation result
     */
    ValidationResult validate(MarketDataResponse data);
    
    /**
     * Get validator name for identification
     */
    String getValidatorName();
    
    /**
     * Get validation rules description
     */
    String getValidationRules();
}
